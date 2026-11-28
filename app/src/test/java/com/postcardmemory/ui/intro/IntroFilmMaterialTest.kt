package com.postcardmemory.ui.intro

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 146일차 B안 필름 재질. 승인 목업과 같은 고정 시드 값을 내는지, 질감을 프레임마다 다시 만들지 않는지,
 * 천공을 실제로 뚫는지, 새 이미지·사진에 기대지 않는지를 지킨다. 시각적 미감은 실기기 QA 몫이다.
 */
class IntroFilmMaterialTest {

    private fun sourceOf(relativePath: String): String {
        val candidates = listOf("src/$relativePath", "app/src/$relativePath")
        return candidates.map(::File).firstOrNull(File::exists)?.readText()
            ?: error("$relativePath 를 찾을 수 없음(cwd=${File(".").absolutePath})")
    }

    private val materialSource: String by lazy {
        sourceOf("main/java/com/postcardmemory/ui/intro/IntroFilmMaterial.kt")
    }

    private val introSource: String by lazy {
        sourceOf("main/java/com/postcardmemory/ui/intro/AppIntroScreen.kt")
    }

    @Test
    fun random_matchesApprovedMockupMulberry32() {
        // docs/ai/mockups/intro-film-texture-day146.html의 mulberry32(1000)·mulberry32(23) 첫 값들.
        val frame = IntroFilmRandom(introFilmFrameSeed(0))
        assertEquals(0.7951949f, frame.next(), 1e-6f)
        assertEquals(0.8276879f, frame.next(), 1e-6f)
        assertEquals(0.6915161f, frame.next(), 1e-6f)

        val grain = IntroFilmRandom(INTRO_FILM_BASE_GRAIN_SEED)
        assertEquals(0.0927227f, grain.next(), 1e-6f)
        assertEquals(0.0683119f, grain.next(), 1e-6f)
    }

    @Test
    fun random_isDeterministicPerFrameAndDiffersBetweenFrames() {
        val a = List(4) { IntroFilmRandom(introFilmFrameSeed(3)) }.map { it.next() }
        assertTrue("같은 칸은 언제나 같은 노출 흔적", a.distinct().size == 1)
        assertTrue(
            "이웃 칸은 다른 노출 흔적",
            IntroFilmRandom(introFilmFrameSeed(3)).next() != IntroFilmRandom(introFilmFrameSeed(4)).next()
        )
    }

    @Test
    fun material_isBuiltOnceInCacheNotEveryFrame() {
        val drawStart = materialSource.indexOf("onDrawBehind {")
        assertTrue("drawWithCache로 정적 Path·Brush를 캐시해야 함", materialSource.contains("drawWithCache {"))
        assertTrue(drawStart > 0)
        val drawBlock = materialSource.substring(drawStart)
        assertFalse("그리기 단계에서 난수를 만들지 않음", drawBlock.contains("IntroFilmRandom("))
        assertFalse("그리기 단계에서 그레인 타일을 만들지 않음", drawBlock.contains("introFilmGrainTile("))
        assertFalse("그리기 단계에서 Path를 새로 만들지 않음", drawBlock.contains("Path()"))
        assertTrue("그레인 타일은 composition당 한 번", introSource.contains("remember { introFilmGrain() }"))
    }

    @Test
    fun perforations_areCutThroughTheFilmNotPainted() {
        assertTrue("바탕에서 원을 빼서 실제로 뚫어야 함", materialSource.contains("fillType = PathFillType.EvenOdd"))
        assertFalse("예전처럼 천공을 종이색 점으로 그리지 않음", introSource.contains("drawCircle(PaperTray"))
    }

    @Test
    fun material_needsNoNewImageOrUserPhoto() {
        listOf("painterResource", "imageResource", "BitmapFactory", "R.drawable", "Uri", "contentResolver").forEach {
            assertFalse("필름 재질은 이미지 자원·사용자 사진에 기대지 않음: $it", materialSource.contains(it))
        }
    }

    @Test
    fun dateInk_isOnlyTheApprovedDarkerTone_andStampInkIsUnchanged() {
        assertTrue("사용자 승인 날짜 잉크", introSource.contains("IntroFilmDateInk = Color(0xFF46392E)"))
        assertTrue("필름 날짜만 새 잉크", introSource.contains("color = IntroFilmDateInk,"))
        assertTrue("날짜 크기 유지", introSource.contains("fontSize = 9.5.sp"))
        assertFalse(
            "소인 색은 바꾸지 않음",
            Regex("SealPreviewContent\\([^)]*IntroFilmDateInk").containsMatchIn(introSource)
        )
    }

    @Test
    fun material_followsFilmCoordinates() {
        assertTrue(
            "칸 재질은 이송되는 Row 안의 칸에 붙어야 함",
            introSource.contains(".introFilmFrameMaterial(") &&
                introSource.contains("plan.frames.forEachIndexed { index, frame ->")
        )
        assertTrue(
            "그레인은 칸 번호만큼 밀어 필름 전체에서 이어져야 함",
            materialSource.contains("postTranslate(-index * w, 0f)")
        )
    }
}
