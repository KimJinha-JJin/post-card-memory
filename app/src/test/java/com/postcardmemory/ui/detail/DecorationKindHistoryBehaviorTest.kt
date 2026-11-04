package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import com.postcardmemory.utils.DoodlePoint
import com.postcardmemory.utils.DoodleStroke
import com.postcardmemory.utils.DoodleStrokeWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 꾸미기 5종을 실제 항목 타입과 실제 상한 상수로 [DecorationEditHistory]에 태워, 사용자가 겪는
 * 편집 순서대로 상태가 어떻게 변하는지 검증한다.
 *
 * DetailViewModel은 JVM에서 만들 수 없어(StructureTestSource 참고) [KindEditor]가 ViewModel의
 * 연결 방식(되돌리기 → 상태 복원 → 버튼 갱신 → 자동저장)을 옮겨 둔 복제본이다. 복제본과 실제
 * 코드가 같은 모양인지는 DecorationEditHistoryWiringStructureTest가 소스로 확인한다.
 */
class DecorationKindHistoryBehaviorTest {

    private data class Snapshot<I>(
        val items: List<I>,
        val selectedId: String?
    )

    /** ViewModel 한 종류분의 상태와 기록 연결. */
    private class KindEditor<I>(
        limit: Int,
        private val tracksSelection: Boolean
    ) {
        private val history = DecorationEditHistory<Snapshot<I>>(limit)

        var items: List<I> = emptyList()
        var selectedId: String? = null
        var canUndo = false
        var canRedo = false
        var autosaveCount = 0

        private fun current() = Snapshot(items, selectedId)

        private fun apply(snapshot: Snapshot<I>) {
            items = snapshot.items
            if (tracksSelection) selectedId = snapshot.selectedId
        }

        private fun updateAvailability() {
            canUndo = history.canUndo
            canRedo = history.canRedo
        }

        fun edit(item: I, selectId: String?) {
            history.record(current())
            updateAvailability()
            items = items + item
            selectedId = selectId
        }

        fun undo() {
            val previous = history.undo(current()) ?: return
            apply(previous)
            updateAvailability()
            autosaveCount++
        }

        fun redo() {
            val next = history.redo(current()) ?: return
            apply(next)
            updateAvailability()
            autosaveCount++
        }

        fun clearHistory() {
            history.clear()
            updateAvailability()
        }
    }

    private class Kind<I>(
        val name: String,
        val limit: Int,
        val tracksSelection: Boolean,
        val newItem: (Int) -> I
    ) {
        fun editor() = KindEditor<I>(limit, tracksSelection)
    }

    private val seal = Kind(
        name = "도장",
        limit = SEAL_HISTORY_LIMIT,
        tracksSelection = true,
        newItem = { PostcardSealItem(id = "seal$it", type = SealType.PIGEON_TRACK, offset = Offset(it.toFloat(), 1f)) }
    )
    private val textSticker = Kind(
        name = "텍스트 스티커",
        limit = TEXT_STICKER_HISTORY_LIMIT,
        tracksSelection = true,
        newItem = { TextStickerItem(id = "text$it", text = "글$it") }
    )
    private val maskingTape = Kind(
        name = "마스킹테이프",
        limit = MASKING_TAPE_HISTORY_LIMIT,
        tracksSelection = true,
        newItem = { MaskingTapeItem(id = "tape$it", style = MaskingTapeStyle.LAVENDER_DOT) }
    )
    private val labelSticker = Kind(
        name = "라벨 스티커",
        limit = LABEL_STICKER_HISTORY_LIMIT,
        tracksSelection = true,
        newItem = { LabelStickerItem(id = "label$it", text = "라벨$it") }
    )
    private val doodle = Kind(
        name = "낙서",
        limit = DOODLE_HISTORY_LIMIT,
        tracksSelection = false,
        newItem = {
            DoodleStroke(
                id = "stroke$it",
                points = listOf(DoodlePoint(0.1f, 0.2f)),
                colorArgb = 0xFF000000,
                width = DoodleStrokeWidth.THIN
            )
        }
    )

    private val kinds: List<Kind<*>> = listOf(seal, textSticker, maskingTape, labelSticker, doodle)

    private fun forEachKind(block: (Kind<Any?>) -> Unit) {
        kinds.forEach {
            @Suppress("UNCHECKED_CAST")
            block(it as Kind<Any?>)
        }
    }

    private fun <I> KindEditor<I>.editTimes(kind: Kind<I>, range: IntRange) {
        range.forEach { edit(kind.newItem(it), selectId = "선택$it") }
    }

    @Test
    fun everyKindKeepsItsHistoryLimitOfFifty() {
        forEachKind { kind ->
            assertEquals(kind.name, 50, kind.limit)
        }
    }

