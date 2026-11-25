package com.postcardmemory.ui.bookmark

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 책갈피가 기존 편집·저장·공유 경로에 섞이지 않는지 소스 기준으로 고정해(실제 화면 검증은 실기기 QA 몫). */
class MemoryBookmarkWiringStructureTest {

    private fun source(path: String): String =
        listOf("src/main/java/com/postcardmemory/$path", "app/src/main/java/com/postcardmemory/$path")
            .map(::File).first(File::exists).readText()

    @Test fun sharingExportDraftAndConfirmedSaveDoNotKnowAboutBookmarks() {
        for (path in listOf(
            "utils/PostcardImageExporter.kt", "utils/PostcardRenderSpec.kt",
            "ui/components/PostcardBackExportCapture.kt", "ui/detail/DetailViewModel.kt"
        )) {
            assertFalse(path, source(path).contains("bookmark", ignoreCase = true))
        }
    }

    @Test fun studioDoesNotTouchThePostcardEditor() {
        for (path in listOf(
            "ui/bookmark/MemoryBookmarkSession.kt", "ui/bookmark/MemoryBookmarkViewModel.kt",
            "ui/bookmark/MemoryBookmarkStudio.kt"
        )) {
            val text = source(path)
            assertFalse(path, text.contains("com.postcardmemory.ui.detail"))
            assertFalse(path, text.contains("PostcardRepository"))
            assertFalse(path, text.contains("PostcardDao"))
        }
    }

    /** 연못·쉼터·쫑쫑컵·흔들어서 한 장은 책갈피를 그리지 않고, 월별 보기만 넘겨. */
    @Test fun onlyTheMonthlyGridDrawsBookmarks() {
        val gallery = source("ui/gallery/GalleryScreen.kt")
        assertEquals(1, Regex("""bookmark = bookmarks\?\.get\(postcard\.id\)""").findAll(gallery).count())
        for (path in listOf(
            "ui/gallery/GalleryPlayStage.kt", "ui/gallery/PostcardRaceOverlay.kt",
            "ui/gallery/PixelPostcardRaceCar.kt", "ui/gallery/GalleryRandomPostcardOverlay.kt"
        )) {
            assertFalse(path, source(path).contains("bookmark", ignoreCase = true))
        }
    }

    /** 사진을 불러오기 전에도 뒤의 책갈피가 카드를 뚫고 비치지 않게 불투명 종이를 깔아. */
    @Test fun bookmarkedCardHasAnOpaquePaperBehindThePhoto() {
        assertTrue(source("ui/components/StampCard.kt").contains("Modifier.background(PaperSurface, PinkingPhotoShape)"))
        assertTrue(source("ui/bookmark/MemoryBookmarkStudio.kt").contains(".background(PaperSurface, PinkingPhotoShape)"))
    }
}
