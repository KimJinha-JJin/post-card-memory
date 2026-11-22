package com.postcardmemory.ui.camera

import kotlin.math.pow

/**
 * 138일차 채택 C안(경쾌한 손맛형) 우표 펀칭의 시간표와 한 시각의 물리 상태.
 * 장면·높이·계산 구조는 `docs/ai/mockups/stamp-punch-motion-mockup.html`의 `CANDS` C안·`stateAt`을
 * 그대로 옮겼다. 140일차 실기기 확인 뒤 사용자 요청으로 시간만 1.9초 → 2.8초로 늘리고
 * 하강(접근할수록 감속)·상승(천천히 떨어져 빠져나감) 곡선을 바꿨다.
 * 단위: 시간 ms, 높이는 목업 뷰박스(140×110) 단위.
 */
internal object StampPunchTimeline {
    const val TOTAL_MS = 2_800
    const val DESCENT_START_MS = 120
    const val CONTACT_MS = 950
    const val CUT_MS = 1_050
    const val PRESS_END_MS = 1_180
    const val RISE_END_MS = 1_800
    const val LIFT_START_MS = 1_900
    const val LIFT_END_MS = 2_350
    const val SETTLE_END_MS = 2_800

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
private fun outCubic(p: Float) = 1f - (1f - p).pow(3)

/** 천천히 출발해 중간에 빨라지고 끝에서 다시 느려진다(하강은 종이에 묵직하게 닿고, 상승은 천천히 떨어져 빠져나감). */
private fun inOutCubic(p: Float) = if (p < 0.5f) 4f * p * p * p else 1f - (-2f * p + 2f).pow(3) / 2f

internal fun stampPunchStateAt(timeMs: Float): StampPunchState {
    val t = timeMs
    with(StampPunchTimeline) {
        val dieBottom = when {
            t < DESCENT_START_MS -> DIE_START_HEIGHT
            t < CONTACT_MS -> lerp(DIE_START_HEIGHT, PAPER_TOP, inOutCubic(prog(t, DESCENT_START_MS, CONTACT_MS)))
            t < PRESS_END_MS -> PAPER_TOP - BITE * outCubic(prog(t, CONTACT_MS, maxOf(CUT_MS, CONTACT_MS + 40)))
            else -> lerp(PAPER_TOP - BITE, DIE_EXIT_HEIGHT, inOutCubic(prog(t, PRESS_END_MS, RISE_END_MS)))
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
