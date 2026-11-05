package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.postcardmemory.utils.PostcardImageExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 공유와 갤러리 저장이 함께 쓰는 앞면 오버레이 조립의 결과를 고정한다.
 * 기대값은 123일차 리팩터링 전 개별 조립 함수를 직접 호출해 먼저 통과시킨 값이다.
 *
 * 이 테스트는 "그리기 직전 입력값"까지만 검증한다. 실제 픽셀은 JVM에서 만들 수 없고
 * (Robolectric 없음), 사진 스티커는 android.net.Uri 인스턴스를 만들 수 없어 여기서 다루지
 * 않는다. 사진 스티커 연결은 PostcardFrontExportRoutingStructureTest가 소스 수준으로 고정한다.
 */
class PostcardFrontOverlaysAssemblyTest {

    private val postcardSize = IntSize(1000, 1000)

    private fun assemble(
        seals: List<PostcardSealItem> = emptyList(),
        sealSizes: Map<String, IntSize> = emptyMap(),
        textStickers: List<TextStickerItem> = emptyList(),
        textStickerSizes: Map<String, IntSize> = emptyMap(),
        maskingTapes: List<MaskingTapeItem> = emptyList(),
        maskingTapeSizes: Map<String, IntSize> = emptyMap(),
        labelStickers: List<LabelStickerItem> = emptyList(),
        labelStickerSizes: Map<String, IntSize> = emptyMap(),
        size: IntSize = postcardSize,
        capturedAtMillis: Long? = 1234L
    ): PostcardImageExporter.FrontOverlays =
        createFrontOverlaysForExport(
            postcardSize = size,
            photoStickers = emptyList(),
            stickerSizes = emptyMap(),
            baseStickerPx = 300f,
            photoSeals = seals,
            sealSizes = sealSizes,
            baseSealPx = 200f,
            sealMinVisibleEdgePx = 24f,
            capturedAtMillis = capturedAtMillis,
            textStickers = textStickers,
            textStickerSizes = textStickerSizes,
            baseTextStickerFontPx = 40f,
            maskingTapes = maskingTapes,
            maskingTapeSizes = maskingTapeSizes,
            baseMaskingTapeWidthPx = 100f,
            baseMaskingTapeHeightPx = 20f,
            labelStickers = labelStickers,
            labelStickerSizes = labelStickerSizes,
            baseLabelStickerFontPx = 40f
        )

    // 시나리오 1: 꾸미기 없는 기본 엽서

    @Test
    fun noDecorations_allListsEmpty() {
        val result = assemble()

        assertTrue(result.stickers.isEmpty())
        assertTrue(result.seals.isEmpty())
        assertTrue(result.textStickers.isEmpty())
        assertTrue(result.maskingTapes.isEmpty())
        assertTrue(result.labelStickers.isEmpty())
    }

    // 시나리오 2·4: 단일 요소, 회전·크기 변경

    @Test
    fun rotatedAndScaledSeal_keepsPositionSizeRotationInkAndCapturedAt() {
        val seal = PostcardSealItem(
            id = "seal-a",
            type = SealType.CIRCLE_POSTMARK,
            offset = Offset(100f, 200f),
            scale = 1.5f,
            rotationDegrees = 30f,
            colorArgb = 0xFF112233L
        )

        val result = assemble(
            seals = listOf(seal),
            sealSizes = mapOf("seal-a" to IntSize(100, 100))
        )

        assertEquals(
            listOf(
                PostcardImageExporter.SealOverlay(
                    type = "CIRCLE_POSTMARK",
                    normalizedX = 0.1f,
                    normalizedY = 0.2f,
                    sizeRatio = 0.1f,
                    rotationDegrees = 30f,
                    colorArgb = 0xFF112233L,
                    capturedAtMillis = 1234L,
                    inkSeed = sealInkSeed("seal-a")
                )
            ),
            result.seals
        )
    }

    // 시나리오 6: 텍스트(글꼴 크기는 비율로 전달)

    @Test
    fun rotatedAndScaledText_keepsFontRatioColorsAndRotation() {
        val text = TextStickerItem(
            id = "text-a",
            text = "여름",
            offset = Offset(250f, 500f),
            scale = 1.5f,
            rotationDegrees = -15f,
            colorArgb = 0xFF445566L,
            outlineColorArgb = 0xFF778899L
        )

        val result = assemble(
            textStickers = listOf(text),
            textStickerSizes = mapOf("text-a" to IntSize(200, 100))
        )

        assertEquals(
            listOf(
                PostcardImageExporter.TextStickerOverlay(
                    text = "여름",
                    normalizedX = 0.25f,
                    normalizedY = 0.5f,
                    fontSizeRatio = 0.06f,
                    rotationDegrees = -15f,
                    colorArgb = 0xFF445566L,
                    outlineColorArgb = 0xFF778899L
                )
            ),
            result.textStickers
        )
    }

    // 시나리오 5·8: 마스킹테이프(투명도·회전)

