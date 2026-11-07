package com.postcardmemory.ui.detail

import com.postcardmemory.testsupport.extractBalancedCall
import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 공유와 파일 내보내기의 연결을 소스 수준으로 고정한다. Composable과 ViewModel은 JVM에서
 * 실행할 수 없고, 사진 스티커는 Uri 인스턴스를 만들 수 없어 대체 수단이 없다.
 * 실제 픽셀이나 실행 결과를 보장하지는 않는다(StructureTestSource.kt 주석 참고).
 */
class PostcardFrontExportRoutingStructureTest {

    private fun source(path: String): String =
        readStructureTestSource(listOf("src/main/java/$path", "app/src/main/java/$path"))
            .replace("\r\n", "\n")

    private val detailScreen by lazy {
        source("com/postcardmemory/ui/detail/DetailScreen.kt")
    }
    private val viewModel by lazy {
        source("com/postcardmemory/ui/detail/DetailViewModel.kt")
    }
    private val exporter by lazy {
        source("com/postcardmemory/utils/PostcardImageExporter.kt")
    }

    private fun count(text: String, pattern: String): Int =
        Regex(pattern).findAll(text).count()

    /**
     * 상단바 버튼의 onClick·enabled·진행 표시 부분. 125일차에 공유·파일 내보내기를 케밥 메뉴에서
     * 상단바 IconButton으로 옮겨, 그 버튼의 contentDescription 직전 IconButton( 부터 자른다.
     */
    private fun menuHandler(label: String): String {
        val description = detailScreen.indexOf("contentDescription = \"$label\"")
        assertTrue("[$label] 상단바 버튼을 찾지 못했다", description >= 0)
        assertEquals(
            "[$label] 버튼은 한 곳에만 있어야 한다",
            description,
            detailScreen.lastIndexOf("contentDescription = \"$label\"")
        )
        val start = detailScreen.lastIndexOf("IconButton(", description)
        assertTrue("[$label] IconButton 안에 있어야 한다", start >= 0)
        return detailScreen.substring(start, description)
    }

    /** 옮긴 버튼도 메뉴 항목과 같은 조건으로 막히고, 준비 중에는 같은 상태로 진행 표시를 보여 준다. */
    @Test
    fun topBarButtonsKeepTheMenuGuardAndProgressState() {
        mapOf(
            "공유" to "if (shareState is ShareState.Preparing)",
            "파일 내보내기" to "if (exportState is ExportState.Exporting)"
        ).forEach { (label, progress) ->
            val handler = menuHandler(label)
            assertTrue("[$label] controlsEnabled로 막혀야 한다", handler.contains("enabled = controlsEnabled"))
            assertTrue("[$label] 진행 표시가 기존 상태를 읽어야 한다", handler.contains(progress))
            assertTrue("[$label] 진행 표시가 있어야 한다", handler.contains("CircularProgressIndicator("))
            assertTrue("[$label] 진행 표시는 기존 18dp", handler.contains(".size(18.dp)"))
        }
    }

    @Test
    fun eachKindIsAssembledAtExactlyOneSite_insideTheSharedAssembler() {
        val assembler = extractBalancedCall(
            detailScreen,
            detailScreen.indexOf("PostcardImageExporter.FrontOverlays(\n")
        )

        listOf(
            "createStickerOverlaysForExport",
            "createSealOverlaysForExport",
            "createTextStickerOverlaysForExport",
            "createMaskingTapeOverlaysForExport",
            "createLabelStickerOverlaysForExport"
        ).forEach { function ->
            assertEquals(
                "[$function] 호출은 공통 조립 함수 안 한 곳이어야 한다",
                1,
                count(detailScreen, """(?<!fun )\b$function\(""")
            )
            assertTrue("[$function] 호출이 공통 조립 함수 밖에 있다", assembler.contains("$function("))
        }
    }

    /** 사진 스티커는 JVM 행동 테스트로 확인할 수 없어 인자 연결을 여기서 고정한다. */
    @Test
    fun photoStickerArgumentsAreForwardedUnchanged() {
        val call = extractBalancedCall(
            detailScreen,
            detailScreen.indexOf("createStickerOverlaysForExport(\n                photoStickers")
        )

        listOf(
            "photoStickers = photoStickers",
            "postcardSize = postcardSize",
            "stickerSizes = stickerSizes",
            "baseStickerPx = baseStickerPx"
        ).forEach { argument ->
            assertTrue("[$argument] 연결이 달라졌다", call.contains(argument))
        }
    }