    @Test
    fun consecutiveUndoAndRedo_restoreEveryIntermediateState() {
        forEachKind { kind ->
            val editor = kind.editor()
            val states = mutableListOf(editor.items)
            (0 until 5).forEach {
                editor.edit(kind.newItem(it), selectId = "선택$it")
                states += editor.items
            }

            (4 downTo 0).forEach { index ->
                editor.undo()
                assertEquals("${kind.name} undo $index", states[index], editor.items)
            }
            (1..5).forEach { index ->
                editor.redo()
                assertEquals("${kind.name} redo $index", states[index], editor.items)
            }
        }
    }

    @Test
    fun undoOnEmptyHistory_changesNothingAndSkipsAutosave() {
        forEachKind { kind ->
            val editor = kind.editor()

            editor.undo()
            editor.redo()

            assertEquals(kind.name, emptyList<Any?>(), editor.items)
            assertEquals(kind.name, 0, editor.autosaveCount)
            assertFalse(kind.name, editor.canUndo)
            assertFalse(kind.name, editor.canRedo)
        }
    }

    @Test
    fun undoAndRedo_eachScheduleOneAutosave_butRecordingDoesNot() {
        forEachKind { kind ->
            val editor = kind.editor()

            editor.edit(kind.newItem(0), selectId = null)
            assertEquals(kind.name, 0, editor.autosaveCount)

            editor.undo()
            assertEquals(kind.name, 1, editor.autosaveCount)

            editor.redo()
            assertEquals(kind.name, 2, editor.autosaveCount)
        }
    }

    @Test
    fun buttonState_followsEachStep() {
        forEachKind { kind ->
            val editor = kind.editor()
            assertFalse(kind.name, editor.canUndo)
            assertFalse(kind.name, editor.canRedo)

            editor.edit(kind.newItem(0), selectId = null)
            assertTrue(kind.name, editor.canUndo)
            assertFalse(kind.name, editor.canRedo)

            editor.undo()
            assertFalse(kind.name, editor.canUndo)
            assertTrue(kind.name, editor.canRedo)

            editor.redo()
            assertTrue(kind.name, editor.canUndo)
            assertFalse(kind.name, editor.canRedo)
        }
    }

    @Test
    fun newEditAfterUndo_removesRedo() {
        forEachKind { kind ->
            val editor = kind.editor()
            editor.edit(kind.newItem(0), selectId = null)
            editor.edit(kind.newItem(1), selectId = null)
            editor.undo()
            assertTrue(kind.name, editor.canRedo)

            editor.edit(kind.newItem(2), selectId = null)
            val afterEdit = editor.items

            assertFalse(kind.name, editor.canRedo)
            editor.redo()
            assertEquals(kind.name, afterEdit, editor.items)
        }
    }

    @Test
    fun pastLimit_onlyTheLatestFiftyEditsCanBeUndone() {
        forEachKind { kind ->
            val editor = kind.editor()
            (0 until kind.limit + 5).forEach { editor.edit(kind.newItem(it), selectId = null) }

            var undoCount = 0
            while (editor.canUndo) {
                editor.undo()
                undoCount++
            }

            assertEquals(kind.name, kind.limit, undoCount)
            // 처음 다섯 번의 편집은 밀려나서 되돌릴 수 없다.
            assertEquals(
                kind.name,
                (0 until 5).map { kind.newItem(it) },
                editor.items
            )
        }
    }

    @Test
    fun selectionIsRestored_forTheFourKindsThatTrackIt() {
        listOf(seal, textSticker, maskingTape, labelSticker).forEach { kind ->
            @Suppress("UNCHECKED_CAST")
            val typed = kind as Kind<Any?>
            val editor = typed.editor()
            editor.edit(typed.newItem(0), selectId = "첫째")
            editor.edit(typed.newItem(1), selectId = "둘째")

            editor.undo()
            assertEquals(kind.name, "첫째", editor.selectedId)

            editor.redo()
            assertEquals(kind.name, "둘째", editor.selectedId)
        }
    }

    @Test
    fun doodleHasNoSelection_soUndoLeavesItAlone() {
        val editor = doodle.editor()
        editor.edit(doodle.newItem(0), selectId = "낙서엔 선택이 없다")

        editor.undo()

        assertEquals("낙서엔 선택이 없다", editor.selectedId)
        assertEquals(emptyList<DoodleStroke>(), editor.items)
    }

    /** 스냅샷은 복사되지 않으므로 복원된 목록은 기록 당시의 그 인스턴스다. */
    @Test
    fun undoRestoresTheExactListInstanceThatWasRecorded() {
        forEachKind { kind ->
            val editor = kind.editor()
            editor.edit(kind.newItem(0), selectId = null)
            val recorded = editor.items
            editor.edit(kind.newItem(1), selectId = null)

            editor.undo()

            assertSame(kind.name, recorded, editor.items)
        }
    }

