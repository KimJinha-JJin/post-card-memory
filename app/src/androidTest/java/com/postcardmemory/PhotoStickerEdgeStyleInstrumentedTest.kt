package com.postcardmemory

import android.net.Uri
import com.postcardmemory.ui.detail.PhotoStickerEdgeStyle
import com.postcardmemory.ui.detail.PhotoStickerItem
import com.postcardmemory.ui.detail.deserializePhotoStickerItem
import com.postcardmemory.ui.detail.renderedEdgeStyle
import com.postcardmemory.ui.detail.serialize
import com.postcardmemory.ui.detail.withStickerEdgeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 사진 스티커 오림 스타일의 저장·복원·편집 규칙을 실제 android.net.Uri와 함께
 * 검증한다(PhotoStickerItem은 Uri 때문에 순수 JUnit에서 만들 수 없다).
 * 파일·DB·앱 데이터를 건드리지 않는 순수 객체 테스트다.
 */
class PhotoStickerEdgeStyleInstrumentedTest {

    private val uri = Uri.parse("file:///data/user/0/test/sticker.jpg")

    private fun sticker(
        id: String = "s1",
        edgeStyle: PhotoStickerEdgeStyle = PhotoStickerEdgeStyle.DEFAULT,
        edgeSeed: Long = 0L,
        isBackgroundRemoved: Boolean = false
    ) = PhotoStickerItem(
        id = id,
        originalUri = uri,
        displayedUri = uri,
        isBackgroundRemoved = isBackgroundRemoved,
        scale = 1.3f,
        rotationDegrees = 15f,
        edgeStyle = edgeStyle,
        edgeSeed = edgeSeed
    )

    @Test
    fun legacyLinesWithoutStyleFieldsRestoreAsDefault() {
        // 85일차 이전 형식(11필드)과 더 옛 형식(8필드).
        val legacy11 =
            "s1\t$uri\t$uri\t~\tfalse\t10.0\t20.0\t1.3\t15.0\ttrue\tfalse"
        val legacy8 =
            "s2\t$uri\t$uri\t~\tfalse\t~\t~\t0.8"

        val restored11 = deserializePhotoStickerItem(legacy11)
        val restored8 = deserializePhotoStickerItem(legacy8)

        assertNotNull(restored11)
        assertEquals(PhotoStickerEdgeStyle.DEFAULT, restored11!!.edgeStyle)
        assertEquals(0L, restored11.edgeSeed)
        assertEquals(15f, restored11.rotationDegrees)
        assertEquals(true, restored11.flipHorizontal)

        assertNotNull(restored8)
        assertEquals(PhotoStickerEdgeStyle.DEFAULT, restored8!!.edgeStyle)
    }

    @Test
    fun unknownStyleAndBadSeedDoNotDropTheSticker() {
        val line =
            "s1\t$uri\t$uri\t~\tfalse\t~\t~\t1.0\t0.0\tfalse\tfalse\tFUTURE_STYLE\tnot-a-number"

        val restored = deserializePhotoStickerItem(line)

        assertNotNull(restored)
        assertEquals(PhotoStickerEdgeStyle.DEFAULT, restored!!.edgeStyle)
        assertEquals(0L, restored.edgeSeed)
    }

    @Test
    fun styleAndSeedRoundTrip() {
        PhotoStickerEdgeStyle.entries.forEach { style ->
            val original = sticker(edgeStyle = style, edgeSeed = -9_876_543_210L)

            assertEquals(original, deserializePhotoStickerItem(original.serialize()))
        }
    }

    @Test
    fun firstStyleAssignsSeedOnceAndLaterChangesKeepIt() {
        var seedCalls = 0
        val nextSeed = { seedCalls++; 777L }

        val first =
            listOf(sticker()).withStickerEdgeStyle(
                "s1",
                PhotoStickerEdgeStyle.POLAROID,
                nextSeed
            )!!
        assertEquals(777L, first.single().edgeSeed)

        val back =
            first.withStickerEdgeStyle("s1", PhotoStickerEdgeStyle.DEFAULT, nextSeed)!!
        assertEquals(PhotoStickerEdgeStyle.DEFAULT, back.single().edgeStyle)
        assertEquals(777L, back.single().edgeSeed)
        assertEquals(1, seedCalls)
    }

    @Test
    fun sameStyleOrUnknownStickerIsNoChange() {
        val stickers = listOf(sticker(edgeStyle = PhotoStickerEdgeStyle.POLAROID, edgeSeed = 5L))

        assertNull(stickers.withStickerEdgeStyle("s1", PhotoStickerEdgeStyle.POLAROID))
        assertNull(stickers.withStickerEdgeStyle("missing", PhotoStickerEdgeStyle.DEFAULT))
    }

    @Test
    fun backgroundRemovedStickerKeepsStoredStyleButRendersDefault() {
        val removed =
            sticker(
                edgeStyle = PhotoStickerEdgeStyle.POLAROID,
                edgeSeed = 42L,
                isBackgroundRemoved = true
            )

        assertNull(listOf(removed).withStickerEdgeStyle("s1", PhotoStickerEdgeStyle.DEFAULT))
        assertEquals(PhotoStickerEdgeStyle.DEFAULT, removed.renderedEdgeStyle())

        // 원본복원(DetailScreen의 onToggleBackgroundRemoval과 같은 copy) 후 그대로 돌아온다.
        val restored = removed.copy(displayedUri = removed.originalUri, isBackgroundRemoved = false)
        assertEquals(PhotoStickerEdgeStyle.POLAROID, restored.renderedEdgeStyle())
        assertEquals(42L, restored.edgeSeed)
    }

    @Test
    fun duplicateCopyKeepsStyleAndSeed() {
        val original = sticker(edgeStyle = PhotoStickerEdgeStyle.POLAROID, edgeSeed = 31L)

        // DetailViewModel.duplicateSticker의 copy(id = 새 id, offset = …)와 같은 형태.
        val duplicate = original.copy(id = "s2")

        assertEquals(PhotoStickerEdgeStyle.POLAROID, duplicate.edgeStyle)
        assertEquals(31L, duplicate.edgeSeed)
    }
}
