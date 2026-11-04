package com.postcardmemory.ui.detail

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DetailViewModel의 꾸미기 5종이 [DecorationEditHistory]에 같은 순서로 연결돼 있는지 소스 텍스트로
 * 확인한다. ViewModel을 JVM에서 만들 수 없어서 쓰는 구조 테스트다. 실제 터치·버튼 동작을 검증하지
 * 않으며, 기록 규칙 자체는 DecorationEditHistoryTest, 편집 순서는 DecorationKindHistoryBehaviorTest가
 * 실행해서 확인한다.
 */
class DecorationEditHistoryWiringStructureTest {

    private class Wiring(
        val functionName: String,
        val historyName: String,
        val limitConstant: String,
        val stateAssignments: List<String>
    )

    private val wirings = listOf(
        Wiring(
            "Seal",
            "sealHistory",
            "SEAL_HISTORY_LIMIT",
            listOf("_photoSeals.value = snapshot.seals", "_selectedSealId.value = snapshot.selectedSealId")
        ),
        Wiring(
            "TextSticker",
            "textStickerHistory",
            "TEXT_STICKER_HISTORY_LIMIT",
            listOf(
                "_textStickers.value = snapshot.textStickers",
                "_selectedTextStickerId.value = snapshot.selectedTextStickerId"
            )
        ),
        Wiring(
            "MaskingTape",
            "maskingTapeHistory",
            "MASKING_TAPE_HISTORY_LIMIT",
            listOf(
                "_photoMaskingTapes.value = snapshot.maskingTapes",
                "_selectedMaskingTapeId.value = snapshot.selectedMaskingTapeId"
            )
        ),
        Wiring(
            "LabelSticker",
            "labelStickerHistory",
            "LABEL_STICKER_HISTORY_LIMIT",
            listOf(
                "_labelStickers.value = snapshot.labelStickers",
                "_selectedLabelStickerId.value = snapshot.selectedLabelStickerId"
            )
        ),
        Wiring(
            "Doodle",
            "doodleHistory",
            "DOODLE_HISTORY_LIMIT",
            listOf("_doodleStrokes.value = snapshot.strokes")
        )
    )

