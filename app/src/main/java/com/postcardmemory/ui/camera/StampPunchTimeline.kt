package com.postcardmemory.ui.camera

import kotlin.math.pow

/**
 * 138일차 채택 C안(경쾌한 손맛형) 우표 펀칭의 시간표와 한 시각의 물리 상태.
 * 값과 계산은 `docs/ai/mockups/stamp-punch-motion-mockup.html`의 `CANDS` C안·`stateAt`을
 * 그대로 옮겼다. 단위: 시간 ms, 높이는 목업 뷰박스(140×110) 단위.
 */
internal object StampPunchTimeline {
    const val TOTAL_MS = 1_900
    const val DESCENT_START_MS = 250
    const val CONTACT_MS = 540
    const val CUT_MS = 560
    const val PRESS_END_MS = 600
    const val RISE_END_MS = 860
    const val LIFT_START_MS = 1_060
    const val LIFT_END_MS = 1_360
    const val SETTLE_END_MS = 1_660

    const val DIE_START_HEIGHT = 60f
    const val DIE_EXIT_HEIGHT = 170f
    const val PAPER_TOP = 0.9f
    const val BITE = 0.25f
    const val LIFT_HEIGHT = 5f

    /** 움직임 줄이기: 보간 없이 끝 장면(잘려 분리된 우표가 들린 상태)만 잠깐 보여 준다. */
    const val REDUCED_MOTION_HOLD_MS = 600
}

internal enum class StampPunchPhase { WAIT, DESCENT, PRESS, RISE, SEPARATED, LIFT, SETTLE, END }

internal data class StampPunchState(
    /** 펀칭기 날 아래면 높이. 바닥판 0, 종이 윗면 [StampPunchTimeline.PAPER_TOP]. */
    val dieBottom: Float,
    /** 절단 이후면 종이에 구멍이 나고 조각이 따로 존재한다. 절단 전에는 종이 한 장뿐이다. */
    val cut: Boolean,
    /** 잘린 조각이 종이 윗면에서 들린 높이. */
    val lift: Float,
    /** 절단선(구멍과 조각 사이 틈) 진하기 0..1. 들림이 시작되면 사라진다. */
    val seam: Float,
    val phase: StampPunchPhase
)

private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v
private fun prog(t: Float, a: Int, b: Int) = clamp01((t - a) / (b - a).toFloat())
private fun lerp(a: Float, b: Float, p: Float) = a + (b - a) * p
private fun inQuad(p: Float) = p * p
private fun outCubic(p: Float) = 1f - (1f - p).pow(3)
private fun outQuart(p: Float) = 1f - (1f - p).pow(4)

internal fun stampPunchStateAt(timeMs: Float): StampPunchState {
    val t = timeMs
    with(StampPunchTimeline) {
        val dieBottom = when {
            t < DESCENT_START_MS -> DIE_START_HEIGHT
            t < CONTACT_MS -> lerp(DIE_START_HEIGHT, PAPER_TOP, inQuad(prog(t, DESCENT_START_MS, CONTACT_MS)))
            t < PRESS_END_MS -> PAPER_TOP - BITE * outCubic(prog(t, CONTACT_MS, maxOf(CUT_MS, CONTACT_MS + 40)))
            else -> lerp(PAPER_TOP - BITE, DIE_EXIT_HEIGHT, outQuart(prog(t, PRESS_END_MS, RISE_END_MS)))
        }
        val cut = t >= CUT_MS
        val lift = if (cut) LIFT_HEIGHT * outCubic(prog(t, LIFT_START_MS, LIFT_END_MS)) else 0f
        val seamEnd = LIFT_START_MS + ((LIFT_END_MS - LIFT_START_MS) * 0.35f).toInt()
        val seam = if (cut) 1f - prog(t, LIFT_START_MS, seamEnd) else 0f
        val phase = when {
            t < DESCENT_START_MS -> StampPunchPhase.WAIT
            t < CONTACT_MS -> StampPunchPhase.DESCENT
            t < PRESS_END_MS -> StampPunchPhase.PRESS
            t < RISE_END_MS -> StampPunchPhase.RISE
            t < LIFT_START_MS -> StampPunchPhase.SEPARATED
            t < LIFT_END_MS -> StampPunchPhase.LIFT
            t < SETTLE_END_MS -> StampPunchPhase.SETTLE
            else -> StampPunchPhase.END
        }
        return StampPunchState(dieBottom, cut, lift, seam, phase)
    }
}

/**
 * 저장 성공 뒤 갤러리 복귀를 한 번만 허용하는 문. 화면 재구성·회전·애니메이션 종료 콜백이
 * 여러 번 와도 popBackStack이 두 번 불리면 갤러리까지 빠져 버리므로 첫 요청만 통과시킨다.
 */
internal class SuccessExitGate {
    private var claimedPath: String? = null

    /** [imagePath] 저장 성공에 대해 처음 한 번만 true. 성공 상태가 아니면 항상 false. */
    fun tryClaim(state: CaptureState): Boolean {
        val success = state as? CaptureState.Success ?: return false
        if (claimedPath == success.imagePath) return false
        claimedPath = success.imagePath
        return true
    }
}
