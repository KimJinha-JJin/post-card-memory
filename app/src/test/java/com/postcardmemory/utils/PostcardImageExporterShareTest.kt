package com.postcardmemory.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostcardImageExporterShareTest {

    @Test
    fun shareFileNameFor_usesPostcardIdAndPngExtension() {
        assertEquals(
            "postcard_share_42.png",
            PostcardImageExporter.shareFileNameFor(42L)
        )
    }

    @Test
    fun shareFileNameFor_isStableForSameId() {
        val first = PostcardImageExporter.shareFileNameFor(7L)
        val second = PostcardImageExporter.shareFileNameFor(7L)

        assertEquals(first, second)
    }

    @Test
    fun shareFileNameFor_differsAcrossIds() {
        val first = PostcardImageExporter.shareFileNameFor(1L)
        val second = PostcardImageExporter.shareFileNameFor(2L)

        assertFalse(first == second)
    }

    @Test
    fun shareFileNameFor_containsNoPathTraversalOrUserText() {
        val fileName = PostcardImageExporter.shareFileNameFor(123L)

        assertTrue(
            fileName.matches(Regex("postcard_share_[0-9]+\\.png"))
        )
        assertFalse(fileName.contains(".."))
        assertFalse(fileName.contains("/"))
        assertFalse(fileName.contains("\\"))
    }
}
