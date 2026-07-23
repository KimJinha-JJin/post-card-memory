package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 5일차: 공유 캐시 정리가 "새 공유마다 현재 파일 이름과 다르면 전부 삭제"
 * 하던 예전 방식(직전 공유 파일이 chooser에서 아직 쓰이는 중일 수 있는데도
 * 즉시 지움) 대신 TTL·최대 보관 개수 기반으로만 제한적으로 정리하는지
 * 검증한다. java.io.File만 다루므로 Robolectric 없이 TemporaryFolder로
 * 검증 가능하다.
 */
class PostcardImageExporterShareCacheCleanupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun fileWithAge(dir: File, name: String, ageMillis: Long, now: Long): File {
        val file = File(dir, name)
        file.writeText("fake-png-bytes")
        file.setLastModified(now - ageMillis)
        return file
    }

    @Test
    fun cleanup_neverDeletesCurrentFile_evenIfItWouldBeExpiredByAge() {
        val shareDir = tempFolder.newFolder("shared_postcards")
        val now = 10_000_000L
        // setLastModified로 일부러 아주 오래된 것처럼 보이게 해도 currentFileName이면 지우면 안 된다.
        val current = fileWithAge(shareDir, "current.png", ageMillis = 999_999_999L, now = now)

        PostcardImageExporter.cleanupOldShareFiles(
            shareDir = shareDir,
            currentFileName = "current.png",
            ttlMillis = 1_000L,
            maxFiles = 0,
            nowMillis = now
        )

        assertTrue(current.exists())
    }

    @Test
    fun cleanup_keepsFilesWithinTtl() {
        val shareDir = tempFolder.newFolder("shared_postcards")
        val now = 10_000_000L
        val recent = fileWithAge(shareDir, "recent.png", ageMillis = 1_000L, now = now)

        PostcardImageExporter.cleanupOldShareFiles(
            shareDir = shareDir,
            currentFileName = "current.png",
            ttlMillis = 24 * 60 * 60 * 1000L,
            maxFiles = 20,
            nowMillis = now
        )

        // 방금 만든(직전 공유) 파일은 TTL 안이므로 살아있어야 한다 —
        // "새 공유가 직전 공유 파일을 즉시 삭제하지 않는다"의 핵심.
        assertTrue(recent.exists())
    }

    @Test
    fun cleanup_deletesFilesPastTtl() {
        val shareDir = tempFolder.newFolder("shared_postcards")
        val now = 10_000_000L
        val old = fileWithAge(shareDir, "old.png", ageMillis = 2_000L, now = now)

        PostcardImageExporter.cleanupOldShareFiles(
            shareDir = shareDir,
            currentFileName = "current.png",
            ttlMillis = 1_000L,
            maxFiles = 20,
            nowMillis = now
        )

        assertFalse(old.exists())
    }

    @Test
    fun cleanup_keepsNewestWithinMaxFiles_deletesOldestOverflow() {
        val shareDir = tempFolder.newFolder("shared_postcards")
        val now = 10_000_000L
        // 모두 TTL 이내지만 개수가 maxFiles(2)를 넘는 3개를 만든다.
        val oldest = fileWithAge(shareDir, "a.png", ageMillis = 300L, now = now)
        val middle = fileWithAge(shareDir, "b.png", ageMillis = 200L, now = now)
        val newest = fileWithAge(shareDir, "c.png", ageMillis = 100L, now = now)

        PostcardImageExporter.cleanupOldShareFiles(
            shareDir = shareDir,
            currentFileName = "current.png",
            ttlMillis = 1_000_000L,
            maxFiles = 2,
            nowMillis = now
        )

        assertFalse(oldest.exists())
        assertTrue(middle.exists())
        assertTrue(newest.exists())
    }

    @Test
    fun cleanup_emptyDirectory_doesNotThrow() {
        val shareDir = tempFolder.newFolder("shared_postcards")

        PostcardImageExporter.cleanupOldShareFiles(
            shareDir = shareDir,
            currentFileName = "current.png"
        )
    }

    @Test
    fun cleanup_missingDirectory_doesNotThrow() {
        val shareDir = File(tempFolder.root, "does-not-exist")

        PostcardImageExporter.cleanupOldShareFiles(
            shareDir = shareDir,
            currentFileName = "current.png"
        )
    }

    @Test
    fun cleanup_immediateResharedFile_survivesBecauseWithinTtlAndMaxFiles() {
        // 시나리오 A/C: 같은 초 또는 곧바로 재공유 — 직전 공유 파일이 남아 있어야 한다.
        val shareDir = tempFolder.newFolder("shared_postcards")
        val now = 10_000_000L
        val previousShare = fileWithAge(shareDir, "previous_share.png", ageMillis = 50L, now = now)

        PostcardImageExporter.cleanupOldShareFiles(
            shareDir = shareDir,
            currentFileName = "new_share.png",
            ttlMillis = 24 * 60 * 60 * 1000L,
            maxFiles = 20,
            nowMillis = now
        )

        assertTrue(previousShare.exists())
    }
}
