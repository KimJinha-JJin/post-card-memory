package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** 연결 구조 검사다. 실제 lifecycle·프레임·TalkBack 동작 검증과 구분한다. */
class MemoryScaleWiringStructureTest {
    private fun source(name: String): String = listOf("src/main/java", "app/src/main/java")
        .map { File("$it/com/postcardmemory/ui/gallery/$name.kt") }.first(File::exists).readText()
    private val page get() = source("GalleryMemoryScalePage")

    @Test fun sourceAndPeriodSelectionUseOrdinaryGalleryFlowWithoutMockControls() {
        val screen = source("GalleryScreen")
        assertTrue(screen.contains("GalleryMemoryScalePage("))
        assertTrue(screen.contains("postcards = displayedPostcards"))
        assertTrue(page.contains("memoryScalePostcards(postcards, period, date, zone)"))
        assertTrue(page.contains("mutableStateOf(MemoryScalePeriod.ALL.name)"))
        assertFalse(page.contains("Slider(")); assertFalse(page.contains("OutlinedTextField("))
        assertFalse(page.contains("ImageBitmap.imageResource"))
    }
    @Test fun animationKeyIsActualTargetAndVisibilityLifecycleInsteadOfComposition() {
        assertTrue(page.contains("measured.map { it.id }.toSet()"))
        assertTrue(page.contains("LaunchedEffect(measurementKey, active, reducedMotion)"))
        assertTrue(page.contains("val active = isPageVisible && resumed"))
        assertTrue(page.contains("Lifecycle.State.RESUMED"))
        assertTrue(page.contains("if (!active || reducedMotion || count == 0)"))
        assertTrue(source("GalleryScreen").contains("isPageVisible = pagerState.currentPage == page"))
        assertTrue(page.contains("lifecycle.removeObserver(observer)"))
        assertTrue(page.contains("resolver.unregisterContentObserver(settingsObserver)"))
    }
    @Test fun drawingReadsFramesOnlyInDrawAndSharesTrayTransformWithPile() {
        val drawing = source("MemoryScaleDrawing")
        assertTrue(drawing.contains("drawWithCache"))
        assertTrue(drawing.indexOf("onDrawBehind {") < drawing.indexOf("memoryScaleFrame(count, elapsedMs())"))
        val tray = drawing.substring(drawing.indexOf("translate(top = frame.trayY)"), drawing.indexOf("drawCircle(paper, 85f"))
        assertTrue(tray.contains("drawPath(tray, trayBrush)")); assertTrue(tray.contains("cards(frame.layers)"))
        assertTrue(drawing.contains("61f - frame.trayY"))
        assertTrue(drawing.contains("rotate(frame.needleDegrees, pivot = Offset(220f, 269f))"))
        assertTrue(drawing.contains("count.coerceIn(0, 12)"))
        assertFalse(drawing.contains("AsyncImage")); assertFalse(drawing.contains("Bitmap.createBitmap"))
        // 질감용 작은 알갱이 그림은 composition당 한 번만 만들고, 프레임(draw) 단계에서는 만들지 않는다.
        assertTrue(drawing.contains("val speckle = remember { memoryScaleSpeckleTile() }"))
        val perFrame = drawing.substring(drawing.indexOf("onDrawBehind {"))
        assertFalse(perFrame.contains("ImageBitmap(")); assertFalse(perFrame.contains("memoryScaleSpeckleTile()"))
        // 145일차 사용자 결정: 저울 하단 영어 문구는 표시하지 않는다.
        assertFalse(drawing.contains("M E M O R Y")); assertFalse(drawing.contains("POST OFFICE"))
    }
    @Test fun resultHasActualCountAndDecorativeTicksAreNotIndividualSemantics() {
        assertTrue(page.contains("contentDescription = \"기억 저울, 엽서 \${count}장, 최대 눈금 \${capacity}장\""))
        assertTrue(page.contains("Settings.Global.ANIMATOR_DURATION_SCALE"))
        assertTrue(page.contains("override fun onChange(selfChange: Boolean)"))
        assertTrue(source("MemoryScaleDrawing").contains("clearAndSetSemantics { }"))
    }
    @Test fun monthlyAggregationSurvivesWithoutGraphAndSavedPageIdentifierIsCompatible() {
        assertFalse(page.contains("GalleryPostcardYearChart"))
        assertFalse(source("GalleryScreen").contains("GalleryMemoryDensityBar"))
        assertFalse(source("GalleryScreen").contains("MEMORY_DENSITY_"))
        assertTrue(source("MemoryScaleModel").contains("postcardMonthsForYear(ordinary, date.year, zone)"))
        assertTrue(source("MemoryScaleDrawing").contains("aspectRatio(440f / 325f)"))
        assertTrue(source("GalleryPageFormat").contains("DENSITY"))
        assertTrue(source("GalleryScreen").contains("save = { it.name }"))
        assertTrue(source("GalleryScreen").contains("GalleryPageFormat.valueOf(saved)"))
    }
}
