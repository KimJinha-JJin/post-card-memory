package com.postcardmemory.ui.detail

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** [DecorationEditHistory]를 직접 실행해 기록 규칙을 검증한다. 화면 연결은 다른 테스트의 몫이다. */
class DecorationEditHistoryTest {

    private fun history(limit: Int = 50) = DecorationEditHistory<String>(limit)

    @Test
    fun newHistory_hasNothingToUndoOrRedo() {
        val history = history()

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals(emptyList<String>(), history.retainedSnapshots())
    }

    @Test
    fun undoOnEmptyHistory_returnsNullAndDoesNotKeepCurrentForRedo() {
        val history = history()

        assertNull(history.undo("현재"))

        assertFalse(history.canRedo)
        assertEquals(emptyList<String>(), history.retainedSnapshots())
    }

    @Test
    fun redoOnEmptyHistory_returnsNullAndDoesNotKeepCurrentForUndo() {
        val history = history()

        assertNull(history.redo("현재"))

        assertFalse(history.canUndo)
        assertEquals(emptyList<String>(), history.retainedSnapshots())
    }

    @Test
    fun record_enablesUndoAndNotRedo() {
        val history = history()

        history.record("A")

        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun undoThenRedo_walksStatesBackAndForth() {
        val history = history()
        history.record("A")

        assertEquals("A", history.undo("B"))
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)

        assertEquals("B", history.redo("A"))
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun consecutiveUndoAndRedo_followLastInFirstOutOrder() {
        val history = history()
        history.record("A")
        history.record("B")
        history.record("C")

        assertEquals("C", history.undo("D"))
        assertEquals("B", history.undo("C"))
        assertEquals("A", history.undo("B"))
        assertNull(history.undo("A"))

        assertEquals("B", history.redo("A"))
        assertEquals("C", history.redo("B"))
        assertEquals("D", history.redo("C"))
        assertNull(history.redo("D"))
    }

    @Test
    fun newEditAfterUndo_discardsRedoButKeepsRemainingUndo() {
        val history = history()
        history.record("A")
        history.record("B")
        assertEquals("B", history.undo("C"))
        assertTrue(history.canRedo)

        history.record("B2")

        assertFalse(history.canRedo)
        assertNull(history.redo("현재"))
        assertEquals(listOf("A", "B2"), history.retainedSnapshots())
    }

    @Test
    fun recordingExactlyUpToLimit_dropsNothing() {
        val history = history(limit = 3)

        val dropped = (0 until 3).map { history.record("S$it") }

        assertEquals(listOf(false, false, false), dropped)
        assertEquals(listOf("S0", "S1", "S2"), history.retainedSnapshots())
    }

    @Test
    fun recordingPastLimit_dropsOnlyOldestAndReportsEachDrop() {
        val history = history(limit = 3)

        val dropped = (0 until 5).map { history.record("S$it") }

        assertEquals(listOf(false, false, false, true, true), dropped)
        assertEquals(listOf("S2", "S3", "S4"), history.retainedSnapshots())
    }

    @Test
    fun droppedSnapshot_cannotBeReachedByUndo() {
        val history = history(limit = 2)
        history.record("A")
        history.record("B")
        history.record("C")

        assertEquals("C", history.undo("D"))
        assertEquals("B", history.undo("C"))
        assertNull(history.undo("B"))
    }

    @Test
    fun limitOfOne_keepsOnlyLatestSnapshot() {
        val history = history(limit = 1)
        history.record("A")
        history.record("B")

        assertEquals("B", history.undo("C"))
        assertNull(history.undo("B"))
    }

    @Test
    fun redoAtFullUndo_doesNotDropAnything() {
        val history = history(limit = 2)
        history.record("A")
        history.record("B")
        assertEquals("B", history.undo("C"))

        assertEquals("C", history.redo("B"))

        assertEquals(listOf("A", "B"), history.retainedSnapshots())
    }

    @Test
    fun clear_removesUndoAndRedo() {
        val history = history()
        history.record("A")
        history.record("B")
        assertEquals("B", history.undo("C"))
        assertTrue(history.canUndo)
        assertTrue(history.canRedo)

        history.clear()

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertNull(history.undo("현재"))
        assertNull(history.redo("현재"))
        assertEquals(emptyList<String>(), history.retainedSnapshots())
    }

    @Test
    fun historyIsUsableAgainAfterClear() {
        val history = history()
        history.record("A")
        history.clear()

        history.record("B")

        assertEquals("B", history.undo("C"))
        assertEquals("C", history.redo("B"))
    }

    @Test
    fun retainedSnapshots_listsUndoOldestFirstThenRedoOldestFirst() {
        val history = history()
        history.record("A")
        history.record("B")
        history.record("C")
        assertEquals("C", history.undo("D"))
        assertEquals("B", history.undo("C"))

        assertEquals(listOf("A", "D", "C"), history.retainedSnapshots())
    }

    @Test
    fun retainedSnapshots_isCopyThatDoesNotFollowOrAffectHistory() {
        val history = history()
        history.record("A")

        val before = history.retainedSnapshots()
        history.record("B")
        assertEquals(listOf("A"), before)

        (before as MutableList<String>).clear()
        assertEquals(listOf("A", "B"), history.retainedSnapshots())
    }

    @Test
    fun snapshotsAreReturnedAsTheSameInstance() {
        val recorded = listOf("항목")
        val current = listOf("현재")
        val history = DecorationEditHistory<List<String>>(limit = 5)
        history.record(recorded)

        assertSame(recorded, history.undo(current))
        assertSame(current, history.redo(recorded))
    }

    @Test
    fun nonPositiveLimit_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { history(limit = 0) }
        assertThrows(IllegalArgumentException::class.java) { history(limit = -1) }
    }

