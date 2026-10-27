package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 78일차 P3-7: 파일 소유권을 문자열 prefix로만 판정하던 문제.
 *
 * `path.startsWith(root.path)`로 되돌리면 sibling 테스트가 실패한다 —
 * 이 판정은 곧바로 `delete()`로 이어지므로 잘못 통과하면 남의 폴더 파일을
 * 지우게 된다.
 */
class AppFileOwnershipTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun filesDir(): File = tempFolder.newFolder("files")

    @Test
    fun fileDirectlyInsideTheRoot_isOwned() {
        val filesDir = filesDir()
        val root = File(filesDir, "sticker_originals").apply { mkdirs() }
        val file = File(root, "a.jpg").apply { writeText("x") }

        assertTrue(isInsideDirectory(root, file))
    }

    @Test
    fun fileInANestedSubdirectory_isOwned() {
        val filesDir = filesDir()
        val root = File(filesDir, "draft_sticker_bgs").apply { mkdirs() }
        val nested = File(root, "42/inner").apply { mkdirs() }
        val file = File(nested, "b.png").apply { writeText("x") }

        assertTrue(isInsideDirectory(root, file))
    }

    @Test
    fun theRootItself_isNotOwned() {
        val filesDir = filesDir()
        val root = File(filesDir, "sticker_originals").apply { mkdirs() }

        assertFalse(isInsideDirectory(root, root))
    }

    @Test
    fun aSiblingDirectoryWhoseNameMerelyStartsWithTheRootName_isNotOwned() {
        val filesDir = filesDir()
        val root = File(filesDir, "sticker_originals").apply { mkdirs() }
        val sibling = File(filesDir, "sticker_originals_backup").apply { mkdirs() }
        val file = File(sibling, "a.jpg").apply { writeText("x") }

        assertFalse(isInsideDirectory(root, file))
    }

    @Test
    fun aSiblingFileWhoseNameMerelyStartsWithTheRootName_isNotOwned() {
        val filesDir = filesDir()
        val root = File(filesDir, "masking_tape_photos").apply { mkdirs() }
        val lookalike = File(filesDir, "masking_tape_photos.bak").apply { writeText("x") }

        assertFalse(isInsideDirectory(root, lookalike))
    }

    @Test
    fun aPathEscapingTheRootWithDotDot_isNotOwned() {
        val filesDir = filesDir()
        val root = File(filesDir, "sticker_bgs").apply { mkdirs() }
        val outside = File(filesDir, "secret.txt").apply { writeText("x") }

        val escaping = File(root, "..${File.separator}secret.txt")

        assertTrue(outside.exists())
        assertFalse(isInsideDirectory(root, escaping))
    }

    @Test
    fun aPathThatWalksOutAndBackIn_isStillOwned() {
        val filesDir = filesDir()
        val root = File(filesDir, "sticker_bgs").apply { mkdirs() }
        File(root, "a.jpg").writeText("x")

        val roundabout = File(root, "..${File.separator}sticker_bgs${File.separator}a.jpg")

        assertTrue(isInsideDirectory(root, roundabout))
    }

    @Test
    fun aCompletelyUnrelatedPath_isNotOwned() {
        val filesDir = filesDir()
        val root = File(filesDir, "sticker_originals").apply { mkdirs() }
        val elsewhere = tempFolder.newFolder("external")
        val file = File(elsewhere, "user_photo.jpg").apply { writeText("x") }

        assertFalse(isInsideDirectory(root, file))
    }

    @Test
    fun theParentOfTheRoot_isNotOwned() {
        val filesDir = filesDir()
        val root = File(filesDir, "sticker_originals").apply { mkdirs() }

        assertFalse(isInsideDirectory(root, filesDir))
    }

    @Test
    fun aFileThatDoesNotExistYet_isJudgedByItsPathAlone() {
        val filesDir = filesDir()
        val root = File(filesDir, "postcards").apply { mkdirs() }

        assertTrue(isInsideDirectory(root, File(root, "not_created_yet.jpg")))
        assertFalse(
            isInsideDirectory(root, File(filesDir, "postcards_old/not_created_yet.jpg"))
        )
    }
}
