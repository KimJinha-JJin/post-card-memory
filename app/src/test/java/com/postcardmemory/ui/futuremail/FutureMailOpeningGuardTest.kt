package com.postcardmemory.ui.futuremail

import com.postcardmemory.testsupport.readStructureTestSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 78일차 P1-3: 묶음 개봉 도중 예외가 나면 "개봉 중" 표시가 남아 재시도가
 * 영구히 막히던 문제.
 *
 * finally를 지우면 아래 테스트 대부분이 실패한다.
 */
class FutureMailOpeningGuardTest {

    private val deliverAt = 1_800_000_000_000L

    @Test
    fun beginOrSkip_secondCallForTheSameGroupIsSkipped() {
        val guard = FutureMailOpeningGuard()

        assertTrue(guard.beginOrSkip(deliverAt))
        assertFalse(guard.beginOrSkip(deliverAt))
        assertTrue(guard.isOpening(deliverAt))
    }

    @Test
    fun beginOrSkip_differentGroupsDoNotBlockEachOther() {
        val guard = FutureMailOpeningGuard()

        assertTrue(guard.beginOrSkip(deliverAt))
        assertTrue(guard.beginOrSkip(deliverAt + 86_400_000L))
    }

    @Test
    fun releasingAfter_successfulOpen_clearsTheMark() = runBlocking {
        val guard = FutureMailOpeningGuard()
        guard.beginOrSkip(deliverAt)

        guard.releasingAfter(deliverAt) { /* 정상 개봉 */ }

        assertFalse(guard.isOpening(deliverAt))
    }

    @Test
    fun releasingAfter_failedOpen_stillClearsTheMarkAndRethrows() = runBlocking {
        val guard = FutureMailOpeningGuard()
        guard.beginOrSkip(deliverAt)

        var thrown: Throwable? = null
        try {
            guard.releasingAfter(deliverAt) {
                throw IllegalStateException("두 번째 엽서 update 실패")
            }
        } catch (error: IllegalStateException) {
            thrown = error
        }

        assertEquals("두 번째 엽서 update 실패", thrown?.message)
        assertFalse(guard.isOpening(deliverAt))
    }

    @Test
    fun releasingAfter_failedOpen_userCanRetryTheSameGroup() = runBlocking {
        val guard = FutureMailOpeningGuard()
        guard.beginOrSkip(deliverAt)

        runCatching {
            guard.releasingAfter(deliverAt) {
                throw IllegalStateException("중간 실패")
            }
        }

        // 재시도가 막히지 않아야 한다 — 이것이 이 가드의 존재 이유다.
        assertTrue(guard.beginOrSkip(deliverAt))
    }

    @Test
    fun releasingAfter_cancelledOpen_alsoClearsTheMark() = runBlocking {
        val guard = FutureMailOpeningGuard()
        guard.beginOrSkip(deliverAt)

        runCatching {
            guard.releasingAfter(deliverAt) {
                throw CancellationException("화면 이탈로 취소")
            }
        }

        assertFalse(guard.isOpening(deliverAt))
        assertTrue(guard.beginOrSkip(deliverAt))
    }
}

/**
 * 가드 테스트로는 "묶음을 한 번에 여는가"를 확인할 수 없다(원자성은 SQLite
 * 단일 문장 수준의 성질이고, 실제 Room DB 검증은 실기기 계측 테스트가
 * 필요해 이번 범위에서 금지된다). 재발하면 곧바로 부분 개봉으로 돌아가는
 * 지점 하나만 소스 수준에서 못박아 둔다.
 */
class FutureMailGroupOpenAtomicityStructureTest {

    private fun source(name: String): String =
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/$name",
                "app/src/main/java/com/postcardmemory/$name"
            )
        )

    @Test
    fun viewModelOpensTheWholeGroupWithOneCall_notOncePerPostcardId() {
        val vm = source("ui/futuremail/FutureMailboxViewModel.kt")

        assertTrue(vm.contains("repository.openFutureMailGroup(group.postcardIds)"))
        // id마다 따로 여는 루프가 돌아오면 중간 실패 시 부분 개봉이 재발한다.
        assertFalse(vm.contains("group.postcardIds.forEach"))
    }

    @Test
    fun daoOpensTheGroupInASingleUpdateStatement() {
        val dao = source("data/PostcardDao.kt")

        assertTrue(dao.contains("WHERE id IN (:ids)"))
        assertEquals(
            1,
            Regex("SET futureMailState = 'NONE'").findAll(dao).count()
        )
    }
}