    @Test
    fun currentStateLambda_forwardsEveryScreenValueToItsOwnParameter() {
        val call = extractBalancedCall(
            detailScreen,
            detailScreen.indexOf(
                "createFrontOverlaysForExport(\n            postcardSize = postcardPreviewSize"
            )
        )

        listOf(
            "postcardSize = postcardPreviewSize",
            "photoStickers = photoStickers",
            "stickerSizes = stickerSizes",
            "baseStickerPx = baseStickerPx",
            "photoSeals = photoSeals",
            "sealSizes = sealSizes",
            "baseSealPx = baseSealPx",
            "sealMinVisibleEdgePx = sealMinVisibleEdgePx",
            "capturedAtMillis = capturedAtMillis",
            "textStickers = textStickers",
            "textStickerSizes = textStickerSizes",
            "baseTextStickerFontPx = baseTextStickerFontPx",
            "maskingTapes = photoMaskingTapes",
            "maskingTapeSizes = maskingTapeSizes",
            "baseMaskingTapeWidthPx = baseMaskingTapeWidthPx",
            "baseMaskingTapeHeightPx = baseMaskingTapeHeightPx",
            "labelStickers = labelStickers",
            "labelStickerSizes = labelStickerSizes",
            "baseLabelStickerFontPx = baseLabelStickerFontPx"
        ).forEach { argument ->
            assertTrue("[$argument] 연결이 달라졌다", call.contains(argument))
        }
    }

    @Test
    fun bothMenusAssembleThroughTheSameEntry_afterTheSameGuards() {
        mapOf(
            "공유" to "viewModel.sharePostcard(",
            "파일 내보내기" to "viewModel.exportPostcardToGallery("
        ).forEach { (label, call) ->
            val handler = menuHandler(label)
            val backGuard = handler.indexOf("if (isBackFace)")
            val sizeGuard = handler.indexOf("if (postcardPreviewSize == IntSize.Zero)")
            val export = handler.indexOf(call)

            assertTrue("[$label] 뒷면 분기가 앞면 내보내기보다 먼저여야 한다", backGuard in 0 until sizeGuard)
            assertTrue("[$label] 크기 미측정 안내가 앞면 내보내기보다 먼저여야 한다", sizeGuard in 0 until export)
            assertTrue(
                "[$label] 공통 조립 진입점을 거쳐야 한다",
                handler.substring(export).contains("createCurrentFrontOverlaysForExport(pc.capturedAt)")
            )
        }
    }

    /** 뒷면 내보내기는 앞면 조립과 별개의 경로이며 sharing 플래그만 다르다. */
    @Test
    fun menusKeepTheirOwnBackFaceFlag() {
        assertTrue(menuHandler("공유").contains("backExportRequest = pc to true"))
        assertTrue(menuHandler("파일 내보내기").contains("backExportRequest = pc to false"))
    }

    /**
     * 125일차: 상단바 버튼 연타가 완료 창이 그려지기 전 틈에 새 내보내기를 시작하지 않도록,
     * 앞면·뒷면 내보내기 모두 대기 상태에서만 시작한다(공유는 원래 그랬다).
     */
    @Test
    fun exportsStartOnlyFromIdle() {
        fun head(function: String): String {
            val start = viewModel.indexOf("fun $function(")
            assertTrue("[$function] 찾지 못했다", start >= 0)
            return viewModel.substring(start, viewModel.indexOf("viewModelScope.launch", start))
        }

        assertTrue(head("exportPostcardToGallery").contains("if (_exportState.value !is ExportState.Idle) {"))
        assertTrue(
            head("exportBackPostcard")
                .contains("if (_exportState.value !is ExportState.Idle || _shareState.value !is ShareState.Idle) {")
        )
        assertTrue(head("sharePostcard").contains("ShareState.Idle"))
    }