    @Test
    fun maskingTape_keepsAlphaRotationEdgeAndCustomColors() {
        val tape = MaskingTapeItem(
            id = "tape-a",
            style = MaskingTapeStyle.CUSTOM,
            offset = Offset(250f, 500f),
            rotationDegrees = 45f,
            edgeStyle = MaskingTapeEdgeStyle.NOTCHED_BOTH,
            customBaseColorArgb = 0xFF112233L,
            customPatternColorArgb = 0xFF445566L,
            customPatternKind = MaskingTapePatternKind.STAR
        )

        val result = assemble(
            maskingTapes = listOf(tape),
            maskingTapeSizes = mapOf("tape-a" to IntSize(80, 30))
        )

        assertEquals(
            listOf(
                PostcardImageExporter.MaskingTapeOverlay(
                    normalizedX = 0.25f,
                    normalizedY = 0.5f,
                    widthRatio = 0.08f,
                    heightRatio = 0.03f,
                    rotationDegrees = 45f,
                    edgeStyle = MaskingTapeEdgeStyle.NOTCHED_BOTH,
                    baseColorArgb = 0xFF112233L,
                    patternColorArgb = 0xFF445566L,
                    patternKind = MaskingTapePatternKind.STAR,
                    alpha = MaskingTapeStyle.CUSTOM.alpha,
                    isPhoto = false,
                    photoUri = null
                )
            ),
            result.maskingTapes
        )
    }

    @Test
    fun labelSticker_keepsStyleFontRatioRotationAndCustomColor() {
        val label = LabelStickerItem(
            id = "label-a",
            text = "우리집",
            style = LabelTapeStyle.BLACK,
            offset = Offset(100f, 100f),
            rotationDegrees = 5f,
            customTapeColorArgb = 0xFFABCDEFL
        )

        val result = assemble(
            labelStickers = listOf(label),
            labelStickerSizes = mapOf("label-a" to IntSize(300, 60))
        )

        assertEquals(
            listOf(
                PostcardImageExporter.LabelStickerOverlay(
                    text = "우리집",
                    style = LabelTapeStyle.BLACK,
                    normalizedX = 0.1f,
                    normalizedY = 0.1f,
                    fontSizeRatio = 0.04f,
                    rotationDegrees = 5f,
                    customTapeColorArgb = 0xFFABCDEFL
                )
            ),
            result.labelStickers
        )
    }

    // 시나리오 3: 겹친 여러 요소의 순서

    @Test
    fun overlappingItems_keepEachKindsListOrder() {
        val samePosition = Offset(300f, 300f)
        val texts = listOf("맨아래", "가운데", "맨위").map { name ->
            TextStickerItem(id = name, text = name, offset = samePosition)
        }
        val labels = listOf("첫째", "둘째").map { name ->
            LabelStickerItem(id = name, text = name, offset = samePosition)
        }

        val result = assemble(
            textStickers = texts,
            textStickerSizes = texts.associate { it.id to IntSize(100, 50) },
            labelStickers = labels,
            labelStickerSizes = labels.associate { it.id to IntSize(100, 50) }
        )

        assertEquals(listOf("맨아래", "가운데", "맨위"), result.textStickers.map { it.text })
        assertEquals(listOf("첫째", "둘째"), result.labelStickers.map { it.text })
    }

    // 시나리오 9: 가장자리 배치(도장만 걸침 허용, 나머지는 안쪽으로)

    @Test
    fun edgePlacement_sealKeepsOverhang_othersClampInside() {
        val seal = PostcardSealItem(
            id = "seal-edge",
            type = SealType.CIRCLE_POSTMARK,
            offset = Offset(950f, 450f)
        )
        val text = TextStickerItem(id = "text-edge", text = "끝", offset = Offset(950f, 950f))
        val tape = MaskingTapeItem(
            id = "tape-edge",
            style = MaskingTapeStyle.MINT_CHECK,
            offset = Offset(990f, 990f)
        )
        val label = LabelStickerItem(id = "label-edge", text = "끝", offset = Offset(950f, 950f))

        val result = assemble(
            seals = listOf(seal),
            sealSizes = mapOf("seal-edge" to IntSize(100, 100)),
            textStickers = listOf(text),
            textStickerSizes = mapOf("text-edge" to IntSize(200, 100)),
            maskingTapes = listOf(tape),
            maskingTapeSizes = mapOf("tape-edge" to IntSize(80, 30)),
            labelStickers = listOf(label),
            labelStickerSizes = mapOf("label-edge" to IntSize(200, 100))
        )

        assertEquals(0.95f, result.seals.single().normalizedX)
        assertEquals(0.8f, result.textStickers.single().normalizedX)
        assertEquals(0.9f, result.textStickers.single().normalizedY)
        assertEquals(0.92f, result.maskingTapes.single().normalizedX)
        assertEquals(0.97f, result.maskingTapes.single().normalizedY)
        assertEquals(0.8f, result.labelStickers.single().normalizedX)
        assertEquals(0.9f, result.labelStickers.single().normalizedY)
    }

    // 측정값이 없을 때: 도장·테이프는 대체 크기, 글·라벨은 건너뜀(종류별로 의도적으로 다름)

