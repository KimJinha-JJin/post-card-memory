package com.postcardmemory.utils

import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 78일차 P1-5: 이미지 파일은 만들어졌는데 DB 커밋이 실패·취소되면 아무도
 * 참조하지 않는 고아 파일이 남던 문제와, JPEG 쓰기 도중 실패하면 반쯤 쓰인
 * 파일이 남던 문제.
 *
 * 두 규칙 모두 production 함수([withProvisionalFile],
 * [ImageUtils.writeOrDeletePartialFile])를 그대로 호출해 검증한다.
 * finally / catch 의 정리를 지우면 전부 실패한다.
 */
class ProvisionalFileTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun producedFile(name: String): File =
        File(tempFolder.root, name).apply { writeText("cropped-jpeg-bytes") }

    // ── 커밋 성공/실패에 따른 소유권 ──

    @Test
    fun commitSucceeds_theProducedFileIsKept() = runBlocking {
        val file = producedFile("postcard_1.jpg")

        val result = withProvisionalFile(
            produce = { file },
            commit = { it.absolutePath }
        )

        assertEquals(file.absolutePath, result)
        assertTrue(file.exists())
    }

    @Test
    fun commitFails_theProducedFileIsDeletedAndTheFailureIsRethrown() = runBlocking {
        val file = producedFile("postcard_2.jpg")

        var thrown: Throwable? = null
        try {
            withProvisionalFile(
                produce = { file },
                commit = { throw IOException("DB insert 실패") }
            )
        } catch (error: IOException) {
            thrown = error
        }

        assertEquals("DB insert 실패", thrown?.message)
        assertFalse(file.exists())
    }

    @Test
    fun commitCancelled_theProducedFileIsStillDeleted() = runBlocking {
        val file = producedFile("postcard_3.jpg")

        runCatching {
            withProvisionalFile(
                produce = { file },
                commit = { throw CancellationException("화면 이탈") }
            )
        }

        assertFalse(file.exists())
    }

    @Test
    fun realCoroutineCancellationDuringCommit_stillDeletesTheProducedFile() = runBlocking {
        // 취소 예외를 손으로 던지는 게 아니라 실제로 job을 취소한다.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val file = producedFile("postcard_4.jpg")

        val enteredCommit = CompletableDeferred<Unit>()
        val job = scope.launch {
            withProvisionalFile(
                produce = { file },
                commit = {
                    enteredCommit.complete(Unit)
                    delay(10_000)
                }
            )
        }

        enteredCommit.await()
        assertTrue(file.exists())

        job.cancel()
        runCatching { job.join() }

        assertFalse(file.exists())
        scope.cancel()
    }

    @Test
    fun produceItselfFails_nothingIsDeletedBecauseNothingWasProduced() = runBlocking {
        val untouched = producedFile("postcard_5.jpg")

        runCatching {
            withProvisionalFile<Unit>(
                produce = { throw IOException("자르기 실패") },
                commit = { }
            )
        }

        // withProvisionalFile은 자기가 만든 파일만 지운다 — 남의 파일은 건드리지 않는다.
        assertTrue(untouched.exists())
    }

    // ── 부분 출력 파일 ──

    @Test
    fun compressReturningFalse_removesThePartialOutputFile() {
        val output = File(tempFolder.root, "partial_1.jpg")

        var thrown: Throwable? = null
        try {
            ImageUtils.writeOrDeletePartialFile(output) { stream ->
                stream.write(ByteArray(64))
                false
            }
        } catch (error: IllegalStateException) {
            thrown = error
        }

        assertEquals("정사각형 사진을 저장하지 못했습니다.", thrown?.message)
        assertFalse(output.exists())
    }

    @Test
    fun compressThrowing_removesThePartialOutputFile() {
        val output = File(tempFolder.root, "partial_2.jpg")

        var thrown: Throwable? = null
        try {
            ImageUtils.writeOrDeletePartialFile(output) { stream ->
                stream.write(ByteArray(64))
                throw IOException("저장 중 디스크 오류")
            }
        } catch (error: IOException) {
            thrown = error
        }

        assertEquals("저장 중 디스크 오류", thrown?.message)
        assertFalse(output.exists())
    }

    @Test
    fun successfulWrite_keepsTheOutputFileWithItsContents() {
        val output = File(tempFolder.root, "written.jpg")

        ImageUtils.writeOrDeletePartialFile(output) { stream: OutputStream ->
            stream.write("jpeg".toByteArray())
            true
        }

        assertTrue(output.exists())
        assertEquals("jpeg", output.readText())
    }

    @Test
    fun failedWriteOverAnExistingPath_doesNotLeaveTheHalfWrittenFileBehind() {
        val output = File(tempFolder.root, "reused.jpg")
        output.writeText("old")

        runCatching {
            ImageUtils.writeOrDeletePartialFile(output) { stream ->
                stream.write(ByteArray(8))
                false
            }
        }

        // FileOutputStream이 이미 기존 내용을 잘라낸 뒤라 되살릴 것이 없다.
        // 최소한 반쯤 쓰인 파일이 남지는 않아야 한다.
        assertFalse(output.exists())
    }
}
