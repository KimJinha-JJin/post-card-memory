package com.postcardmemory.ui.gallery

import androidx.compose.ui.geometry.Offset
import com.postcardmemory.testsupport.readStructureTestSource
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 108일차 연잎 접촉 충돌: 실제로 맞닿아 다가갈 때만 충격이 생기고(같은 질량·반발 0.75), 세기·상한·
 * 최소 전달·같은 쌍 재충돌 금지·연쇄 단계 상한·붙잡힌 연잎·날고 있는 연잎·수면 물살을 고정한다.
 * 실제 튕김의 느낌과 복귀 연출은 실기기 확인 몫이다.
 */
class PondLeafContactTest {

    private val radius = 50f
    private val tuning = PondContactTuning(
        minTransferSpeedPx = 100f,
        maxTransferSpeedPx = 2_000f,
        minRipplePx = 20f,
        maxRipplePx = 40f
    )
    private val restitution = POND_CONTACT_RESTITUTION

    private fun body(id: Long, x: Float, y: Float) =
        PondLeafBody(id).apply { centerX = x; centerY = y; this.radius = this@PondLeafContactTest.radius }

    /** [block] 동안 [PondController.hits]로 나온 충격을 모은다. */
    private fun collectHits(controller: PondController, block: () -> Unit): List<PondHit> = runBlocking {
        val received = mutableListOf<PondHit>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { controller.hits.collect { received.add(it) } }
        block()
        yield()
        job.cancel()
        received
    }

    @Test
    fun contact_onlyWhenTouchingAndApproaching() {
        val out = FloatArray(3)
        assertTrue("80px 떨어진 반경 50 두 원은 닿아 있음", pondLeafContact(0f, 0f, 500f, 0f, 50f, 80f, 0f, 50f, out))
        assertEquals(1f, out[0], 1e-5f)
        assertEquals(0f, out[1], 1e-5f)
        assertEquals(500f, out[2], 1e-3f)

        assertFalse("닿지 않으면 충돌 없음", pondLeafContact(0f, 0f, 500f, 0f, 50f, 120f, 0f, 50f, out))
        assertFalse("닿아도 멀어지는 중이면 충돌 없음", pondLeafContact(0f, 0f, -500f, 0f, 50f, 80f, 0f, 50f, out))
    }

    @Test
    fun hitRestingLeaf_pushesItAlongTheContactNormal_andSlowsTheShooter() {
        val controller = PondController()
        controller.registerLeaf(body(1, 0f, 0f))
        controller.registerLeaf(body(2, 80f, 0f))
        var after = Offset.Zero

        val hits = collectHits(controller) {
            after = controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 1_000L, tuning)
        }