    @Test
    fun missingMeasurements_sealAndTapeUseFallback_textAndLabelAreDropped() {
        val result = assemble(
            seals = listOf(
                PostcardSealItem(
                    id = "seal",
                    type = SealType.CIRCLE_POSTMARK,
                    offset = Offset(100f, 100f),
                    scale = 0.5f
                )
            ),
            maskingTapes = listOf(
                MaskingTapeItem(
                    id = "tape",
                    style = MaskingTapeStyle.MINT_CHECK,
                    offset = Offset(100f, 100f),
                    scale = 2f,
                    lengthScale = 1.5f,
                    thicknessScale = 0.5f
                )
            ),
            textStickers = listOf(TextStickerItem(id = "text", text = "글")),
            labelStickers = listOf(LabelStickerItem(id = "label", text = "라벨"))
        )

        assertEquals(0.1f, result.seals.single().sizeRatio)
        assertEquals(0.3f, result.maskingTapes.single().widthRatio)
        assertEquals(0.02f, result.maskingTapes.single().heightRatio)
        assertTrue(result.textStickers.isEmpty())
        assertTrue(result.labelStickers.isEmpty())
    }

    @Test
    fun sealFarOutside_isCorrectedWithTheGivenMinimumVisibleEdge() {
        // 도장이 작아야 비율 하한보다 최소 노출 값(24px)이 결과를 결정한다.
        val offset = Offset(2000f, 450f)
        val size = IntSize(40, 40)

        val result = assemble(
            seals = listOf(
                PostcardSealItem(id = "seal", type = SealType.CIRCLE_POSTMARK, offset = offset)
            ),
            sealSizes = mapOf("seal" to size)
        )

        val expected = correctSealOffsetForMinimumVisibility(
            offset = offset,
            sealSize = size,
            rotationDegrees = 0f,
            postcardSize = postcardSize,
            minimumVisibleEdgePx = 24f
        )
        val withoutMinimum = correctSealOffsetForMinimumVisibility(
            offset = offset,
            sealSize = size,
            rotationDegrees = 0f,
            postcardSize = postcardSize,
            minimumVisibleEdgePx = 0f
        )
        assertTrue("입력값이 최소 노출 값을 구분하지 못한다", expected != withoutMinimum)
        assertEquals(expected.x / 1000f, result.seals.single().normalizedX)
    }

    @Test
    fun missingOffset_centersOnPostcard() {
        val result = assemble(
            textStickers = listOf(TextStickerItem(id = "text", text = "글", offset = null)),
            textStickerSizes = mapOf("text" to IntSize(200, 100))
        )

        assertEquals(0.4f, result.textStickers.single().normalizedX)
        assertEquals(0.45f, result.textStickers.single().normalizedY)
    }

    @Test
    fun unmeasuredPostcard_producesNoOverlays() {
        val result = assemble(
            seals = listOf(PostcardSealItem(id = "seal", type = SealType.CIRCLE_POSTMARK)),
            maskingTapes = listOf(
                MaskingTapeItem(id = "tape", style = MaskingTapeStyle.MINT_CHECK)
            ),
            size = IntSize.Zero
        )

        assertTrue(result.seals.isEmpty())
        assertTrue(result.maskingTapes.isEmpty())
    }

    /** 글과 라벨의 기준 글자 크기가 서로 바뀌어 전달되면 실패한다. */
    @Test
    fun textAndLabel_useTheirOwnBaseFontSize() {
        val overlays = createFrontOverlaysForExport(
            postcardSize = postcardSize,
            photoStickers = emptyList(),
            stickerSizes = emptyMap(),
            baseStickerPx = 300f,
            photoSeals = emptyList(),
            sealSizes = emptyMap(),
            baseSealPx = 200f,
            sealMinVisibleEdgePx = 24f,
            capturedAtMillis = null,
            textStickers = listOf(TextStickerItem(id = "t", text = "글", offset = Offset.Zero)),
            textStickerSizes = mapOf("t" to IntSize(100, 50)),
            baseTextStickerFontPx = 40f,
            maskingTapes = emptyList(),
            maskingTapeSizes = emptyMap(),
            baseMaskingTapeWidthPx = 100f,
            baseMaskingTapeHeightPx = 20f,
            labelStickers = listOf(LabelStickerItem(id = "l", text = "라벨", offset = Offset.Zero)),
            labelStickerSizes = mapOf("l" to IntSize(100, 50)),
            baseLabelStickerFontPx = 60f
        )

        assertEquals(0.04f, overlays.textStickers.single().fontSizeRatio)
        assertEquals(0.06f, overlays.labelStickers.single().fontSizeRatio)
    }

    @Test
    fun capturedAt_reachesEverySeal() {
        val seals = listOf("a", "b").map { id ->
            PostcardSealItem(id = id, type = SealType.CIRCLE_POSTMARK, offset = Offset(100f, 100f))
        }

        val result = assemble(
            seals = seals,
            sealSizes = seals.associate { it.id to IntSize(100, 100) },
            capturedAtMillis = 777L
        )

        assertEquals(listOf(777L, 777L), result.seals.map { it.capturedAtMillis })
    }
}