    private val source: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt"
            )
        )
    }

    /** 선언부터 같은 들여쓰기의 닫는 중괄호까지 자른다. 줄바꿈 형식에 흔들리지 않게 LF로 맞춘다. */
    private fun functionBody(declaration: String): String {
        val normalized = source.replace("\r\n", "\n")
        val start = normalized.indexOf(declaration)
        assertTrue("[$declaration] 선언을 찾지 못했다", start >= 0)
        val end = normalized.indexOf("\n    }\n", start)
        assertTrue("[$declaration] 끝을 찾지 못했다", end >= 0)
        return normalized.substring(start, end)
    }

    private fun assertInOrder(body: String, vararg tokens: String) {
        var cursor = 0
        tokens.forEach { token ->
            val index = body.indexOf(token, cursor)
            assertTrue("[$token]이 기대한 순서에 없다:\n$body", index >= 0)
            cursor = index + token.length
        }
    }

    @Test
    fun fiveKinds_noLongerHaveTheirOwnStacks_andUseSharedHistoryWithOwnLimit() {
        wirings.forEach { wiring ->
            val prefix = wiring.historyName.removeSuffix("History")
            assertFalse(wiring.functionName, source.contains("${prefix}UndoStack"))
            assertFalse(wiring.functionName, source.contains("${prefix}RedoStack"))
            assertTrue(
                wiring.functionName,
                Regex(
                    """private val ${wiring.historyName}\s*=\s*DecorationEditHistory<${wiring.functionName}Snapshot>\(\s*${wiring.limitConstant}\s*\)"""
                ).containsMatchIn(source)
            )
        }
    }

    /** 사진 스티커와 사진 위치·크기 기록은 이번 통합 범위가 아니다. */
    @Test
    fun photoStickerAndPhotoTransformHistories_keepTheirOwnStacks() {
        listOf(
            "private val stickerUndoStack",
            "private val stickerRedoStack",
            "private val photoTransformUndoStack",
            "private val photoTransformRedoStack"
        ).forEach { declaration ->
            assertTrue(declaration, source.contains(declaration))
        }
        assertFalse(source.contains("DecorationEditHistory<StickerSnapshot>"))
        assertFalse(source.contains("DecorationEditHistory<PhotoTransformSnapshot>"))
    }

    @Test
    fun publicFunctionNamesAndFlows_areKept() {
        wirings.forEach { wiring ->
            val name = wiring.functionName
            listOf(
                "fun record${name}SnapshotForUndo() {",
                "fun undo${name}Change() {",
                "fun redo${name}Change() {",
                "val canUndo$name: StateFlow<Boolean>",
                "val canRedo$name: StateFlow<Boolean>",
                "private fun clear${name}History() {",
                "private fun update${name}HistoryAvailability() {"
            ).forEach { declaration ->
                assertTrue(declaration, source.contains(declaration))
            }
        }
    }

    @Test
    fun snapshotApply_assignsEveryStateField() {
        wirings.forEach { wiring ->
            val body = functionBody("private fun apply${wiring.functionName}Snapshot(")
            assertInOrder(body, *wiring.stateAssignments.toTypedArray())
        }
    }

    @Test
    fun undoAndRedo_restoreThenUpdateButtonsThenScheduleAutosave() {
        wirings.forEach { wiring ->
            val name = wiring.functionName
            listOf("undo", "redo").forEach { action ->
                assertInOrder(
                    functionBody("fun $action${name}Change() {"),
                    "${wiring.historyName}.$action(current${name}Snapshot()) ?: return",
                    "apply${name}Snapshot(",
                    "update${name}HistoryAvailability()",
                    "scheduleDraftAutosave()"
                )
            }
        }
    }

    @Test
    fun record_writesCurrentStateThenUpdatesButtons_withoutAutosave() {
        wirings.forEach { wiring ->
            val name = wiring.functionName
            val body = functionBody("fun record${name}SnapshotForUndo() {")
            assertInOrder(
                body,
                "${wiring.historyName}.record(current${name}Snapshot())",
                "update${name}HistoryAvailability()"
            )
            assertFalse(name, body.contains("scheduleDraftAutosave"))
        }
    }

    @Test
    fun clear_emptiesHistoryThenUpdatesButtons_withoutAutosave() {
        wirings.forEach { wiring ->
            val name = wiring.functionName
            val body = functionBody("private fun clear${name}History() {")
            assertInOrder(body, "${wiring.historyName}.clear()", "update${name}HistoryAvailability()")
            assertFalse(name, body.contains("scheduleDraftAutosave"))
        }
    }

    @Test
    fun buttonState_readsFromSharedHistory() {
        wirings.forEach { wiring ->
            val name = wiring.functionName
            val body = functionBody("private fun update${name}HistoryAvailability() {")
            assertInOrder(
                body,
                "_canUndo$name.value = ${wiring.historyName}.canUndo",
                "_canRedo$name.value = ${wiring.historyName}.canRedo"
            )
        }
    }

    /** 마스킹테이프만 사진 파일을 정리한다. 정리 호출 자리가 바뀌면 삭제 시점이 달라진다. */
    @Test
    fun maskingTapeOnly_sweepsPhotoCandidatesAtTheSamePoints() {
        val sweep = "sweepMaskingTapePhotoCleanupCandidates()"

        assertInOrder(
            functionBody("fun undoMaskingTapeChange() {"),
            "updateMaskingTapeHistoryAvailability()",
            sweep,
            "scheduleDraftAutosave()"
        )
        assertInOrder(
            functionBody("fun redoMaskingTapeChange() {"),
            "updateMaskingTapeHistoryAvailability()",
            sweep,
            "scheduleDraftAutosave()"
        )
        assertInOrder(
            functionBody("private fun clearMaskingTapeHistory() {"),
            "maskingTapeHistory.clear()",
            "updateMaskingTapeHistoryAvailability()",
            sweep
        )

        val record = functionBody("fun recordMaskingTapeSnapshotForUndo() {")
        assertEquals(1, Regex(Regex.escape(sweep)).findAll(record).count())
        assertTrue(
            "밀려났을 때만 정리해야 한다:\n$record",
            Regex(
                """if \(maskingTapeHistory\.record\(currentMaskingTapeSnapshot\(\)\)\) \{\s*""" +
                    Regex.escape(sweep) + """\s*\}\s*updateMaskingTapeHistoryAvailability\(\)"""
            ).containsMatchIn(record)
        )

        wirings.filter { it.functionName != "MaskingTape" }.forEach { wiring ->
            val name = wiring.functionName
            listOf(
                "fun record${name}SnapshotForUndo() {",
                "fun undo${name}Change() {",
                "fun redo${name}Change() {",
                "private fun clear${name}History() {"
            ).forEach { declaration ->
                assertFalse(declaration, functionBody(declaration).contains("sweep"))
            }
        }
    }

    @Test
    fun maskingTapePhotoReferenceChecks_includeEveryRetainedSnapshot() {
        val check = functionBody("private fun isMaskingTapePhotoStillReferenced(")
        assertTrue(check.contains("maskingTapeHistory.retainedSnapshots().flatMap { it.maskingTapes }"))

        val delete = functionBody("fun deleteMaskingTapePhotoIfUnreferenced(")
        assertTrue(delete.contains("maskingTapeHistory.retainedSnapshots().flatMap { it.maskingTapes }"))
    }
}
