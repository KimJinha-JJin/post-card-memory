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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class PostcardBackSaveTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun viewModel(dao: PostcardDao): DetailViewModel {
        val repository = PostcardRepository(dao)
        return DetailViewModel(repository, PostcardDeletionManager(context, repository), context)
    }

    @Test fun delayedFailedBodySaveCannotUndoNewTextAndTimeSurvivesReload() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PostcardDatabase::class.java).build()
        val dao = db.postcardDao()
        val id = dao.insertPostcard(Postcard(imagePath = "test-only", title = "test"))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var writes = 0
        val gatedDao = object : PostcardDao by dao {
            override suspend fun updatePostcardBackMessage(id: Long, backMessage: String, writtenAt: Long?, offsetMinutes: Int?) {
                if (++writes == 1) {
                    entered.complete(Unit)
                    release.await()
                    throw java.io.IOException("injected first-write failure")
                }
                dao.updatePostcardBackMessage(id, backMessage, writtenAt, offsetMinutes)
            }
        }
        val vm = viewModel(gatedDao)
        try {
            withTimeout(10_000) {
                withContext(Dispatchers.Main) { vm.loadPostcard(id) }
                vm.postcard.filterNotNull().first()
                withContext(Dispatchers.Main) { vm.updateBackMessage("처음 쓴 글") }
                entered.await()
                val firstTime = vm.postcard.value!!.backWrittenAt
                assertNotNull(firstTime)
                withContext(Dispatchers.Main) {
                    vm.updateBackMessage("나중에 쓴 글")
                    vm.updateBackPostscript("추신도 저장")
                    vm.loadPostcard(id) // recreation must not reload the still-empty DB row
                    assertEquals("나중에 쓴 글", vm.postcard.value!!.backMessage)
                }
                release.complete(Unit)
                withContext(Dispatchers.Main) { vm.awaitPendingStyleSaves() }
                val saved = dao.getPostcardById(id)!!
                assertEquals("나중에 쓴 글", saved.backMessage)
                assertEquals("추신도 저장", saved.backPostscript)
                assertEquals(firstTime, saved.backWrittenAt)
                assertEquals(saved.backMessage, vm.postcard.value!!.backMessage)
                val recreated = viewModel(dao)
                try {
                    withContext(Dispatchers.Main) { recreated.loadPostcard(id) }
                    assertEquals(saved, recreated.postcard.filterNotNull().first())
                } finally { recreated.viewModelScope.cancel() }
            }
        } finally {
            release.complete(Unit)
            vm.viewModelScope.cancel()
            db.close()
        }
    }

    @Test fun failedPostscriptSaveRestoresPersistedTextWithoutStartingWritingRecord() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PostcardDatabase::class.java).build()
        val dao = db.postcardDao()
        val id = dao.insertPostcard(Postcard(imagePath = "test-only", title = "test", backPostscript = "보존할 추신"))
        val failingDao = object : PostcardDao by dao {
            override suspend fun updatePostcardBackPostscript(id: Long, postscript: String?) {
                throw java.io.IOException("injected postscript failure")
            }
        }
        val vm = viewModel(failingDao)
        try {
            withTimeout(10_000) {
                withContext(Dispatchers.Main) { vm.loadPostcard(id) }
                vm.postcard.filterNotNull().first()
                withContext(Dispatchers.Main) {
                    vm.updateBackPostscript("저장 실패할 추신")
                    vm.awaitPendingStyleSaves()
                }
                assertEquals("보존할 추신", vm.postcard.value!!.backPostscript)
                assertEquals("보존할 추신", dao.getPostcardById(id)!!.backPostscript)
                assertNull(vm.postcard.value!!.backWrittenAt)
            }
        } finally {
            vm.viewModelScope.cancel()
            db.close()
        }
    }
}
