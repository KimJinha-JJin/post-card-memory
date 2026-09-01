package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 60일차 상세 설정창 정돈: 결과 확인에 필요한 Preview는 유지하되,
 * Preview 뒤에 습관적으로 놓였던 PaperField 둥근 배경은 다시 만들지 않는다.
 * 도장의 저장된 흰 잉크처럼 실제 대비를 보장하는 배경은 기능적 예외로 남긴다.
 */
class DialogPreviewFlatContainerStructureTest {

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            "src/main/java/com/postcardmemory/ui/detail/$relativePath",
            "app/src/main/java/com/postcardmemory/ui/detail/$relativePath"
        )
        val file = candidates
            .map(::File)
            .firstOrNull(File::exists)
            ?: error(
                "소스 파일을 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                    "candidates=$candidates"
            )
        return file.readText()
    }

    private val maskingTapeSource by lazy {
        readSource("MaskingTapeDetailScreen.kt")
    }

    private val labelStickerSource by lazy {
        readSource("LabelStickerDetailScreen.kt")
    }

    private val sealSource by lazy {
        readSource("PostcardSealDetailScreen.kt")
    }

    private fun previewPrefix(
        source: String,
        functionName: String,
        previewCall: String
    ): String {
        val functionStart = source.indexOf("private fun $functionName(")
        assertTrue("$functionName 선언을 찾지 못함", functionStart >= 0)

        val previewStart = source.indexOf("$previewCall(", functionStart)
        assertTrue(
            "$functionName 안에서 $previewCall 호출을 찾지 못함",
            previewStart > functionStart
        )

        return source.substring((previewStart - 420).coerceAtLeast(functionStart), previewStart)
    }

    @Test
    fun maskingTapePresetAndEditPreviews_keepContentWithoutDecorativeBackground() {
        listOf(
            "MaskingTapePresetCreateDialog",
            "MaskingTapeEditDialog"
        ).forEach { functionName ->
            val prefix = previewPrefix(
                source = maskingTapeSource,
                functionName = functionName,
                previewCall = "MaskingTapeContent"
            )

            assertFalse(
                "$functionName Preview 뒤에 장식성 background를 다시 두면 안 됨",
                prefix.contains(".background(")
            )
            assertTrue(
                "$functionName Preview의 기존 여백은 유지해야 함",
                prefix.contains(".padding(vertical = 20.dp)")
            )
        }
    }

    @Test
    fun labelCreateAndEditPreviews_keepContentWithoutDecorativeBackground() {
        listOf(
            "LabelStickerCreateDialog",
            "LabelStickerEditDialog"
        ).forEach { functionName ->
            val prefix = previewPrefix(
                source = labelStickerSource,
                functionName = functionName,
                previewCall = "LabelStickerContent"
            )

            assertFalse(
                "$functionName Preview 뒤에 장식성 background를 다시 두면 안 됨",
                prefix.contains(".background(")
            )
            assertTrue(
                "$functionName Preview의 긴 문구 가로 스크롤은 유지해야 함",
                prefix.contains(".horizontalScroll(rememberScrollState())")
            )
        }
    }

    @Test
    fun sealPreview_keepsFunctionalContrastBackgroundForLegacyWhiteInk() {
        val prefix = previewPrefix(
            source = sealSource,
            functionName = "SealDesignDialog",
            previewCall = "SealPreviewContent"
        )

        assertTrue(
            "저장된 흰 잉크 도장을 볼 수 있게 조건부 대비 배경을 유지해야 함",
            prefix.contains(
                "color = if (isDraftWhiteInk) NeutralLight else Color.Transparent"
            )
        )
        assertFalse(
            "일반 잉크 Preview 뒤에 PaperField 장식 배경을 두면 안 됨",
            prefix.contains("else PaperField")
        )
    }
}
