package com.postcardmemory.ui.detail

import com.postcardmemory.data.Postcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PostcardTemplate/PostcardTemplateStyle은 Postcard와 마찬가지로 Long/String/
 * Float만 다루는 순수 데이터 클래스라 Robolectric 없이 검증할 수 있다.
 */
class PostcardTemplateTest {

    private fun sampleStyle() =
        PostcardTemplateStyle(
            layoutStyle = "LETTER",
            backgroundColorArgb = 0xFFF5EBDDL,
            backgroundPattern = "SPECKLE",
            backgroundPatternDensity = 0.8f,
            messageFont = "SERIF",
            dateFormat = "DOT",
            messageTextScale = 1.1f,
            dateTextScale = 0.95f,
            photoEdgeBlur = 0.2f,
            stampPhotoScale = 0.9f,
            stampPhotoOffsetX = 0.1f,
            stampPhotoOffsetY = -0.2f,
            stampPhotoZoom = 1.05f,
            polaroidPhotoScale = 1f,
            polaroidPhotoOffsetX = 0f,
            polaroidPhotoOffsetY = 0f,
            polaroidPhotoZoom = 1f,
            tapedFilmPhotoOffsetX = 0f,
            tapedFilmPhotoOffsetY = 0f,
            tapedFilmPhotoZoom = 1f
        )

    private fun samplePostcard(id: Long = 1L) =
        Postcard(
            id = id,
            imagePath = "/data/postcards/photo_$id.jpg",
            title = "postcard-$id",
            capturedAt = 1_700_000_000_000L,
            location = "제주",
            message = "오늘의 기록",
            backgroundColorArgb = 0xFFFFFBF7L,
            backgroundImagePath = "/data/postcard_backgrounds/bg_$id.jpg",
            backgroundPattern = "NONE",
            layoutStyle = "STAMP"
        )

    // ---- 직렬화 라운드트립 ----

    @Test
    fun serialize_thenParse_roundTripsExactly_noSeal() {
        val template =
            PostcardTemplate(
                id = "abc-123",
                source = TemplateSource.USER,
                name = "나의 템플릿 1",
                style = sampleStyle(),
                seal = null,
                createdAtMillis = 1_000L,
                updatedAtMillis = 2_000L
            )

        val parsed = parsePostcardTemplate(template.serialize())

        assertEquals(template.id, parsed?.id)
        assertEquals(template.name, parsed?.name)
        assertEquals(template.style, parsed?.style)
        assertNull(parsed?.seal)
        assertEquals(template.createdAtMillis, parsed?.createdAtMillis)
        assertEquals(template.updatedAtMillis, parsed?.updatedAtMillis)
        // 파일에서 읽은 템플릿은 항상 사용자 템플릿으로 취급한다(내장 템플릿은 파일로 저장되지 않음).
        assertEquals(TemplateSource.USER, parsed?.source)
    }

    @Test
    fun serialize_thenParse_roundTripsExactly_withSeal() {
        val template =
            PostcardTemplate(
                id = "with-seal",
                name = "여행 우편 사본",
                style = sampleStyle(),
                seal = PostcardTemplateSeal(
                    type = SealType.CIRCLE_POSTMARK,
                    colorArgb = 0xFF7A3B2EL
                ),
                createdAtMillis = 5_000L,
                updatedAtMillis = 5_000L
            )

        val parsed = parsePostcardTemplate(template.serialize())

        assertEquals(SealType.CIRCLE_POSTMARK, parsed?.seal?.type)
        assertEquals(0xFF7A3B2EL, parsed?.seal?.colorArgb)
    }

    @Test
    fun parse_corruptedHeader_returnsNull() {
        val parsed = parsePostcardTemplate("NOT_A_TEMPLATE\nabc\tname\t1\t1\t0")

        assertNull(parsed)
    }

    @Test
    fun parse_truncatedStyleLine_returnsNull() {
        val text = "POSTCARD_TEMPLATE_V1\nid\tname\t1\t1\t0\nSTAMP\t100"

        assertNull(parsePostcardTemplate(text))
    }

    @Test
    fun parse_blankId_returnsNull() {
        val template =
            PostcardTemplate(
                id = "",
                name = "이름",
                style = sampleStyle(),
                createdAtMillis = 1L,
                updatedAtMillis = 1L
            )

        assertNull(parsePostcardTemplate(template.serialize()))
    }

    @Test
    fun serialize_nameWithTabOrNewline_isSanitized_stillParsesBack() {
        val template =
            PostcardTemplate(
                id = "tabbed",
                name = "이름\t줄\n바꿈",
                style = sampleStyle(),
                createdAtMillis = 1L,
                updatedAtMillis = 1L
            )

        val parsed = parsePostcardTemplate(template.serialize())

        assertTrue(parsed != null)
        assertTrue(parsed!!.name.none { it == '\t' || it == '\n' })
    }

    @Test
    fun defaultConstructor_generatesUniqueIds() {
        val first = PostcardTemplate(name = "a", style = sampleStyle())
        val second = PostcardTemplate(name = "b", style = sampleStyle())

        assertNotEquals(first.id, second.id)
    }

    // ---- 적용 시 기록 보호(toTemplateStyle / applyTemplateStyle) ----

    @Test
    fun toTemplateStyle_extractsOnlyStyleFields() {
        val postcard = samplePostcard()

        val style = postcard.toTemplateStyle()

        assertEquals(postcard.layoutStyle, style.layoutStyle)
        assertEquals(postcard.backgroundColorArgb, style.backgroundColorArgb)
        assertEquals(postcard.backgroundPattern, style.backgroundPattern)
    }

    @Test
    fun applyTemplateStyle_changesOnlyStyleFields_keepsRecordFields() {
        val postcard = samplePostcard()
        val style = sampleStyle()

        val applied = postcard.applyTemplateStyle(style)

        // 기록(사진/문구/날짜/위치/ID/배경사진 파일)은 그대로.
        assertEquals(postcard.id, applied.id)
        assertEquals(postcard.imagePath, applied.imagePath)
        assertEquals(postcard.title, applied.title)
        assertEquals(postcard.message, applied.message)
        assertEquals(postcard.capturedAt, applied.capturedAt)
        assertEquals(postcard.location, applied.location)
        assertEquals(postcard.backgroundImagePath, applied.backgroundImagePath)

        // 스타일 값만 템플릿 값으로 바뀐다.
        assertEquals(style.layoutStyle, applied.layoutStyle)
        assertEquals(style.backgroundColorArgb, applied.backgroundColorArgb)
        assertEquals(style.backgroundPattern, applied.backgroundPattern)
        assertEquals(style.stampPhotoScale, applied.stampPhotoScale)
    }

    @Test
    fun applyTemplateStyle_thenToTemplateStyle_isIdempotent() {
        val postcard = samplePostcard()
        val style = sampleStyle()

        val roundTripped = postcard.applyTemplateStyle(style).toTemplateStyle()

        assertEquals(style, roundTripped)
    }
}
