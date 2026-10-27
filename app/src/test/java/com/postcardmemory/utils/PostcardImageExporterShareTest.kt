package com.postcardmemory.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 5일차: 공유 파일명이 초 단위 timestamp만으로 충돌하던 문제를 검증한다.
 * 예전에는 같은 초에 두 번 공유하면 파일명이 완전히 같아져 두 번째 공유가
 * 첫 번째 파일을 덮어썼다 — 그래서 "같은 timestamp면 같은 이름"이던 예전
 * 테스트를 "기본 호출은 매번 달라야 한다"는 정반대 계약으로 바꿨다.
 */
class PostcardImageExporterShareTest {

    @Test
    fun shareFileNameFor_matchesExpectedPattern() {
        val fileName =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 42L,
                timestampMillis = 0L,
                uuid = "11111111-1111-1111-1111-111111111111"
            )

        assertEquals(
            "postcard_42_0_11111111-1111-1111-1111-111111111111.png",
            fileName
        )
    }

    @Test
    fun shareFileNameFor_defaultCall_differsEvenForSameInstantAndPostcard() {
        // uuid를 지정하지 않으면(실제 프로덕션 호출 방식) 같은 postcardId와
        // 같은 timestampMillis라도 매번 새 UUID가 붙어 파일명이 달라야 한다.
        // 이게 "같은 초에 두 번 공유해도 파일명이 충돌하지 않는다"의 핵심.
        val first =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 1L,
                timestampMillis = 1_700_000_000_000L
            )
        val second =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 1L,
                timestampMillis = 1_700_000_000_000L
            )

        assertFalse(first == second)
    }

    @Test
    fun shareFileNameFor_isDeterministic_whenUuidExplicitlyFixed() {
        val first =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 1L,
                timestampMillis = 1_700_000_000_000L,
                uuid = "same-uuid"
            )
        val second =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 1L,
                timestampMillis = 1_700_000_000_000L,
                uuid = "same-uuid"
            )

        assertEquals(first, second)
    }

    @Test
    fun shareFileNameFor_differsAcrossPostcardIds() {
        val first =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 1L,
                timestampMillis = 1_700_000_000_000L,
                uuid = "same-uuid"
            )
        val second =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 2L,
                timestampMillis = 1_700_000_000_000L,
                uuid = "same-uuid"
            )

        assertFalse(first == second)
    }

    @Test
    fun shareFileNameFor_differsAcrossTimestamps() {
        val first =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 1L,
                timestampMillis = 1_700_000_000_000L,
                uuid = "same-uuid"
            )
        val second =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 1L,
                timestampMillis = 1_700_000_060_000L,
                uuid = "same-uuid"
            )

        assertFalse(first == second)
    }

    @Test
    fun shareFileNameFor_containsNoPathTraversalOrUserText() {
        val fileName =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 1L,
                timestampMillis = 1_700_000_000_000L
            )

        assertFalse(fileName.contains(".."))
        assertFalse(fileName.contains("/"))
        assertFalse(fileName.contains("\\"))
    }

    @Test
    fun shareFileNameFor_endsWithPngExtension() {
        val fileName =
            PostcardImageExporter.shareFileNameFor(
                postcardId = 1L,
                timestampMillis = 1_700_000_000_000L
            )

        assertTrue(fileName.endsWith(".png"))
    }
}
