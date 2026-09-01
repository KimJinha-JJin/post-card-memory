package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PostcardSealItem은 Float/Long/String만 다루는 순수 데이터 클래스라
 * Robolectric 없이 검증할 수 있다.
 */
class PostcardSealItemTest {

    private fun sampleSeal() =
        PostcardSealItem(
            id = "seal-1",
            type = SealType.PIGEON_TRACK,
            offset = Offset(0.2f, -0.4f),
            scale = 0.6f,
            rotationDegrees = 15f,
            colorArgb = 0xFF112233L
        )

    @Test
    fun serialize_thenParse_roundTripsExactly() {
        val seal = sampleSeal()

        val parsed = deserializePostcardSealItem(seal.serialize())

        assertEquals(seal, parsed)
    }

    @Test
    fun serialize_thenParse_roundTripsExactly_nullOffset() {
        val seal = sampleSeal().copy(offset = null)

        val parsed = deserializePostcardSealItem(seal.serialize())

        assertEquals(seal, parsed)
    }

    @Test
    fun parse_tooFewFields_returnsNull() {
        assertNull(deserializePostcardSealItem("only\tthree\tfields"))
    }

    @Test
    fun parse_nonNumericScale_returnsNull() {
        val corrupted =
            listOf("seal-1", "CIRCLE_POSTMARK", "~", "~", "not-a-float", "0", "16777215")
                .joinToString("\t")

        assertNull(deserializePostcardSealItem(corrupted))
    }

    // ---- 인식하지 못하는 SealType은 다른 도장으로 대체하지 않고 이 항목만 버린다 ----
    // (건강검진 후속 라운드에서 발견: deserializePhotoStickerItem, PostcardTemplate.
    // parseTemplateSeal은 둘 다 손상된 개별 항목을 null로 버리는데, 이 함수만
    // CIRCLE_POSTMARK로 조용히 대체하는 유일한 예외였다. 현재 삭제/변경된
    // SealType은 없어 오늘 도달 가능한 결함은 아니지만, 프로젝트 전체 패턴과
    // 정렬해 향후 enum 이름이 바뀌어도 엉뚱한 도장으로 둔갑하지 않게 한다.)

    @Test
    fun parse_unknownSealType_returnsNull_doesNotSubstituteDefaultType() {
        val line =
            listOf("seal-1", "UNKNOWN_FUTURE_SEAL_TYPE", "~", "~", "1.0", "0.0", "2434341")
                .joinToString("\t")

        assertNull(deserializePostcardSealItem(line))
    }
}
