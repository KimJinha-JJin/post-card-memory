package com.postcardmemory

import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardDao
import com.postcardmemory.data.PostcardDatabase
import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.ui.detail.DetailViewModel
import com.postcardmemory.utils.PostcardDeletionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DetailViewModel.updateBackgroundColor의 경합 안전성을 실제 DetailViewModel +
 * 실제 in-memory Room으로 검증한다. `BackgroundColorSaveRaceTest`(JVM replica,
 * `ui.detail` 패키지)가 같은 계열의 시나리오를 FakeViewModel로 이미 재현하고
 * 있고, 이 파일은 그중 "현재 실제 UI 호출자가 있는" 시나리오만 골라 production
 * 코드로 다시 검증한다 — replica는 79일차 분류상 category C(Context/Room/Hilt
 * 바인딩 때문에 JVM에서 직접 호출 불가)로 남아 있고, 이 파일이 그 production
 * 연결 대응이다. `saveBackgroundImagePath`가 겨냥하는 경로 컬럼 경합은 현재
 * 그 값을 쓰는 실제 호출자가 없어(replica 자체 주석 참고) 여기서는 재현하지
 * 않았다 — 존재하지 않는 호출 경로를 꾸며내는 대신 미검증으로 남긴다.
 *
 * 79일차 후속 감사에서 바로잡은 점: production `updateBackgroundColor`는
 * 매 호출마다 `backgroundColorSaveJob?.cancel()`로 **이전 저장 Job 자체를
 * 취소**한다(같은 필드를 대상으로 하는 Job이 하나뿐이라 replica가 가정한
 * "두 저장이 서로 독립적으로 동시에 실행되다 하나가 실패로 끝난다"는 구조는
 * production에서 재현되지 않는다 — 새 호출이 오면 이전 Job은 실패가 아니라
 * 취소로 끝난다). 그래서 아래 첫 테스트는 "실패가 최신 값을 되돌리지 않는다"가
 * 아니라, 실제로 보장되는 것 — **아직 커밋되지 않은 오래된 저장은 새 저장이
 * 오면 취소되어 절대 DB에 반영되지 않는다** — 를 검증하도록 다시 짰다.
 */
class PostcardBackgroundColorSaveRaceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun viewModel(dao: PostcardDao): DetailViewModel {
        val repository = PostcardRepository(dao)
        return DetailViewModel(
            repository,
            PostcardDeletionManager(context, repository),
            context,
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }

    @Test fun staleInFlightColorSave_isCancelledAndNeverReachesTheDatabase() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PostcardDatabase::class.java).build()
        val dao = db.postcardDao()
        val id = dao.insertPostcard(Postcard(imagePath = "test-only", title = "test"))
        val staleEntered = CompletableDeferred<Unit>()
        // 의도적으로 절대 완료하지 않는다 — 취소되지 않는다면 이 테스트는
        // production의 2초 PENDING_STYLE_SAVE_TIMEOUT_MS 안에 실패로 드러난다.
        val staleNeverReleases = CompletableDeferred<Unit>()
        val actuallyWrittenColors = mutableListOf<Long>()
        val gatedDao = object : PostcardDao by dao {
            override suspend fun updatePostcardBackground(
                id: Long,
                backgroundColorArgb: Long,
                backgroundImagePath: String?
            ) {
                if (backgroundColorArgb == COLOR_STALE) {
                    staleEntered.complete(Unit)
                    staleNeverReleases.await()
                }
                actuallyWrittenColors += backgroundColorArgb
                dao.updatePostcardBackground(id, backgroundColorArgb, backgroundImagePath)
            }
        }
        val vm = viewModel(gatedDao)
        try {
            withTimeout(10_000) {
                withContext(Dispatchers.Main) { vm.loadPostcard(id) }
                vm.postcard.filterNotNull().first()

                withContext(Dispatchers.Main) { vm.updateBackgroundColor(COLOR_STALE) }
                staleEntered.await() // 오래된 저장이 Mutex를 쥔 채 멈춰 있다.

                // 아직 멈춰 있는 저장이 커밋되기 전에, 사용자가 바로 다른 색을 고른다.
                // production은 이 호출에서 backgroundColorSaveJob?.cancel()로
                // 위 멈춰 있는 Job을 취소한다.
                withContext(Dispatchers.Main) { vm.updateBackgroundColor(COLOR_FRESH) }

                withContext(Dispatchers.Main) { vm.awaitPendingStyleSaves() }

                // 취소된 오래된 저장은 DB에 절대 닿지 않는다 — 실제로 커밋된 색은
                // 최신 색 하나뿐이어야 한다.
                assertEquals(listOf(COLOR_FRESH), actuallyWrittenColors)
                assertEquals(COLOR_FRESH, vm.postcard.value!!.backgroundColorArgb)
                assertEquals(COLOR_FRESH, dao.getPostcardById(id)!!.backgroundColorArgb)
            }
        } finally {
            staleNeverReleases.complete(Unit)
            vm.viewModelScope.cancel()
            db.close()
        }
    }

    @Test fun afterFailure_nextColorSaveSucceedsNormally() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PostcardDatabase::class.java).build()
        val dao = db.postcardDao()
        val id = dao.insertPostcard(Postcard(imagePath = "test-only", title = "test"))
        var calls = 0
        val failingOnceDao = object : PostcardDao by dao {
            override suspend fun updatePostcardBackground(
                id: Long,
                backgroundColorArgb: Long,
                backgroundImagePath: String?
            ) {
                if (++calls == 1) throw java.io.IOException("injected first-write failure")
                dao.updatePostcardBackground(id, backgroundColorArgb, backgroundImagePath)
            }
        }
        val vm = viewModel(failingOnceDao)
        try {
            withTimeout(10_000) {
                withContext(Dispatchers.Main) { vm.loadPostcard(id) }
                vm.postcard.filterNotNull().first()
                val initialColor = vm.postcard.value!!.backgroundColorArgb

                withContext(Dispatchers.Main) {
                    vm.updateBackgroundColor(0xFFFF0000L)
                    vm.awaitPendingStyleSaves()
                }
                // 실패한 저장은 자기 낙관적 갱신만 되돌린다.
                assertEquals(initialColor, vm.postcard.value!!.backgroundColorArgb)
                assertEquals(initialColor, dao.getPostcardById(id)!!.backgroundColorArgb)

                withContext(Dispatchers.Main) {
                    vm.updateBackgroundColor(0xFF00FF00L)
                    vm.awaitPendingStyleSaves()
                }
                assertEquals(0xFF00FF00L, vm.postcard.value!!.backgroundColorArgb)
                assertEquals(0xFF00FF00L, dao.getPostcardById(id)!!.backgroundColorArgb)
            }
        } finally {
            vm.viewModelScope.cancel()
            db.close()
        }
    }

    private companion object {
        const val COLOR_STALE = 0xFFFF0000L
        const val COLOR_FRESH = 0xFF00FF00L
    }
}
