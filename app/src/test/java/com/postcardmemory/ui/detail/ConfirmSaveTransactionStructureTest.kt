package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 121일차: 완료 저장이 여섯 종과 확정 누끼를 [com.postcardmemory.utils.ConfirmedEditTransaction]으로
 * "모두 준비 → 한 번에 교체"하도록 연결된 구조를 고정한다.
 *
 * DetailViewModel은 순수 JUnit에서 만들 수 없어(ConfirmSaveHistoryClearStructureTest 참고) 소스
 * 텍스트로 연결과 순서를 확인한다. 준비 실패 시 확정 파일이 그대로인지, 교체 결과가 어떻게 나뉘는지는
 * ConfirmedEditTransactionTest가 실제 파일로 검증한다. 실제 화면 경로는 실기기 QA 몫이다.
 */
class ConfirmSaveTransactionStructureTest {

    private val source: String by lazy {
        listOf(
            "src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt",
            "app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt"
        )
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("DetailViewModel.kt를 찾을 수 없음(cwd=${File(".").absolutePath})")
    }

    private fun functionBody(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("$signature 를 찾지 못했다", start >= 0)
        // 시그니처의 반환 타입 등에 '{'가 없으므로 첫 '{'가 본문 시작이다.
        val open = source.indexOf('{', source.indexOf(')', start))
        var depth = 0
        for (index in open until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(open, index + 1)
                }
            }
        }
        error("$signature 의 끝을 찾지 못했다")
    }

    private fun assertOrdered(body: String, vararg markers: String) {
        var previous = -1
        markers.forEach { marker ->
            val index = body.indexOf(marker, previous + 1)
            assertTrue("[$marker]가 앞 단계 뒤에 없다", index > previous)
            previous = index
        }
    }

    private val textKinds =
        listOf(
            "persistSealEditState",
            "persistDoodleEditState",
            "persistTextStickerEditState",
            "persistMaskingTapeEditState",
            "persistLabelStickerEditState"
        )

    @Test
    fun confirmSave_stagesAllSixKinds_thenCommitsOnce_thenDeletesDraft() {
        val body = functionBody("fun saveEditsAndClearDraft(")
        assertOrdered(
            body,
            "val transaction = ConfirmedEditTransaction()",
            "persistStickerEditState(postcardId, transaction)",
            "persistSealEditState(postcardId, transaction)",
            "persistDoodleEditState(postcardId, transaction)",
            "persistTextStickerEditState(postcardId, transaction)",
            "persistMaskingTapeEditState(postcardId, transaction)",
            "persistLabelStickerEditState(postcardId, transaction)",
            "shouldConfirmSaveSucceed(",
            "transaction.commit()",
            "transaction.discard()",
            "commitResult == ConfirmedEditTransaction.CommitResult.COMMITTED",
            "withContext(NonCancellable)",
            "PostcardDraftStorage.deleteDraft(context, postcardId)"
        )
        assertEquals(
            "교체는 한 번만 한다",
            1,
            Regex(Regex.escape("transaction.commit()")).findAll(body).count()
        )
    }

    @Test
    fun confirmSave_stagingException_discardsAndCountsAsFailure() {
        val body = functionBody("fun saveEditsAndClearDraft(")
        assertOrdered(
            body,
            "} catch (exception: CancellationException) {",
            "transaction.discard()",
            "throw exception",
            "} catch (exception: Exception) {",
            "false"
        )
    }

    @Test
    fun confirmSave_screenAndCacheChangeOnlyAfterSuccessfulCommit() {
        val body = functionBody("fun saveEditsAndClearDraft(")
        assertOrdered(
            body,
            "transaction.commit()",
            "withContext(Dispatchers.Main) {",
            "if (allSaved) {",
            "_photoStickers.value = committedStickers",
            "confirmedStickersBaseline = _photoStickers.value",
            "stagedStickers?.releasedCacheSources.orEmpty()",
            "clearStickerHistory()"
        )
    }

    @Test
    fun confirmedStateFiles_areNeverWrittenOutsideTheTransaction() {
        assertFalse(
            "확정 상태 파일을 종류마다 곧바로 교체하는 예전 방식이 남아 있다",
            source.contains("writeTextAtomically")
        )
        textKinds.forEach { kind ->
            val body = functionBody("private fun $kind(")
            assertTrue("$kind 가 트랜잭션에 준비하지 않는다", body.contains("transaction.stageText("))
        }
        val stickerBody = functionBody("private fun persistStickerEditState(")
        assertTrue(stickerBody.contains("transaction.stageText("))
        assertFalse(
            "스티커 준비 단계에서 화면 상태를 바꾸면 실패 시 화면과 확정본이 어긋난다",
            stickerBody.contains("_photoStickers.value =")
        )
    }

    @Test
    fun stickerBackground_isStagedForConfirmSave_andNeverDeletesItsSource() {
        val body = functionBody("private fun persistStickerBackground(")
        assertTrue(body.contains("transaction.stageCopy(srcFile, destFile)"))
        assertFalse(
            "누끼 준비 중에 원본(캐시)을 지우면 뒤이은 실패 때 화면·undo가 지워진 파일을 가리킨다",
            body.contains(".delete()")
        )
    }
}
