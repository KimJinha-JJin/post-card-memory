package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 127일차: 미래 우체통 종이의 구조 고정(소스 텍스트 검사 — 실제 렌더링·탭 동작은 보장하지 않는다).
 * 진행률을 쓰지 않고, 기존 우체통 이동 콜백만 쓰고, 달력 종이·봉투·원형 소인을 재사용하며,
 * 기억 밀도 그래프 아래 남은 칸에서 높이를 재 조건부로 그린다.
 */
class GalleryFutureMailDioramaStructureTest {

    private fun read(name: String): String {
        val candidates = listOf(
            "src/main/java/com/postcardmemory/ui/gallery/$name",
            "app/src/main/java/com/postcardmemory/ui/gallery/$name"
        )
        return candidates.map(::File).firstOrNull(File::exists)?.readText()
            ?: error("$name 을 찾을 수 없음(cwd=${File(".").absolutePath})")
    }

    private val diorama by lazy { read("GalleryFutureMailDiorama.kt") }
    private val screen by lazy { read("GalleryScreen.kt") }
    private val viewModel by lazy { read("GalleryViewModel.kt") }

    @Test
    fun diorama_doesNotUseProgressPercent() {
        val code = diorama.lines().filterNot {
            val t = it.trimStart()
            t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")
        }
        assertFalse(code.any { it.contains("progressPercent") })
    }

    @Test
    fun diorama_reusesCalendarPaperEnvelopeAndPostmark() {
        assertTrue(diorama.contains("R.drawable.visit_calendar_paper"))
        assertTrue(diorama.contains("R.drawable.visit_calendar_envelope_base"))
        assertTrue(diorama.contains("visitCalendarPaperEdgeOutline("))
        assertTrue(diorama.contains("drawVisitCalendarTape("))
        assertTrue(diorama.contains("VISIT_CALENDAR_PAPER_SHADOW_LAYERS"))
        assertTrue(diorama.contains("SealType.CIRCLE_POSTMARK"))
    }

    @Test
    fun diorama_isOneAccessibleButton() {
        assertTrue(diorama.contains("clearAndSetSemantics"))
        assertTrue(diorama.contains("role = Role.Button"))
        assertTrue(diorama.contains("onClick(label = \"미래 우체통 열기\")"))
    }

    @Test
    fun slot_measuresRemainingHeightAndHidesInsteadOfShrinking() {
        assertTrue(diorama.contains("BoxWithConstraints"))
        assertTrue(
            diorama.contains(
                "if (!futureMailDioramaFits(maxHeight.value, sheetHeight.value)) return@BoxWithConstraints"
            )
        )
        assertFalse(diorama.contains("verticalScroll"))
    }

    @Test
    fun densityPage_placesSlotAfterGraphAndUsesExistingMailboxNavigation() {
        val start = screen.indexOf("private fun GalleryDensityPage(")
        val end = screen.indexOf("private fun GalleryMemoryDensityBar(", start)
        assertTrue(start >= 0 && end > start)
        val body = screen.substring(start, end)
        val foot = body.indexOf("GalleryMemoryDensityFoot(")
        val slot = body.indexOf("GalleryFutureMailDioramaSlot(")
        assertTrue(foot >= 0 && slot > foot)
        assertTrue(body.substring(slot).contains(".weight(1f)"))
        assertEquals(1, Regex("""onOpenFutureMailbox = onNavigateToFutureMailbox""").findAll(screen).count())
    }

    @Test
    fun viewModel_buildsGroupsLikeMailboxAndStartsAsUnknown() {
        assertTrue(viewModel.contains("repository.getFutureMailPostcards()"))
        assertTrue(viewModel.contains("dayBoundaryTicks()"))
        assertTrue(viewModel.contains("buildFutureMailGroups("))
        assertTrue(viewModel.contains("val futureMailGroups: StateFlow<List<FutureMailGroup>?>"))
        assertTrue(viewModel.contains("initialValue = null"))
    }
}
