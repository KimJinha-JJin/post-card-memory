package com.postcardmemory.ui.gallery

import com.postcardmemory.data.Postcard
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 141일차 B안 삭제 확인창의 썸네일 고르기([galleryDeleteDialogThumbnails]): 선택한 엽서만,
 * 갤러리 순서 그대로, 앞의 4장만 보여주고 나머지는 "+N" 수로 넘긴다.
 */
class GalleryDeleteDialogThumbnailsTest {

    private fun postcard(id: Long) =
        Postcard(id = id, imagePath = "photo$id", title = "t$id", capturedAt = id * 1_000L)

    private val gallery = (1L..10L).map(::postcard)

    @Test
    fun showsOnlySelectedPostcardsInGalleryOrder() {
        val result = galleryDeleteDialogThumbnails(gallery, setOf(7L, 2L, 5L))

        assertEquals(listOf(2L, 5L, 7L), result.shown.map { it.id })
        assertEquals(0, result.remaining)
    }

    @Test
    fun capsAtFourAndCountsTheRest() {
        val result = galleryDeleteDialogThumbnails(gallery, (1L..9L).toSet())

        assertEquals(GALLERY_DELETE_THUMBNAIL_LIMIT, result.shown.size)
        assertEquals(listOf(1L, 2L, 3L, 4L), result.shown.map { it.id })
        assertEquals(5, result.remaining)
    }

    @Test
    fun exactlyFourHasNoRemainder() {
        val result = galleryDeleteDialogThumbnails(gallery, setOf(1L, 2L, 3L, 4L))

        assertEquals(4, result.shown.size)
        assertEquals(0, result.remaining)
    }

    @Test
    fun idsMissingFromGalleryAreIgnored() {
        // 이미 사라진 엽서 id가 선택에 남아 있어도 없는 썸네일을 만들지 않는다.
        val result = galleryDeleteDialogThumbnails(gallery, setOf(3L, 99L))

        assertEquals(listOf(3L), result.shown.map { it.id })
        assertEquals(0, result.remaining)
    }
}
