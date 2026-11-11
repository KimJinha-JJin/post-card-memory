package com.postcardmemory.ui.futuremail

import com.postcardmemory.data.FUTURE_MAIL_STATE_SENT
import com.postcardmemory.data.Postcard
import com.postcardmemory.testsupport.readStructureTestSource
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 129일차 A안(우편 서류함) 미래 우체통 화면.
 *
 * 앞 절반은 날짜 문구 계산(순수 로직), 뒤 절반은 소스 텍스트 구조 검사다 — 구조 검사는
 * 실제 렌더링·탭 동작·미감을 보장하지 않으며 그건 실기기 QA 몫이다.
 */
class FutureMailboxPaperScreenTest {

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    private fun millisOf(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun sent(id: Long, deliverAt: Long, capturedAt: Long) = Postcard(
        id = id,
        imagePath = "/$id.jpg",
        title = "postcard-$id",
        capturedAt = capturedAt,
        futureMailState = FUTURE_MAIL_STATE_SENT,
        futureMailDeliverAt = deliverAt
    )

    private fun groupsAt(now: Long, vararg deliverDays: Long): List<FutureMailGroup> =
        buildFutureMailGroups(
            sentPostcards = deliverDays.mapIndexed { i, d ->
                sent(i + 1L, d, capturedAt = millisOf(2026, 11, 1))
            },
            nowMillis = now,
            zone = zone
        )

    // ── 날짜 문구: 디오라마와 같은 자연어, 올해 판정은 그룹의 오늘 기준 ──

    @Test
    fun sameYear_dropsTheYear() {
        val group = groupsAt(millisOf(2026, 11, 11, 15), millisOf(2026, 12, 24)).single()
        assertEquals("12월 24일", futureMailboxDateText(group, zone))
    }

    @Test
    fun otherYear_keepsTheYear() {
        val group = groupsAt(millisOf(2026, 11, 11, 15), millisOf(2027, 1, 1)).single()
        assertEquals("2027년 1월 1일", futureMailboxDateText(group, zone))
    }

    @Test
    fun arrivedLastYear_keepsTheYear_evenThoughDaysLeftIsNegative() {
        // 2026-12-30에 도착했고 오늘은 2027-01-02 — daysLeft = -3, 도착일은 작년.
        val group = groupsAt(millisOf(2027, 1, 2, 9), millisOf(2026, 12, 30)).single()
        assertTrue(group.arrived)
        assertEquals(-3L, group.daysLeft)
        assertEquals("2026년 12월 30일", futureMailboxDateText(group, zone))
    }

    @Test
    fun newYearsEve_nextDayDeliveryInNewYear_keepsTheYear() {
        val group = groupsAt(millisOf(2026, 12, 31, 23), millisOf(2027, 1, 1)).single()
        assertEquals(1L, group.daysLeft)
        assertEquals("2027년 1월 1일", futureMailboxDateText(group, zone))
    }

    // ── 구조: A안 문법과 기존 기능 보존 ──

    private val screen by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/futuremail/FutureMailboxScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/futuremail/FutureMailboxScreen.kt"
            )
        )
    }

    private val code by lazy {
        screen.lines().filterNot {
            val t = it.trimStart()
            t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")
        }.joinToString("\n")
    }

    @Test
    fun noEmojiAndNoMaterialGoldButton() {
        listOf("📮", "💌", "⛵", "\\u26F5").forEach { assertFalse("이모지 $it 가 남아 있음", screen.contains(it)) }
        // Material 채움 버튼(Button(...))은 없어야 한다. 뒤로가기 IconButton은 그대로.
        assertFalse(Regex("""(?<![A-Za-z])Button\(""").containsMatchIn(code))
        assertFalse(code.contains("import androidx.compose.material3.Button\n"))
        assertFalse(code.contains("SunsetGold"))
        assertTrue(code.contains("text = \"미래 우체통\""))
    }

    @Test
    fun progressIsNeverShownAsANumber_andUnitIsTong() {
        assertFalse(code.contains("progressPercent}%"))
        assertFalse(code.contains("퍼센트"))
        assertFalse(code.contains("건\""))
        assertFalse(code.contains("}건"))
        assertTrue(code.contains("}통"))
    }

    @Test
    fun keepsGroupOpeningAndToast_andOnlyArrivedRowsCanOpen() {
        assertEquals(1, Regex("""viewModel\.openArrivedGroup\(group\)""").findAll(code).count())
        assertTrue(code.contains("viewModel.openedMessages.collect"))
        assertTrue(code.contains("IconButton(onClick = onNavigateBack)"))
        val arrivedStart = code.indexOf("item(key = \"arrived\")")
        val travelingStart = code.indexOf("item(key = \"traveling\")")
        assertTrue(arrivedStart in 0 until travelingStart)
        assertTrue(code.substring(arrivedStart, travelingStart).contains("openArrivedGroup"))
        assertFalse(code.substring(travelingStart).contains("openArrivedGroup"))
    }

    @Test
    fun reusesCalendarPaperEnvelopePostmarkAndPeekCard_withSampledPaperDecode() {
        assertTrue(code.contains("R.drawable.visit_calendar_envelope_base"))
        assertTrue(code.contains("calendarPaperSheet("))
        assertTrue(code.contains("SealType.CIRCLE_POSTMARK"))
        assertTrue(code.contains("PeekingPostcard("))
        // 종이는 원본 해상도로 decode하지 않는다(줄여 불러오고, 화면 단위로 한 번만).
        assertEquals(1, Regex("""R\.drawable\.visit_calendar_paper\b""").findAll(code).count())
        assertTrue(code.contains("inSampleSize = FUTURE_MAILBOX_PAPER_SAMPLE"))
        assertFalse(code.contains("imageResource("))
    }

    @Test
    fun inkButtonKeeps48dpTouchTarget_andDdayIsTheMintAccent() {
        assertTrue(code.contains(".heightIn(min = 48.dp)"))
        assertTrue(code.contains(".widthIn(min = 48.dp)"))
        assertTrue(code.contains("text = \"D-\$daysLeft\""))
        assertTrue(code.contains("color = VisitFillColorToday"))
    }

    @Test
    fun emptyStateGuidesToSendingWithoutAddingASendButton() {
        assertTrue(code.contains("아직 떠난 엽서가 없어"))
        assertTrue(code.contains("엽서 편집 ⋮ › 미래 우체통"))
        val empty = code.substring(code.indexOf("private fun FutureMailboxEmptySheet("))
            .substringBefore("private fun Modifier.dashedTopDivider(")
        assertFalse(empty.contains("clickable"))
    }

    @Test
    fun noBDecorations_slotShelfOrWallGradient() {
        assertFalse(code.contains("verticalGradient"))
        assertFalse(code.contains("Brush."))
        assertFalse(code.contains("투입구"))
        assertFalse(code.contains("선반"))
    }
}