        val impulse = 800f * (1f + restitution) / 2f
        assertEquals(1, hits.size)
        assertEquals(2L, hits[0].targetId)
        assertEquals("충돌 방향(+x)으로 밀려야 함", impulse, hits[0].velocity.x, 1e-2f)
        assertEquals(0f, hits[0].velocity.y, 1e-3f)
        assertEquals("사용자 발사(0) 다음 단계", 1, hits[0].generation)
        assertEquals("쏜 연잎도 그만큼 느려져야 함", 800f - impulse, after.x, 1e-2f)
        assertEquals("충돌 지점에 수면 물살", 1, controller.ripples.size)
        assertEquals(50f, controller.ripples[0].center.x, 1e-3f)
    }

    @Test
    fun noContact_changesNothing() {
        val controller = PondController()
        controller.registerLeaf(body(1, 0f, 0f))
        controller.registerLeaf(body(2, 300f, 0f))

        val hits = collectHits(controller) {
            val after = controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 1_000L, tuning)
            assertEquals(Offset(800f, 0f), after)
        }
        assertTrue(hits.isEmpty())
        assertTrue(controller.ripples.isEmpty())
    }

    @Test
    fun fasterImpact_hitsHarderAndSplashesMore_butIsCapped() {
        fun hitFor(speed: Float): Pair<Float, Float> {
            val controller = PondController()
            controller.registerLeaf(body(1, 0f, 0f))
            controller.registerLeaf(body(2, 80f, 0f))
            val hits = collectHits(controller) {
                controller.resolveContacts(1, 0f, 0f, Offset(speed, 0f), 0, 1_000L, tuning)
            }
            return hits.single().velocity.x to controller.ripples.single().maxRadiusPx
        }
        val (slowPush, slowSplash) = hitFor(400f)
        val (fastPush, fastSplash) = hitFor(1_600f)
        assertTrue("빠를수록 더 세게: $slowPush < $fastPush", slowPush < fastPush)
        assertTrue("빠를수록 물살도 크게: $slowSplash < $fastSplash", slowSplash < fastSplash)

        val (cappedPush, cappedSplash) = hitFor(10_000f)
        assertEquals(tuning.maxTransferSpeedPx, cappedPush, 1e-2f)
        assertEquals(tuning.maxRipplePx, cappedSplash, 1e-3f)
    }

    @Test
    fun samePair_isNotRehitWithinCooldown() {
        val controller = PondController()
        controller.registerLeaf(body(1, 0f, 0f))
        controller.registerLeaf(body(2, 80f, 0f))

        val hits = collectHits(controller) {
            controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 1_000L, tuning)
            controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 1_000L + POND_PAIR_COOLDOWN_MS - 1, tuning)
            // 반대쪽(B가 A를 친다고 봐도) 같은 쌍이면 막힌다.
            controller.resolveContacts(2, 80f, 0f, Offset(-800f, 0f), 0, 1_100L, tuning)
            controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 1_000L + POND_PAIR_COOLDOWN_MS, tuning)
        }
        assertEquals("첫 충돌 1번 + 쿨다운 지난 뒤 1번", 2, hits.size)
    }

    @Test
    fun chainStopsAtMaxGeneration_andWhenTooWeak() {
        val controller = PondController()
        controller.registerLeaf(body(1, 0f, 0f))
        controller.registerLeaf(body(2, 80f, 0f))
        controller.registerLeaf(body(3, 0f, 80f))

        val hits = collectHits(controller) {
            controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), POND_CHAIN_MAX_GENERATION, 1_000L, tuning)
            controller.resolveContacts(1, 0f, 0f, Offset(0f, 100f), 0, 1_000L, tuning)
        }
        assertTrue("최대 단계를 넘거나(첫째) 너무 약하면(둘째) 넘기지 않음", hits.isEmpty())
        assertEquals("넘기지 않아도 부딪힌 자리 물살은 생김", 2, controller.ripples.size)
    }

    @Test
    fun chain_A_B_C_decaysEachHop() {
        val controller = PondController()
        controller.registerLeaf(body(1, 0f, 0f))
        controller.registerLeaf(body(2, 80f, 0f))
        controller.registerLeaf(body(3, 160f, 0f))

        val first = collectHits(controller) {
            controller.resolveContacts(1, 0f, 0f, Offset(1_200f, 0f), 0, 1_000L, tuning)
        }.single()
        assertEquals(2L, first.targetId)

        // B가 받은 속도로 날아가 C에 닿는다.
        val second = collectHits(controller) {
            controller.resolveContacts(2, 80f, 0f, first.velocity, first.generation, 1_050L, tuning)
        }
        val toC = second.single { it.targetId == 3L }
        assertEquals(2, toC.generation)
        assertTrue("단계마다 약해져야 함: ${first.velocity.x} > ${toC.velocity.x}", toC.velocity.x < first.velocity.x)
    }

    @Test
    fun heldLeaf_staysPut_andShooterBouncesOff() {
        val controller = PondController()
        controller.registerLeaf(body(1, 0f, 0f))
        controller.registerLeaf(body(2, 80f, 0f).apply { isHeld = true })
        var after = Offset.Zero

        val hits = collectHits(controller) {
            after = controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 1_000L, tuning)
        }
        assertTrue("붙잡힌 연잎에는 충격을 보내지 않음", hits.isEmpty())
        assertEquals("쏜 연잎만 벽처럼 반사·감쇠", -800f * restitution, after.x, 1e-2f)
    }

    @Test
    fun flyingLeaf_getsExtraVelocityInsteadOfANewFlight() {
        val controller = PondController()
        controller.registerLeaf(body(1, 0f, 0f))
        controller.registerLeaf(body(2, 80f, 0f).apply { isFlying = true })

        val hits = collectHits(controller) {
            controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 1_000L, tuning)
        }
        assertTrue(hits.isEmpty())
        val pending = controller.takePendingVelocity(2)
        assertEquals(800f * (1f + restitution) / 2f, pending.x, 1e-2f)
        assertEquals("한 번 꺼내면 비워짐", Offset.Zero, controller.takePendingVelocity(2))
    }

    @Test
    fun unregisteredLeaves_andReset_clearContactState() {
        val controller = PondController()
        val a = body(1, 0f, 0f)
        val b = body(2, 80f, 0f).apply { isFlying = true }
        controller.registerLeaf(a)
        controller.registerLeaf(b)
        controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 1_000L, tuning)

        controller.reset()
        assertEquals("reset은 쌓인 충격을 비움", Offset.Zero, controller.takePendingVelocity(2))
        // 쿨다운도 비워져 같은 시각에 다시 충돌 가능.
        controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 1_000L, tuning)
        assertTrue(controller.takePendingVelocity(2).x > 0f)

        controller.unregisterLeaf(b)
        val after = controller.resolveContacts(1, 0f, 0f, Offset(800f, 0f), 0, 5_000L, tuning)
        assertEquals("등록 해제된 연잎과는 충돌하지 않음", Offset(800f, 0f), after)
    }

    @Test
    fun stampCard_usesContactsInFlight_andProximityPushIsGone() {
        val stampCard = readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/components/StampCard.kt",
                "app/src/main/java/com/postcardmemory/ui/components/StampCard.kt"
            )
        )
        val flight = stampCard.substring(
            stampCard.indexOf("suspend fun runPondFlight("),
            stampCard.indexOf("springPondBackToOrigin()\n    }", stampCard.indexOf("suspend fun runPondFlight("))
        )
        assertTrue("비행 루프가 접촉 충돌을 판정해야 함", flight.contains("pondController.resolveContacts("))
        assertTrue("비행 중 맞아 쌓인 속도를 더해야 함", flight.contains("pondController.takePendingVelocity(postcard.id)"))
        assertTrue("비행이 끝나거나 취소돼도 비행 표시를 끔", flight.contains("} finally {\n            leafBody.isFlying = false"))

        assertTrue("연못일 때만 몸체 등록·해제", stampCard.contains("pondController.registerLeaf(leafBody)") &&
            stampCard.contains("pondController.unregisterLeaf(leafBody)"))
        assertTrue("맞으면 기존 비행으로 날아감", stampCard.contains("runPondFlight(hit.velocity, hit.generation)"))
        assertFalse("발사 지점 근접 밀림은 접촉 충돌로 대체됨", stampCard.contains("lastImpulse") || stampCard.contains("notifyLaunch"))
    }
}
