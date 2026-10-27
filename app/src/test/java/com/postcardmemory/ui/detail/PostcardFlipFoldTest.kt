package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import com.postcardmemory.testsupport.readStructureTestSource
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 엽서 앞↔뒤 전환의 종이 날개 기하와 연결 구조. 실제 Compose 렌더링·감각은
 * JVM에서 확인할 수 없어 실기기 QA 대상이다.
 */
class PostcardFlipFoldTest {

    private val side = 300f
    // DetailScreen.cornerFoldCrease와 같은 방향: 오른쪽 아래 → 왼쪽 위 대각선.
    private val normal = Offset(-1f / sqrt(2f), -1f / sqrt(2f))

    private fun creaseAt(progress: Float) =
        Offset(side, side) + Offset(-side, -side) * progress

    private fun assertNear(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.01f)
        assertEquals(expected.y, actual.y, 0.01f)
    }

    private val detailScreenText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
            )
        )
    }

    @Test
    fun creaseLineStaysPutAndReflectingTwiceReturnsTheSamePoint() {
        val crease = creaseAt(0.3f)
        val along = Offset(-normal.y, normal.x) * 50f
        assertNear(crease + along, reflectAcrossCrease(crease + along, crease, normal))
        listOf(Offset(0f, 0f), Offset(side, side), Offset(120f, 260f)).forEach { point ->
            val once = reflectAcrossCrease(point, crease, normal)
            assertNear(point, reflectAcrossCrease(once, crease, normal))
        }
    }

    @Test
    fun drawingMatrixMatchesTheGeometricReflection() {
        val crease = creaseAt(0.62f)
        val matrix = Matrix().apply { setCreaseReflection(crease, normal) }
        listOf(Offset(0f, 0f), Offset(side, 0f), Offset(side, side), Offset(40f, 210f)).forEach {
            assertNear(reflectAcrossCrease(it, crease, normal), matrix.map(it))
        }
    }

    @Test
    fun liftedCornerLandsOnTheRemainingSideInsideTheCardDuringTheFirstHalf() {
        val progress = 0.25f
        val crease = creaseAt(progress)
        val corner = reflectAcrossCrease(Offset(side, side), crease, normal)
        // 접혀 올라간 모서리 끝은 남은 면 쪽(접힘선보다 왼쪽 위)에, 엽서 안에 놓인다.
        assertTrue(corner.x + corner.y < crease.x + crease.y)
        assertTrue(corner.x in 0f..side && corner.y in 0f..side)
    }

    @Test
    fun bothFacesDrawTheFlapWithTheOppositeSidePaperOnlyWhileTheyAreLeaving() {
        val source = detailScreenText
        assertEquals(2, Regex("drawCornerFoldFlap\\(\\s*revealProgress =").findAll(source).count())
        assertTrue(
            source.contains(
                "if (isBackFace) {\n" +
                    "                                        // 앞면이 넘어가는 중: 날개에 보이는 건 뒷면 종이.\n" +
                    "                                        drawCornerFoldFlap(\n" +
                    "                                            revealProgress = legProgress,\n" +
                    "                                            flapPaperColor = PaperSurface,"
            )
        )
        assertTrue(
            source.contains(
                "if (!isBackFace) {\n" +
                    "                                        // 뒷면이 넘어가는 중: 날개에 보이는 건 앞면 종이색.\n" +
                    "                                        drawCornerFoldFlap(\n" +
                    "                                            revealProgress = legProgress,\n" +
                    "                                            flapPaperColor = Color(pc.backgroundColorArgb),"
            )
        )
        // 비트맵 캡처 없이 live 콘텐츠를 반사 replay한다.
        val flapBody = source.substringAfter("private fun ContentDrawScope.drawCornerFoldFlap(")
            .substringBefore("\n}\n")
        assertTrue(flapBody.contains("withTransform({ transform(reflection) })"))
        assertTrue(flapBody.contains("this@drawCornerFoldFlap.drawContent()"))
        assertTrue(flapBody.contains("clipRect(0f, 0f, size.width, size.height)"))
    }

    @Test
    fun flipUsesTheSharedDurationAndStillIgnoresInputWhileAnimating() {
        val source = detailScreenText
        assertTrue(source.contains("durationMillis = POSTCARD_FLIP_DURATION_MS"))
        assertTrue(source.contains("targetRotation at POSTCARD_FLIP_DURATION_MS"))
        assertTrue(
            source.contains(
                "val triggerFlip: () -> Unit = {\n        if (!isFlipAnimating) {"
            )
        )
        assertTrue(source.contains("onClick = triggerFlip,\n                    enabled = controlsEnabled && !isFlipAnimating"))
        assertTrue(POSTCARD_FLIP_DURATION_MS in 650 until 1100)
        // 115일차 QA: 날개가 생긴 뒤 정지 화면의 dog-ear 힌트는 불필요해져 제거했다.
        assertFalse(source.contains("drawIdleCornerFoldHint"))
    }
}