    /**
     * 위처럼 참조를 공유해도 안전한 이유는 5종의 항목이 모두 불변이어서다. 항목에 var가 생기면
     * 편집이 과거 기록까지 바꾸게 되므로 여기서 먼저 실패한다. 목록을 제자리에서 고치는 호출이
     * 없다는 점은 코드 조사로 확인했고, 이 테스트가 잡아 주지는 않는다.
     */
    @Test
    fun allFiveItemTypesAreImmutable_soSharingSnapshotReferencesIsSafe() {
        val itemClasses = listOf(
            PostcardSealItem::class.java,
            TextStickerItem::class.java,
            MaskingTapeItem::class.java,
            LabelStickerItem::class.java,
            DoodleStroke::class.java,
            DoodlePoint::class.java
        )

        itemClasses.forEach { itemClass ->
            val setters = itemClass.declaredMethods.filter { it.name.startsWith("set") }
            assertEquals(itemClass.simpleName, emptyList<String>(), setters.map { it.name })
        }
    }

    @Test
    fun kindsAreIndependent_undoingOneLeavesOthersUntouched() {
        val sealEditor = seal.editor()
        val textEditor = textSticker.editor()
        val tapeEditor = maskingTape.editor()
        val labelEditor = labelSticker.editor()
        val doodleEditor = doodle.editor()

        sealEditor.editTimes(seal, 0..1)
        textEditor.editTimes(textSticker, 0..1)
        tapeEditor.editTimes(maskingTape, 0..1)
        labelEditor.editTimes(labelSticker, 0..1)
        doodleEditor.editTimes(doodle, 0..1)

        val textBefore = textEditor.items
        val tapeBefore = tapeEditor.items
        val labelBefore = labelEditor.items
        val doodleBefore = doodleEditor.items

        sealEditor.undo()
        sealEditor.undo()

        assertFalse(sealEditor.canUndo)
        assertTrue(sealEditor.canRedo)
        assertEquals(textBefore, textEditor.items)
        assertEquals(tapeBefore, tapeEditor.items)
        assertEquals(labelBefore, labelEditor.items)
        assertEquals(doodleBefore, doodleEditor.items)
        listOf(textEditor, tapeEditor, labelEditor, doodleEditor).forEach { other ->
            assertTrue(other.canUndo)
            assertFalse(other.canRedo)
            assertEquals(0, other.autosaveCount)
        }

        sealEditor.clearHistory()
        assertTrue(textEditor.canUndo)
        assertTrue(tapeEditor.canUndo)
        assertTrue(labelEditor.canUndo)
        assertTrue(doodleEditor.canUndo)
    }

    /**
     * 완료 저장 성공 뒤 기록을 비우는 호환성. 저장된 결과는 그대로 남고, 되돌리기·다시하기로
     * 저장 이전 상태로 돌아갈 수 없어야 한다. 파일에 실제로 쓰는 경로는 이 테스트 범위가 아니다.
     */
    @Test
    fun afterConfirmSave_savedStateStaysAndHistoryCannotReachBeforeIt() {
        forEachKind { kind ->
            val editor = kind.editor()
            editor.edit(kind.newItem(0), selectId = null)
            editor.edit(kind.newItem(1), selectId = null)
            editor.undo()
            val saved = editor.items
            val autosavesBeforeSave = editor.autosaveCount

            editor.clearHistory()

            assertEquals(kind.name, saved, editor.items)
            assertFalse(kind.name, editor.canUndo)
            assertFalse(kind.name, editor.canRedo)
            assertEquals(kind.name, autosavesBeforeSave, editor.autosaveCount)

            editor.undo()
            editor.redo()
            assertEquals(kind.name, saved, editor.items)
            assertEquals(kind.name, autosavesBeforeSave, editor.autosaveCount)
        }
    }

    @Test
    fun afterConfirmSave_newEditsStartAFreshHistory() {
        forEachKind { kind ->
            val editor = kind.editor()
            editor.edit(kind.newItem(0), selectId = null)
            editor.clearHistory()
            val saved = editor.items

            editor.edit(kind.newItem(1), selectId = null)
            editor.undo()

            assertEquals(kind.name, saved, editor.items)
            assertFalse(kind.name, editor.canUndo)
        }
    }

    /** 옛 코드의 undo.flatMap + redo.flatMap과 같은 순서여야 사진 참조 판정의 distinctBy 결과도 같다. */
    @Test
    fun maskingTapeRetainedSnapshots_keepUndoThenRedoOrder() {
        val history = DecorationEditHistory<List<MaskingTapeItem>>(MASKING_TAPE_HISTORY_LIMIT)
        val first = listOf(maskingTape.newItem(1))
        val second = listOf(maskingTape.newItem(2))
        val current = listOf(maskingTape.newItem(3))
        history.record(first)
        history.record(second)
        assertEquals(second, history.undo(current))

        val reachableIds = history.retainedSnapshots().flatten().map { it.id }

        assertEquals(listOf("tape1", "tape3"), reachableIds)
    }
}