    @Test
    fun viewModelKeepsSeparateStatesAndReadsDoodlesAtExportTime() {
        fun body(function: String, next: String): String {
            val start = viewModel.indexOf("fun $function(")
            return viewModel.substring(start, viewModel.indexOf(next, start))
        }

        val gallery = body("exportPostcardToGallery", "fun resetExportState(")
        val share = body("sharePostcard", "fun resetShareState(")

        assertTrue(gallery.contains(".exportToGallery("))
        assertTrue(gallery.contains("_exportState.value"))
        assertTrue(gallery.contains("이미지를 저장하지 못했습니다."))
        assertTrue(share.contains(".exportForSharing("))
        assertTrue(share.contains("_shareState.value"))
        assertTrue(share.contains("엽서 이미지를 준비하지 못했어요."))

        listOf(gallery, share).forEach { function ->
            val io = function.indexOf("withContext(Dispatchers.IO)")
            val doodles = function.indexOf("doodleStrokes = _doodleStrokes.value")
            assertTrue("낙서는 IO 블록 안에서 읽는 기존 시점을 유지해야 한다", doodles > io && io >= 0)
            assertTrue(function.contains("overlays = overlays"))
        }
    }

    @Test
    fun exporterHasOneRenderSite_andKeepsItsOwnOutputForEachPath() {
        assertEquals(1, count(exporter, """(?<!fun )\bcreatePostcardBitmap\("""))

        val gallery = exporter.substring(
            exporter.indexOf("fun exportToGallery("),
            exporter.indexOf("fun exportForSharing(")
        )
        val share = exporter.substring(
            exporter.indexOf("fun exportForSharing("),
            exporter.indexOf("private fun <T> renderFrontThenOutput(")
        )

        assertTrue(gallery.contains("saveBitmapToGallery("))
        assertTrue(!gallery.contains("saveBitmapForSharing("))
        assertTrue(share.contains("saveBitmapForSharing("))
        assertTrue(share.contains("postcardId = postcard.id"))
        assertTrue(!share.contains("saveBitmapToGallery("))
    }

    @Test
    fun renderReceivesEachOverlayKind_inItsOwnParameter() {
        val call = extractBalancedCall(exporter, exporter.indexOf("createPostcardBitmap(\n                    context"))

        listOf(
            "stickerOverlays = overlays.stickers",
            "sealOverlays = overlays.seals",
            "doodleStrokes = doodleStrokes",
            "textStickerOverlays = overlays.textStickers",
            "maskingTapeOverlays = overlays.maskingTapes",
            "labelStickerOverlays = overlays.labelStickers"
        ).forEach { argument ->
            assertTrue("[$argument] 연결이 달라졌다", call.contains(argument))
        }
    }

    @Test
    fun renderedBitmapIsRecycledThroughTheSharedRelease() {
        val call = extractBalancedCall(exporter, exporter.indexOf("renderThenOutput(\n            render"))

        assertTrue(call.contains("if (!bitmap.isRecycled)"))
        assertTrue(call.contains("bitmap.recycle()"))
    }

    @Test
    fun outputConditionsStayFixed() {
        assertTrue(exporter.contains("private const val OUTPUT_SIZE = 2048"))
        assertEquals(
            "공유와 갤러리 저장 모두 PNG 100으로 저장한다",
            2,
            count(exporter, """Bitmap\.CompressFormat\.PNG,\s*100,""")
        )
        assertTrue(exporter.contains("\"postcard_memory_\""))
        assertTrue(exporter.contains("\"\${Environment.DIRECTORY_PICTURES}/PostcardMemory\""))
        assertTrue(exporter.contains("SHARE_CACHE_DIR_NAME = \"shared_postcards\""))
    }

    @Test
    fun layersDrawInFixedOrder() {
        val positions = listOf(
            "PostcardRenderSpec.drawBaseContent(",
            "for (overlay in maskingTapeOverlays)",
            "for (overlay in stickerOverlays)",
            "for (overlay in sealOverlays)",
            "for (overlay in textStickerOverlays)",
            "PostcardRenderSpec.drawDoodleStrokes(",
            "for (overlay in labelStickerOverlays)"
        ).map { marker ->
            exporter.indexOf(marker).also { assertTrue("[$marker]를 찾지 못했다", it >= 0) }
        }

        assertEquals(positions.sorted(), positions)
    }
}
