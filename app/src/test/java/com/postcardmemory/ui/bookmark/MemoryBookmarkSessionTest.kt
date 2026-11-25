package com.postcardmemory.ui.bookmark

import androidx.lifecycle.SavedStateHandle
import com.postcardmemory.data.BookmarkDesign
import com.postcardmemory.data.BookmarkPaper
import com.postcardmemory.data.BookmarkShape
import com.postcardmemory.data.BookmarkTrace
import com.postcardmemory.data.MemoryBookmark
import com.postcardmemory.data.MemoryBookmarkDao
import com.postcardmemory.data.MemoryBookmarkRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 제작 화면 한 번의 흐름. 고르는 동안에는 DB에 쓰지 않고, 저장이 성공해야만 끼우는 연출로 넘어가는지 본다. */
class MemoryBookmarkSessionTest {

    private class FakeDao : MemoryBookmarkDao {
        val rows = MutableStateFlow<Map<Long, MemoryBookmark>>(emptyMap())
        var writes = 0
        var removes = 0
        var failWrites = false
        var failReads = false
        var gate: CompletableDeferred<Unit>? = null

        override fun observeAll(): Flow<List<MemoryBookmark>> = rows.map { it.values.toList() }
        override fun observe(postcardId: Long): Flow<MemoryBookmark?> = rows.map { it[postcardId] }
        override suspend fun get(postcardId: Long): MemoryBookmark? {
            check(!failReads) { "read" }
            return rows.value[postcardId]
        }

        override suspend fun upsert(bookmark: MemoryBookmark) {
            gate?.await()
            check(!failWrites) { "disk" }
            writes++
            rows.value = rows.value + (bookmark.postcardId to bookmark)
        }

