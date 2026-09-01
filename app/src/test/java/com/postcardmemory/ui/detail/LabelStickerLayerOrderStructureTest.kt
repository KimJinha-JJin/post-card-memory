package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 라벨은 "이미 꾸며놓은 종이 위에 나중에 붙이는 물리적인 스티커"라 낙서보다
 * 위에 그려져야 하고, 미리보기와 저장 이미지가 같은 순서를 가져야 한다.
 * 이 프로젝트는 Compose UI 테스트 인프라를 쓰지 않으므로
 * (StickerPositionCalculationsStructureTest 상단 주석 참고) 두 렌더 경로의
 * 소스 텍스트에서 호출 순서를 고정한다.
 *
 * 한쪽만 고쳐서 "화면에서는 라벨이 위인데 공유 이미지에서는 낙서가 위"인
 * 상태가 되는 것을 막는 것이 이 테스트의 목적이다.
 */
class LabelStickerLayerOrderStructureTest {

    private fun readSource(candidates: List<String>): String {
        val file = candidates
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?: error(
                "소스 파일을 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                    "candidates=$candidates"
            )
        return file.readText()
    }

    private val detailScreenText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
            )
        )
    }

    private val exporterText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/utils/PostcardImageExporter.kt",
                "app/src/main/java/com/postcardmemory/utils/PostcardImageExporter.kt"
            )
        )
    }

    @Test
    fun previewAndExporterEachHaveExactlyOneDoodleAndLabelRenderSite() {
        // 아래 순서 검증이 indexOf 하나에 기대므로, 호출부가 여러 곳으로
        // 늘어나면 검증이 무의미해진다는 사실을 먼저 고정한다.
        assertEquals(
            1,
            Regex("""drawDoodleStrokes\(""").findAll(detailScreenText).count()
        )
        assertEquals(
            1,
            Regex("""labelStickers\.forEach \{""").findAll(detailScreenText).count()
        )
        assertEquals(
            1,
            Regex("""drawDoodleStrokes\(""").findAll(exporterText).count()
        )
        assertEquals(
            1,
            Regex("""for \(overlay in labelStickerOverlays\)""")
                .findAll(exporterText).count()
        )
    }

    @Test
    fun previewDrawsLabelStickersAfterDoodles() {
        val doodleIndex = detailScreenText.indexOf("drawDoodleStrokes(")
        val labelIndex = detailScreenText.indexOf("labelStickers.forEach {")

        assertTrue("낙서 렌더 호출을 찾지 못했다", doodleIndex >= 0)
        assertTrue("라벨 렌더 루프를 찾지 못했다", labelIndex >= 0)
        assertTrue(
            "미리보기에서 라벨이 낙서보다 먼저 그려지면 낙서가 라벨을 덮는다",
            doodleIndex < labelIndex
        )
    }

    @Test
    fun exporterDrawsLabelStickersAfterDoodles() {
        val doodleIndex = exporterText.indexOf("drawDoodleStrokes(")
        val labelIndex = exporterText.indexOf("for (overlay in labelStickerOverlays)")

        assertTrue("낙서 렌더 호출을 찾지 못했다", doodleIndex >= 0)
        assertTrue("라벨 렌더 루프를 찾지 못했다", labelIndex >= 0)
        assertTrue(
            "공유/저장 이미지에서 라벨이 낙서보다 먼저 그려지면 편집 화면과 결과가 어긋난다",
            doodleIndex < labelIndex
        )
    }

    /**
     * 라벨만 낙서 위로 올린 변경이라, 텍스트 스티커·도장 등 나머지 요소와
     * 낙서의 관계는 그대로여야 한다(작업지시서 12절 D — 다른 요소 순서까지
     * 동시에 바뀌면 안 됨).
     */
    @Test
    fun exporterKeepsOtherOverlaysBelowDoodles() {
        val doodleIndex = exporterText.indexOf("drawDoodleStrokes(")

        listOf(
            "for (overlay in maskingTapeOverlays)",
            "for (overlay in stickerOverlays)",
            "for (overlay in sealOverlays)",
            "for (overlay in textStickerOverlays)"
        ).forEach { marker ->
            val index = exporterText.indexOf(marker)
            assertTrue("[$marker] 렌더 루프를 찾지 못했다", index >= 0)
            assertTrue(
                "[$marker] 는 낙서보다 아래에 그려지는 관계를 유지해야 한다",
                index < doodleIndex
            )
        }
    }
}
