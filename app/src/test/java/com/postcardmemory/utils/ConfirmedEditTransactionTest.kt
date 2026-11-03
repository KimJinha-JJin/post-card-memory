package com.postcardmemory.utils

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 121일차: 완료 저장(꾸미기 여섯 종 + 확정 누끼)의 부분 저장 위험과 [ConfirmedEditTransaction]
 * 검증. 실제 사용자 파일이 아니라 JUnit 임시 폴더에 엽서 한 장 분량의 확정 파일을 만들고,
 * 실패는 실제 파일시스템 조건(임시 파일 자리에 폴더, 비어 있지 않은 폴더로 교체 등)이나
 * 교체 함수 주입으로 만든다. 반환값만 보지 않고 파일 내용(해시)을 직접 비교한다.
 */
class ConfirmedEditTransactionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /** 완료 저장이 쓰는 여섯 상태 파일과 같은 배치(종류별 폴더/<엽서>.txt). */
    private val kinds =
        listOf(
            "sticker_states",
            "seal_states",
            "doodle_states",
            "text_sticker_states",
            "masking_tape_states",
            "label_sticker_states"
        )

    private fun confirmedFiles(filesDir: File): List<File> =
        kinds.map { File(File(filesDir, it), "7.txt") }

    private fun writeOldConfirmed(files: List<File>) {
        files.forEachIndexed { index, file ->
            file.parentFile!!.mkdirs()
            file.writeText("old-$index")
        }
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }

    private fun hashes(files: List<File>): List<String> = files.map(::sha256)

    /**
     * 임시 파일 자리에 비어 있지 않은 폴더를 두면 그 종류의 준비(쓰기)가 실제 I/O 오류로 실패한다
     * (비어 있지 않아 실패 뒤 정리에서도 지워지지 않는다).
     */
    private fun blockStaging(target: File): File {
        val blocker = File(target.parentFile, "${target.name}.tmp")
        File(blocker, "occupied").apply {
            parentFile!!.mkdirs()
            writeText("x")
        }
        return blocker
    }

    private fun leftoverTempFiles(filesDir: File): List<File> =
        filesDir.walkTopDown().filter { it.isFile && it.name.endsWith(".tmp") }.toList()

    // ── 재현: 기존 방식(종류마다 곧바로 원자 교체) ──

    @Test
    fun sequentialAtomicWrites_middleFailure_leavesMixedConfirmedState() {
        // 121일차 이전 saveEditsAndClearDraft가 하던 순서 그대로 실제 writeTextAtomically를 부른다.
        // 파일 하나하나는 원자적이지만, 세 번째(낙서)에서 실패하면 앞의 두 종류는 이미 새 내용이다.
        val filesDir = tempFolder.newFolder("files")
        val files = confirmedFiles(filesDir)
        writeOldConfirmed(files)
        blockStaging(files[2])

        val results =
            files.mapIndexed { index, file ->
                ConfirmedEditStateStorage.writeTextAtomically(file, "new-$index")
            }

        assertEquals(listOf(true, true, false, true, true, true), results)
        assertEquals("new-0", files[0].readText())
        assertEquals("new-1", files[1].readText())
        assertEquals("old-2", files[2].readText())
        // 기존 코드는 &&로 묶지 않고 여섯 종을 모두 시도하므로 뒤 종류도 새 내용이 된다.
        assertEquals("new-5", files[5].readText())
    }

    @Test
    fun inPlaceBackgroundCopy_changesConfirmedCutoutBeforeStateIsSaved() {
        // 121일차 이전: 확정 누끼는 상태 파일을 쓰기 전에 copyTo(overwrite = true)로 제자리에 덮였다.
        // 이후 상태 파일 저장이 실패해도 기존 확정본의 누끼 이미지는 이미 바뀌어 있다.
        val filesDir = tempFolder.newFolder("files")
        val confirmedCutout = File(File(filesDir, "sticker_bgs/7"), "a.png")
        confirmedCutout.parentFile!!.mkdirs()
        confirmedCutout.writeBytes(byteArrayOf(1, 2, 3))
        val before = sha256(confirmedCutout)
        val newCutout = File(tempFolder.newFolder("cache"), "new.png")
        newCutout.writeBytes(byteArrayOf(9, 9, 9, 9))

        newCutout.copyTo(confirmedCutout, overwrite = true)

        assertFalse(before == sha256(confirmedCutout))
    }

    // ── 트랜잭션: 준비 실패는 확정 파일을 바꾸지 않는다 ──

    private fun assertStagingFailureKeepsOldConfirmed(failingIndex: Int) {
        val filesDir = tempFolder.newFolder("files-$failingIndex")
        val files = confirmedFiles(filesDir)
        writeOldConfirmed(files)
        val before = hashes(files)
        blockStaging(files[failingIndex])

        val transaction = ConfirmedEditTransaction()
        var allStaged = true
        for ((index, file) in files.withIndex()) {
            if (!transaction.stageText(file, "new-$index")) {
                allStaged = false
                break
            }
        }
        assertFalse(allStaged)
        transaction.discard()

        assertEquals(before, hashes(files))
        // 준비 실패를 일으키려고 둔 폴더(.tmp 이름의 폴더)는 파일이 아니라 집계에서 빠진다.
        assertEquals(emptyList<File>(), leftoverTempFiles(filesDir))
    }

    @Test
    fun firstStagingFailure_keepsEveryConfirmedFile() = assertStagingFailureKeepsOldConfirmed(0)

    @Test
    fun middleStagingFailure_keepsEveryConfirmedFile() = assertStagingFailureKeepsOldConfirmed(2)

    @Test
    fun lastStagingFailure_keepsEveryConfirmedFile() = assertStagingFailureKeepsOldConfirmed(5)

    @Test
    fun allStaged_commit_replacesEveryConfirmedFile_andLeavesNoTemp() {
        val filesDir = tempFolder.newFolder("files")
        val files = confirmedFiles(filesDir)
        writeOldConfirmed(files)

        val transaction = ConfirmedEditTransaction()
        files.forEachIndexed { index, file ->
            assertTrue(transaction.stageText(file, "new-$index"))
        }
        // 준비만으로는 아무것도 바뀌지 않는다.
        files.forEachIndexed { index, file -> assertEquals("old-$index", file.readText()) }

        assertEquals(ConfirmedEditTransaction.CommitResult.COMMITTED, transaction.commit())

        files.forEachIndexed { index, file -> assertEquals("new-$index", file.readText()) }
        assertEquals(emptyList<File>(), leftoverTempFiles(filesDir))
    }

    @Test
    fun newPostcard_withoutConfirmedFiles_commitCreatesThem() {
        val filesDir = tempFolder.newFolder("files")
        val files = confirmedFiles(filesDir)

        val transaction = ConfirmedEditTransaction()
        files.forEachIndexed { index, file ->
            assertTrue(transaction.stageText(file, "new-$index"))
        }

        assertEquals(ConfirmedEditTransaction.CommitResult.COMMITTED, transaction.commit())
        files.forEachIndexed { index, file -> assertEquals("new-$index", file.readText()) }
    }

    @Test
    fun retryAfterStagingFailure_endsWithEveryFileFromTheRetry() {
        val filesDir = tempFolder.newFolder("files")
        val files = confirmedFiles(filesDir)
        writeOldConfirmed(files)
        val blocker = blockStaging(files[3])

        val first = ConfirmedEditTransaction()
        val firstStaged = files.withIndex().all { (index, file) -> first.stageText(file, "try1-$index") }
        assertFalse(firstStaged)
        first.discard()
        files.forEachIndexed { index, file -> assertEquals("old-$index", file.readText()) }

        // 원인(막힌 자리)이 사라진 뒤 같은 편집을 다시 저장한다.
        assertTrue(blocker.deleteRecursively())
        val retry = ConfirmedEditTransaction()
        files.forEachIndexed { index, file -> assertTrue(retry.stageText(file, "try2-$index")) }
        assertEquals(ConfirmedEditTransaction.CommitResult.COMMITTED, retry.commit())

        files.forEachIndexed { index, file -> assertEquals("try2-$index", file.readText()) }
        assertEquals(emptyList<File>(), leftoverTempFiles(filesDir))
    }

    @Test
    fun leftoverTempFromInterruptedSave_isOverwrittenByNextSave() {
        // 준비 중 프로세스가 끝나 `<이름>.tmp`가 남은 상황을 흉내 낸다. 확정 파일은 그대로고,
        // 다음 저장은 남은 임시 파일을 덮어써 정상 교체한다.
        val filesDir = tempFolder.newFolder("files")
        val files = confirmedFiles(filesDir)
        writeOldConfirmed(files)
        val stale = File(files[0].parentFile, "${files[0].name}.tmp")
        stale.writeText("half-written")

        assertEquals("old-0", files[0].readText())

        val transaction = ConfirmedEditTransaction()
        files.forEachIndexed { index, file -> assertTrue(transaction.stageText(file, "new-$index")) }
        assertEquals(ConfirmedEditTransaction.CommitResult.COMMITTED, transaction.commit())

        assertEquals("new-0", files[0].readText())
        assertFalse(stale.exists())
    }

    // ── 확정 누끼 ──

    @Test
    fun stageCopy_doesNotTouchExistingConfirmedCutout_untilCommit() {
        val filesDir = tempFolder.newFolder("files")
        val confirmedCutout = File(File(filesDir, "sticker_bgs/7"), "a.png")
        confirmedCutout.parentFile!!.mkdirs()
        confirmedCutout.writeBytes(byteArrayOf(1, 2, 3))
        val before = sha256(confirmedCutout)
        val newCutout = File(tempFolder.newFolder("cache"), "new.png")
        newCutout.writeBytes(byteArrayOf(9, 9, 9, 9))
        val stateFile = File(File(filesDir, "sticker_states"), "7.txt")
        stateFile.parentFile!!.mkdirs()
        stateFile.writeText("old-state")
        blockStaging(stateFile)

        val transaction = ConfirmedEditTransaction()
        assertTrue(transaction.stageCopy(newCutout, confirmedCutout))
        assertEquals(before, sha256(confirmedCutout))

        // 뒤이은 상태 파일 준비가 실패하면 누끼도 상태도 기존 확정본 그대로다.
        assertFalse(transaction.stageText(stateFile, "new-state"))
        transaction.discard()

        assertEquals(before, sha256(confirmedCutout))
        assertEquals("old-state", stateFile.readText())
        assertTrue(newCutout.exists())
        assertEquals(emptyList<File>(), leftoverTempFiles(filesDir))
    }

    @Test
    fun stageCopy_thenCommit_replacesCutoutWithSourceBytes_andKeepsSource() {
        val filesDir = tempFolder.newFolder("files")
        val confirmedCutout = File(File(filesDir, "sticker_bgs/7"), "a.png")
        confirmedCutout.parentFile!!.mkdirs()
        confirmedCutout.writeBytes(byteArrayOf(1, 2, 3))
        val newCutout = File(tempFolder.newFolder("cache"), "new.png")
        newCutout.writeBytes(byteArrayOf(9, 9, 9, 9))

        val transaction = ConfirmedEditTransaction()
        assertTrue(transaction.stageCopy(newCutout, confirmedCutout))
        assertEquals(ConfirmedEditTransaction.CommitResult.COMMITTED, transaction.commit())

        assertEquals(sha256(newCutout), sha256(confirmedCutout))
        // 원본(캐시) 정리는 호출자가 커밋 확인 뒤에 한다. 트랜잭션은 지우지 않는다.
        assertTrue(newCutout.exists())
    }

    @Test
    fun stageCopy_missingSource_failsWithoutTouchingConfirmedCutout() {
        val filesDir = tempFolder.newFolder("files")
        val confirmedCutout = File(File(filesDir, "sticker_bgs/7"), "a.png")
        confirmedCutout.parentFile!!.mkdirs()
        confirmedCutout.writeBytes(byteArrayOf(1, 2, 3))
        val before = sha256(confirmedCutout)

        val transaction = ConfirmedEditTransaction()
        assertFalse(transaction.stageCopy(File(filesDir, "missing.png"), confirmedCutout))
        transaction.discard()

        assertEquals(before, sha256(confirmedCutout))
        assertEquals(emptyList<File>(), leftoverTempFiles(filesDir))
    }

    // ── 교체 단계 실패 ──

    @Test
    fun firstReplaceFailure_reportsNothingCommitted_andKeepsEveryFile() {
        val filesDir = tempFolder.newFolder("files")
        val files = confirmedFiles(filesDir)
        writeOldConfirmed(files)
        val before = hashes(files)

        val transaction = ConfirmedEditTransaction(replace = { _, _ -> false })
        files.forEachIndexed { index, file -> assertTrue(transaction.stageText(file, "new-$index")) }

        assertEquals(
            ConfirmedEditTransaction.CommitResult.NOTHING_COMMITTED,
            transaction.commit()
        )
        assertEquals(before, hashes(files))
        assertEquals(emptyList<File>(), leftoverTempFiles(filesDir))
    }

    @Test
    fun replaceFailureMidway_reportsPartiallyCommitted_soCallerKeepsDraft() {
        // 실제 파일시스템 실패: 세 번째 대상 자리에 비어 있지 않은 폴더가 있으면 교체(move)가 실패한다.
        val filesDir = tempFolder.newFolder("files")
        val files = confirmedFiles(filesDir)
        writeOldConfirmed(files)
        val transaction = ConfirmedEditTransaction()
        files.forEachIndexed { index, file -> assertTrue(transaction.stageText(file, "new-$index")) }
        assertTrue(files[2].delete())
        File(files[2], "occupied").apply { parentFile!!.mkdirs(); writeText("x") }

        assertEquals(
            ConfirmedEditTransaction.CommitResult.PARTIALLY_COMMITTED,
            transaction.commit()
        )
        // 앞 두 종류는 교체됐고 뒤 세 종류는 기존 그대로다(되돌리지 않음 — 알려진 한계).
        assertEquals("new-0", files[0].readText())
        assertEquals("new-1", files[1].readText())
        assertEquals("old-3", files[3].readText())
        assertEquals("old-5", files[5].readText())
        assertEquals(emptyList<File>(), leftoverTempFiles(filesDir))
    }

    @Test
    fun replaceThrowing_isTreatedAsFailure() {
        val filesDir = tempFolder.newFolder("files")
        val files = confirmedFiles(filesDir)
        writeOldConfirmed(files)
        val transaction =
            ConfirmedEditTransaction(replace = { _, _ -> throw SecurityException("denied") })
        files.forEachIndexed { index, file -> assertTrue(transaction.stageText(file, "new-$index")) }

        assertEquals(
            ConfirmedEditTransaction.CommitResult.NOTHING_COMMITTED,
            transaction.commit()
        )
        files.forEachIndexed { index, file -> assertEquals("old-$index", file.readText()) }
        assertEquals(emptyList<File>(), leftoverTempFiles(filesDir))
    }

    @Test
    fun stagingSameTargetTwice_commitsOnlyTheLastContent() {
        val filesDir = tempFolder.newFolder("files")
        val target = confirmedFiles(filesDir)[0]
        val transaction = ConfirmedEditTransaction()

        assertTrue(transaction.stageText(target, "first"))
        assertTrue(transaction.stageText(target, "second"))
        assertEquals(1, transaction.stagedCount)
        assertEquals(ConfirmedEditTransaction.CommitResult.COMMITTED, transaction.commit())

        assertEquals("second", target.readText())
    }
}