        override suspend fun remove(postcardId: Long) {
            gate?.await()
            check(!failWrites) { "disk" }
            removes++
            rows.value = rows.value - postcardId
        }
    }

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Unconfined)
    private val dao = FakeDao()
    private val handle = SavedStateHandle()
    private fun session(savedState: SavedStateHandle = handle) =
        MemoryBookmarkSession(scope, MemoryBookmarkRepository(dao), savedState)

    @After fun tearDown() = job.cancel()

    private val rose = BookmarkDesign(BookmarkShape.CLASSIC, BookmarkPaper.ROSE, BookmarkTrace.LINES)

    @Test fun newBookmarkStartsFromApprovedDefaultsAndWritesNothingWhileChoosing() {
        val s = session()
        s.open(5)
        assertEquals(BookmarkDesign(), s.state.value!!.draft)
        assertFalse(s.state.value!!.hadBookmark)
        s.changeDraft { rose }
        assertEquals(rose, s.state.value!!.draft)
        assertEquals(0, dao.writes)
        s.close()
        assertNull(s.state.value)
        assertEquals(0, dao.writes)
    }

    @Test fun insertSavesFirstThenPlaysThenFinishes() {
        val s = session()
        s.open(5)
        s.changeDraft { rose }
        val gate = CompletableDeferred<Unit>().also { dao.gate = it }
        s.insert()
        assertEquals(BookmarkStep.SAVING, s.state.value!!.step)
        s.insert() // 연타
        s.changeDraft { BookmarkDesign() } // 저장 중 변경
        s.close() // 저장 중 뒤로가기
        assertEquals(BookmarkStep.SAVING, s.state.value!!.step)
        assertEquals(rose, s.state.value!!.draft)
        gate.complete(Unit)
        assertEquals(BookmarkStep.INSERTING, s.state.value!!.step)
        assertEquals(1, dao.writes)
        assertEquals(rose, dao.rows.value[5L]!!.designOrNull())
        s.insert()
        assertEquals(1, dao.writes)
        s.onInsertionShown()
        assertEquals(BookmarkStep.DONE, s.state.value!!.step)
        assertTrue(s.state.value!!.hadBookmark)
    }

    @Test fun failedSaveNeverPlaysTheInsertionAndKeepsTheChosenPaper() {
        val s = session()
        s.open(5)
        s.changeDraft { rose }
        dao.failWrites = true
        s.insert()
        val failed = s.state.value!!
        assertEquals(BookmarkStep.EDITING, failed.step)
        assertNotNull(failed.error)
        assertEquals(rose, failed.draft)
        assertEquals(0, failed.insertToken)
        dao.failWrites = false
        s.insert()
        assertEquals(BookmarkStep.INSERTING, s.state.value!!.step)
        assertNull(s.state.value!!.error)
    }

    @Test fun cancellingAnEditKeepsThePreviousBookmark() {
        dao.rows.value = mapOf(5L to MemoryBookmark.from(5, rose))
        val s = session()
        s.open(5)
        assertEquals(rose, s.state.value!!.draft)
        assertTrue(s.state.value!!.hadBookmark)
        s.changeDraft { BookmarkDesign() }
        s.close()
        assertEquals(rose, dao.rows.value[5L]!!.designOrNull())
    }

    @Test fun removeDeletesOnlyThisBookmarkAndReportsFailureWithoutClosing() = runBlocking {
        val other = MemoryBookmark.from(6, BookmarkDesign())
        dao.rows.value = mapOf(5L to MemoryBookmark.from(5, rose), 6L to other)
        val s = session()
        s.open(5)
        dao.failWrites = true
        s.remove()
        assertEquals(BookmarkStep.EDITING, s.state.value!!.step)
        assertNotNull(s.state.value!!.error)
        dao.failWrites = false
        s.remove()
        assertNull(s.state.value)
        assertEquals(mapOf(6L to other), dao.rows.value)
        assertEquals("책갈피를 뺐어. 엽서는 그대로야.", s.messages.first())
    }

    @Test fun removeIsIgnoredWithoutABookmark() {
        val s = session()
        s.open(5)
        s.remove()
        assertEquals(0, dao.removes)
        assertEquals(BookmarkStep.EDITING, s.state.value!!.step)
    }

    @Test fun editAgainReturnsToTheSavedDesignAndPlaysANewToken() {
        val s = session()
        s.open(5)
        s.insert()
        s.onInsertionShown()
        s.editAgain()
        s.changeDraft { rose }
        s.insert()
        assertEquals(2, s.state.value!!.insertToken)
        assertEquals(rose, dao.rows.value[5L]!!.designOrNull())
    }

    @Test fun futureFormatIsNeitherOpenedNorOverwritten() = runBlocking {
        val future = MemoryBookmark(5, "FUTURE", "CUSTOM", "OTHER", 2)
        dao.rows.value = mapOf(5L to future)
        val s = session()
        s.open(5)
        assertNull(s.state.value)
        assertEquals("새 버전에서 만든 책갈피라 여기서는 바꿀 수 없어.", s.messages.first())
        assertEquals(future, dao.rows.value[5L])
    }

    @Test fun failedOpenCanBeRetried() = runBlocking {
        val s = session()
        dao.failReads = true
        s.open(5)
        assertNull(s.state.value)
        dao.failReads = false
        s.open(5)
        assertNotNull(s.state.value)
        assertEquals("책갈피를 불러오지 못했어. 다시 시도해 줘.", s.messages.first())
    }

    @Test fun choicesSurviveRecreationAndUnsavedWorkIsNotShownAsDone() {
        session().apply { open(5); changeDraft { rose } }
        val restored = session()
        assertEquals(rose, restored.state.value!!.draft)
        assertEquals(BookmarkStep.EDITING, restored.state.value!!.step)

        dao.gate = CompletableDeferred()
        restored.insert() // 저장이 끝나기 전에 프로세스가 사라진 상황
        assertEquals(BookmarkStep.EDITING, session().state.value!!.step)
    }

    @Test fun savedBookmarkIsRestoredAsDoneAndClosedStudioStaysClosed() {
        session().apply { open(5); insert() }
        val restored = session()
        assertEquals(BookmarkStep.DONE, restored.state.value!!.step)
        assertTrue(restored.state.value!!.hadBookmark)
        // 번호가 이어져야 다음 끼우기가 '이미 본 연출'로 건너뛰어지지 않아.
        assertEquals(1, restored.state.value!!.insertToken)
        restored.editAgain()
        restored.insert()
        assertEquals(2, restored.state.value!!.insertToken)
        restored.close()
        assertNull(session().state.value)
    }

    @Test fun cancelledScopeDoesNotTurnIntoASaveError() {
        val s = session()
        s.open(5)
        dao.gate = CompletableDeferred()
        s.insert()
        job.cancel()
        assertEquals(BookmarkStep.SAVING, s.state.value!!.step)
        assertNull(s.state.value!!.error)
        assertEquals(0, dao.writes)
    }
}
