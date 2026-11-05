package com.postcardmemory.utils

import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 공유와 갤러리 저장이 함께 쓰는 "이미지 생성 → 출력 → 해제" 순서를 실제 실행으로 검증한다.
 * Bitmap은 JVM에서 만들 수 없어 문자열로 대신하며, 실제 recycle 여부는 검증하지 않는다.
 */
class PostcardFrontRenderLifecycleTest {

    private val events = mutableListOf<String>()

    private fun <T> run(
        render: () -> String = { events += "render"; "image" },
        output: (String) -> T
    ): Result<T> =
        PostcardImageExporter.renderThenOutput(
            render = render,
            release = { image -> events += "release:$image" },
            output = output
        )

    @Test
    fun success_outputsThenReleasesOnce_andReturnsOutputResult() {
        val result = run { image ->
            events += "output:$image"
            "saved"
        }

        assertEquals("saved", result.getOrThrow())
        assertEquals(listOf("render", "output:image", "release:image"), events)
    }

    @Test
    fun outputFailure_stillReleasesOnce_andKeepsOriginalError() {
        val failure = IOException("공유용 이미지 파일 저장에 실패했습니다.")

        val result = run<String> { throw failure }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf("render", "release:image"), events)
    }

    @Test
    fun renderFailure_neitherOutputsNorReleases_andKeepsOriginalError() {
        val failure = IOException("원본 사진 파일을 찾지 못했습니다.")
        var outputCalled = false

        val result = run(
            render = { throw failure },
            output = { outputCalled = true }
        )

        assertSame(failure, result.exceptionOrNull())
        assertTrue(!outputCalled)
        assertTrue(events.isEmpty())
    }

    @Test
    fun outOfMemoryWhileOutputting_isReturnedAsFailureAfterRelease() {
        val failure = OutOfMemoryError("test")

        val result = run<String> { throw failure }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf("render", "release:image"), events)
    }

    /** 기존 runCatching과 같이 취소도 Result 실패로 돌려준다(리팩터링으로 바꾸지 않은 동작). */
    @Test
    fun cancellationWhileOutputting_isReturnedAsFailureAfterRelease() {
        val failure = CancellationException("test")

        val result = run<String> { throw failure }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf("render", "release:image"), events)
    }

    @Test
    fun releaseFailure_afterSuccessfulOutput_isReturnedAsFailure() {
        val failure = IllegalStateException("release")

        val result = PostcardImageExporter.renderThenOutput(
            render = { "image" },
            release = { throw failure },
            output = { "saved" }
        )

        assertSame(failure, result.exceptionOrNull())
    }
}
