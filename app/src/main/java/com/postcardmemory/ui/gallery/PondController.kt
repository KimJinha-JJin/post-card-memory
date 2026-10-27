package com.postcardmemory.ui.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.postcardmemory.data.Postcard
import kotlinx.coroutines.flow.MutableSharedFlow

/** 연못에 한 번에 띄우는 엽서 수. */
internal const val POND_MAX_POSTCARDS = 10

/**
 * 연못에 띄울 엽서를 고른다 — 갤러리의 검색어·정렬 순서와 무관하게, 엽서 자체의
 * 날짜(capturedAt) 최신순으로 전체에서 최대 [POND_MAX_POSTCARDS]장. 날짜가 같으면
 * id가 큰(나중에 만든) 쪽을 먼저 둬 순서가 매번 같게 한다.
 */
internal fun pondPostcardsFor(postcards: List<Postcard>): List<Postcard> =
    postcards
        .sortedWith(
            compareByDescending<Postcard> { it.capturedAt }
                .thenByDescending { it.id }
        )
        .take(POND_MAX_POSTCARDS)

/** 엽서의 연못 모드에서 발사된 엽서가 지나가거나 부딪힌 자리에 남는 파문 한 개. */
data class PondRipple(
    val id: Long,
    val center: Offset,
    val bornAtMillis: Long,
    val maxRadiusPx: Float,
    val lifespanMillis: Long = 700L
)

private const val MAX_RIPPLES = 20

// 108일차 연잎 접촉 충돌: 반발 계수(1 = 완전 탄성), 같은 두 연잎을 다시 충돌로 치지 않는 시간,
// 연쇄 최대 단계(사용자 발사 0 → 맞은 연잎 1 → 그 연잎에 맞은 연잎 2 → 3).
internal const val POND_CONTACT_RESTITUTION = 0.75f
internal const val POND_PAIR_COOLDOWN_MS = 350L
internal const val POND_CHAIN_MAX_GENERATION = 3

/**
 * 연못 위 연잎 한 장의 현재 원형 몸체(윈도우 px). 각 StampCard가 이미 돌고 있는 프레임
 * 루프에서 매 프레임 갱신하고, 날아가는 연잎이 접촉 판정에 읽는다. 저장되지 않는 화면 한정 값.
 */
class PondLeafBody(val id: Long) {
    var centerX = 0f
    var centerY = 0f
    var radius = 0f
    /** 비행 루프 안에 있는 동안 true — 맞으면 새 비행 대신 진행 중인 비행 속도에 더한다. */
    var isFlying = false
    /** 사용자가 붙잡고 있는 동안 true — 맞아도 움직이지 않고 부딪힌 쪽만 튕겨 나간다. */
    var isHeld = false
}

/** 쉬고 있던 연잎에 전달되는 충격 — 이 속도(px/s)로 날아가고, 그 비행은 [generation]단계다. */
data class PondHit(
    val targetId: Long,
    val velocity: Offset,
    val generation: Int
)

/**
 * 접촉 충돌 크기 설정(px 단위). [minTransferSpeedPx]보다 약하면 상대에게 전달하지 않고(연쇄
 * 종료), 전달 속도는 [maxTransferSpeedPx]로 묶는다. 충돌 물살 반경은 다가간 속도에 비례해
 * [minRipplePx]~[maxRipplePx] 사이.
 */
class PondContactTuning(
    val minTransferSpeedPx: Float,
    val maxTransferSpeedPx: Float,
    val minRipplePx: Float,
    val maxRipplePx: Float
)

/**
 * 두 원(A가 속도 ([avx], [avy])로 움직이고 B는 멈춰 있다고 봄)이 실제로 맞닿아 서로 다가가는
 * 중이면, A→B 방향 단위벡터와 A가 B 쪽으로 다가가던 속도를 [out]에 (nx, ny, approach)로 쓰고
 * true. 닿지 않았거나 이미 멀어지는 중이면 false.
 */
internal fun pondLeafContact(
    ax: Float, ay: Float, avx: Float, avy: Float, ar: Float,
    bx: Float, by: Float, br: Float,
    out: FloatArray
): Boolean {
    val dx = bx - ax
    val dy = by - ay
    val distSq = dx * dx + dy * dy
    val touch = ar + br
    if (distSq >= touch * touch || distSq < 1e-4f) return false
    val dist = kotlin.math.sqrt(distSq)
    val nx = dx / dist
    val ny = dy / dist
    val approach = avx * nx + avy * ny
    if (approach <= 0f) return false
    out[0] = nx
    out[1] = ny
    out[2] = approach
    return true
}

/**
 * 연못 모드 그리드 하나가 공유하는 임시(화면 한정) 상태 — 파문(수면 물살 입력), 휴대폰 기울기,
 * 그리고 108일차부터 연잎 접촉 충돌. 각 연잎은 자기 몸체([PondLeafBody])를 등록해 매 프레임
 * 위치를 갱신하고, 날아가는 연잎이 [resolveContacts]로 실제로 닿은 연잎에만 충격을 준다.
 * 어떤 저장 데이터도 담지 않으며, 화면을 벗어나면 그냥 버려진다.
 */
class PondController {
    var gridBoundsInWindow: Rect = Rect.Zero

    val ripples = mutableStateListOf<PondRipple>()

    /**
     * 수면이 매 프레임 넘겨주는 평활된 휴대폰 좌우 기울기(+ = 오른쪽이 내려감, ±[POND_TILT_MAX]).
     * 연잎(StampCard)이 draw 단계에서 읽어 낮아진 쪽으로 미끄러진다.
     */
    var waterTilt by mutableFloatStateOf(0f)