    /**
     * 전환 전 DetailViewModel의 5종 구현을 그대로 옮긴 비교 기준. 이 기준은 옛 코드의 복제본이지
     * ViewModel 실행이 아니다. 옛 코드가 넘침 시점에 파일 정리를 불렀으므로 그 순간의 다시하기
     * 개수도 함께 남긴다.
     */
    private class LegacyDequeHistory(private val limit: Int) {
        val undoStack = ArrayDeque<String>()
        val redoStack = ArrayDeque<String>()
        var redoSizeAtLastDrop: Int? = null

        /** 옛 마스킹테이프 구현에서 sweep이 불리던 자리를 true로 알린다. */
        fun record(snapshot: String): Boolean {
            undoStack.addLast(snapshot)
            var dropped = false
            if (undoStack.size > limit) {
                undoStack.removeFirst()
                redoSizeAtLastDrop = redoStack.size
                dropped = true
            }
            redoStack.clear()
            return dropped
        }

        fun undo(current: String): String? {
            val previous = undoStack.removeLastOrNull() ?: return null
            redoStack.addLast(current)
            return previous
        }

        fun redo(current: String): String? {
            val next = redoStack.removeLastOrNull() ?: return null
            undoStack.addLast(current)
            return next
        }

        fun clear() {
            undoStack.clear()
            redoStack.clear()
        }
    }

    /**
     * 무작위 편집 순서에서 옛 구현과 결과·기록·넘침 시점이 모두 같은지 본다. 옛 코드는 다시하기를
     * 비우기 전에 파일 정리를 불렀고 새 코드는 비운 뒤에 부른다. 넘침 때 다시하기가 늘 비어 있으면
     * 정리가 보는 참조가 같으므로 그 전제도 함께 확인한다.
     */
    @Test
    fun randomEditSequences_matchLegacyImplementationStepByStep() {
        for ((limit, seed) in listOf(1 to 1, 2 to 2, 5 to 3, 50 to 4)) {
            val legacy = LegacyDequeHistory(limit)
            val history = DecorationEditHistory<String>(limit)
            var current = "초기"
            var dropCount = 0
            val random = Random(seed)

            repeat(20_000) { step ->
                val label = "limit=$limit seed=$seed step=$step"
                when (random.nextInt(100)) {
                    in 0 until 55 -> {
                        val legacyDropped = legacy.record(current)
                        assertEquals(label, legacyDropped, history.record(current))
                        if (legacyDropped) {
                            dropCount++
                            assertEquals(label, 0, legacy.redoSizeAtLastDrop)
                        }
                        current = "편집$step"
                    }
                    in 55 until 77 -> {
                        val expected = legacy.undo(current)
                        assertEquals(label, expected, history.undo(current))
                        if (expected != null) current = expected
                    }
                    in 77 until 99 -> {
                        val expected = legacy.redo(current)
                        assertEquals(label, expected, history.redo(current))
                        if (expected != null) current = expected
                    }
                    else -> {
                        legacy.clear()
                        history.clear()
                    }
                }

                assertEquals(label, legacy.undoStack.isNotEmpty(), history.canUndo)
                assertEquals(label, legacy.redoStack.isNotEmpty(), history.canRedo)
                assertEquals(label, legacy.undoStack + legacy.redoStack, history.retainedSnapshots())
                assertTrue(label, history.retainedSnapshots().size <= limit)
            }

            assertTrue("limit=$limit: 넘침 경로를 한 번도 지나지 않았다", dropCount > 0)
        }
    }
}
