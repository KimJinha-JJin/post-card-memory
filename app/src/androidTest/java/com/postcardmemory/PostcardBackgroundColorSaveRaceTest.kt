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
 * `ui.detail` 패키지)가 같은 두 시나리오를 FakeViewModel로 이미 재현하고 있고,
 * 이 파일은 그중 "현재 실제 UI 호출자가 있는" 두 시나리오만 골라 production
 * 코드로 다시 검증한다 — replica는 79일차 분류상 category C(Context/Room/Hilt
 * 바인딩 때문에 JVM에서 직접 호출 불가)로 남아 있고, 이 파일이 그 production
 * 연결 대응이다. `saveBackgroundImagePath`가 겨냥하는 경로 컬럼 경합은 현재
 * 그 값을 쓰는 실제 호출자가 없어(replica 자체 주석 참고) 여기서는 재현하지
 * 않았다 — 존재하지 않는 호출 경로를 꾸며내는 대신 미검증으로 남긴다.
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

    @Test fun failedColorSave_doesNotRollbackNewerColor() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PostcardDatabase::class.java).build()
        val dao = db.postcardDao()
        val id = dao.insertPostcard(Postcard(imagePath = "test-only", title = "test"))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val gatedDao = object : PostcardDao by dao {
            override suspend fun updatePostcardBackground(
                id: Long,
                backgroundColorArgb: Long,
                backgroundImagePath: String?
            ) {
                if (++calls == 1) {
                    entered.complete(Unit)
                    release.await()
                    throw java.io.IOException("injected first-write failure")
                }
                dao.updatePostcardBackground(id, backgroundColorArgb, backgroundImagePath)
            }
        }
        val vm = viewModel(gatedDao)
        try {
            withTimeout(10_000) {
                withContext(Dispatchers.Main) { vm.loadPostcard(id) }
                vm.postcard.filterNotNull().first()

                withContext(Dispatchers.Main) { vm.updateBackgroundColor(0xFFFF0000L) }
                entered.await()

                // 첫 저장이 아직 실패로 끝나기 전에, 사용자가 바로 다른 색을 고른다.
                withContext(Dispatchers.Main) { vm.updateBackgroundColor(0xFF00FF00L) }
                release.complete(Unit)

                withContext(Dispatchers.Main) { vm.awaitPendingStyleSaves() }

                assertEquals(0xFF00FF00L, vm.postcard.value!!.backgroundColorArgb)
                assertEquals(0xFF00FF00L, dao.getPostcardById(id)!!.backgroundColorArgb)
            }
        } finally {
            release.complete(Unit)
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
}