    /** 쉬고 있던 연잎이 맞았을 때 받는 충격. 각 StampCard가 자기 id만 골라 비행을 시작한다. */
    val hits = MutableSharedFlow<PondHit>(extraBufferCapacity = 16)

    private val leaves = LinkedHashMap<Long, PondLeafBody>()
    private val pairCooldownUntil = HashMap<Long, Long>()
    private val pendingVelocity = HashMap<Long, Offset>()
    private val contact = FloatArray(3)

    private var rippleIdSeq = 0L

    fun addRipple(centerInWindow: Offset, maxRadiusPx: Float, lifespanMillis: Long = 700L) {
        if (ripples.size >= MAX_RIPPLES) {
            ripples.removeAt(0)
        }
        ripples.add(
            PondRipple(
                id = rippleIdSeq++,
                center = centerInWindow,
                bornAtMillis = System.currentTimeMillis(),
                maxRadiusPx = maxRadiusPx,
                lifespanMillis = lifespanMillis
            )
        )
    }

    /** 수명([PondRipple.lifespanMillis] × [lifeScale])이 지난 파문을 지운다. 지울 게 없으면 목록을 건드리지 않는다. */
    fun pruneRipples(nowMillis: Long, lifeScale: Float = 1f) {
        if (ripples.none { nowMillis - it.bornAtMillis > it.lifespanMillis * lifeScale }) return
        ripples.removeAll { nowMillis - it.bornAtMillis > it.lifespanMillis * lifeScale }
    }

    fun registerLeaf(body: PondLeafBody) {
        leaves[body.id] = body
    }

    fun unregisterLeaf(body: PondLeafBody) {
        if (leaves[body.id] === body) leaves.remove(body.id)
        pendingVelocity.remove(body.id)
    }

    /** 날아가는 중에 맞아 쌓인 추가 속도를 꺼낸다(없으면 [Offset.Zero]). */
    fun takePendingVelocity(id: Long): Offset = pendingVelocity.remove(id) ?: Offset.Zero

    /**
     * [sourceId] 연잎이 ([centerX], [centerY])에서 [velocity]로 날아가는 이번 프레임에, 실제로 맞닿아
     * 다가가는 중인 다른 연잎마다 충격을 나눈다(같은 질량, 반발 [POND_CONTACT_RESTITUTION]).
     * - 상대가 쉬는 중이면 [hits]로 비행을 시작시키고, 날고 있으면 그 비행 속도에 더한다.
     * - 상대가 붙잡혀 있으면 상대는 그대로, 이쪽만 벽처럼 튕긴다.
     * - 같은 두 연잎은 [POND_PAIR_COOLDOWN_MS] 동안 다시 치지 않고, 단계가
     *   [POND_CHAIN_MAX_GENERATION]을 넘거나 전달 속도가 너무 약하면 상대에게 넘기지 않는다.
     * - 충돌 지점에 다가간 속도만큼 큰 물살을 남겨 수면이 출렁이게 한다.
     * 이쪽 연잎의 충돌 뒤 속도를 돌려준다.
     */
    fun resolveContacts(
        sourceId: Long,
        centerX: Float,
        centerY: Float,
        velocity: Offset,
        generation: Int,
        nowMillis: Long,
        tuning: PondContactTuning
    ): Offset {
        val source = leaves[sourceId] ?: return velocity
        var vx = velocity.x
        var vy = velocity.y
        for (other in leaves.values) {
            if (other.id == sourceId || other.radius <= 0f) continue
            if (!pondLeafContact(centerX, centerY, vx, vy, source.radius, other.centerX, other.centerY, other.radius, contact)) {
                continue
            }
            val key = pairKey(sourceId, other.id)
            if (nowMillis < (pairCooldownUntil[key] ?: Long.MIN_VALUE)) continue
            pairCooldownUntil[key] = nowMillis + POND_PAIR_COOLDOWN_MS

            val nx = contact[0]
            val ny = contact[1]
            val approach = contact[2]
            val strength = (approach / tuning.maxTransferSpeedPx).coerceIn(0f, 1f)
            addRipple(
                Offset(centerX + nx * source.radius, centerY + ny * source.radius),
                tuning.minRipplePx + (tuning.maxRipplePx - tuning.minRipplePx) * strength
            )

            if (other.isHeld) {
                // 붙잡힌 연잎은 벽처럼 — 이쪽 속도의 접근 성분만 반사·감쇠.
                vx -= nx * approach * (1f + POND_CONTACT_RESTITUTION)
                vy -= ny * approach * (1f + POND_CONTACT_RESTITUTION)
                continue
            }

            val impulse = approach * (1f + POND_CONTACT_RESTITUTION) / 2f
            vx -= nx * impulse
            vy -= ny * impulse

            val transfer = impulse.coerceAtMost(tuning.maxTransferSpeedPx)
            val nextGeneration = generation + 1
            if (transfer < tuning.minTransferSpeedPx || nextGeneration > POND_CHAIN_MAX_GENERATION) continue

            val push = Offset(nx * transfer, ny * transfer)
            if (other.isFlying) {
                pendingVelocity[other.id] = (pendingVelocity[other.id] ?: Offset.Zero) + push
            } else {
                hits.tryEmit(PondHit(other.id, push, nextGeneration))
            }
        }
        return Offset(vx, vy)
    }

    private fun pairKey(a: Long, b: Long): Long {
        val low = minOf(a, b)
        val high = maxOf(a, b)
        return low * 1_000_003L + high
    }

    /** 연못 모드를 끄거나 화면을 벗어날 때 이 그리드의 모든 임시 상태를 정리한다. */
    fun reset() {
        ripples.clear()
        waterTilt = 0f
        pairCooldownUntil.clear()
        pendingVelocity.clear()
    }
}
