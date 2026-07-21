package com.postcardmemory.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostcardImageExporterShareTest {

    @Test
    fun shareFileNameFor_matchesTimestampedPngPattern() {
        val fileName =
            PostcardImageExporter.shareFileNameFor(timestampMillis = 0L)

        assertTrue(
            fileName.matches(
                Regex("postcard_\\d{4}-\\d{2}-\\d{2}_\\d{6}\\.png")
            )
        )
    }

    @Test
    fun shareFileNameFor_isStableForSameTimestamp() {
        val first =
            PostcardImageExporter.shareFileNameFor(timestampMillis = 1_700_000_000_000L)
        val second =
            PostcardImageExporter.shareFileNameFor(timestampMillis = 1_700_000_000_000L)

        assertEquals(first, second)
    }

    @Test
    fun shareFileNameFor_differsAcrossTimestamps() {
        val first =
            PostcardImageExporter.shareFileNameFor(timestampMillis = 1_700_000_000_000L)
        val second =
            PostcardImageExporter.shareFileNameFor(timestampMillis = 1_700_000_060_000L)

        assertFalse(first == second)
    }

    @Test
    fun shareFileNameFor_containsNoPathTraversalOrUserText() {
        val fileName =
            PostcardImageExporter.shareFileNameFor(timestampMillis = 1_700_000_000_000L)

        assertFalse(fileName.contains(".."))
        assertFalse(fileName.contains("/"))
        assertFalse(fileName.contains("\\"))
    }
}
