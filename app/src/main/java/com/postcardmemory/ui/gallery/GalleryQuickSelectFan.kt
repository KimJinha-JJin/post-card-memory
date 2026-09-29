package com.postcardmemory.ui.gallery

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/*
 * 88일차: 갤러리 퀵 셀렉트(신문 오림 손 부채)의 순수 계산.
 *
 * 화면 오른쪽 밖의 가상 pivot 하나를 기준으로 다섯 손이 부채처럼 펼쳐진다.
 * 각 손은 PNG 안 사각형 블록의 중심이 pivot에서 [QUICK_SELECT_FAN_RADIUS_DP]
 * 떨어진 반지름 위에 오도록 놓이고, 손의 가로축이 그 반지름과 나란하도록
 * 같은 각도만큼 돈다(양수 = 시계 방향 = 손끝이 위로). 그래서 손목 잘린 면은
 * 항상 화면 오른쪽 밖에 남는다.
 *
 * Compose에 의존하지 않는 값만 둬서 JVM 테스트로 단계 전이·겹침을 검증한다.
 * 단위는 모두 dp(Float)다.
 */

/** 퀵 셀렉트 한 번의 열기/닫기 단계. 대규모 상태머신 없이 네 값만 둔다. */
internal enum class GalleryQuickSelectPhase { CLOSED, OPENING, OPEN, CLOSING }

/**
 * 열기/닫기 요청([open])이 들어왔을 때의 다음 단계. 이미 그 방향으로 가는
 * 중이거나 도착해 있으면 그대로 둔다 — 열기 연타·닫기 연타가 연출을 처음부터
 * 다시 시작시키지 않는다. 닫히는 중 열기(또는 그 반대)는 방향만 바꾼다.
 */
internal fun GalleryQuickSelectPhase.onRequest(open: Boolean): GalleryQuickSelectPhase =
    if (open) {
        when (this) {
            GalleryQuickSelectPhase.CLOSED, GalleryQuickSelectPhase.CLOSING -> GalleryQuickSelectPhase.OPENING
            else -> this
        }
    } else {
        when (this) {
            GalleryQuickSelectPhase.OPEN, GalleryQuickSelectPhase.OPENING -> GalleryQuickSelectPhase.CLOSING
            else -> this
        }
    }

/** 연출이 끝까지 재생된 뒤의 단계. */
internal fun GalleryQuickSelectPhase.onAnimationFinished(): GalleryQuickSelectPhase =
    when (this) {
        GalleryQuickSelectPhase.OPENING -> GalleryQuickSelectPhase.OPEN
        GalleryQuickSelectPhase.CLOSING -> GalleryQuickSelectPhase.CLOSED
        else -> this
    }

/** 기능은 완전히 펼쳐진 뒤에만 고를 수 있다 — 펼치는 중·닫히는 중 탭은 무시한다. */
internal val GalleryQuickSelectPhase.acceptsSelection: Boolean
    get() = this == GalleryQuickSelectPhase.OPEN

/**
 * 손 탭 한 번의 처리 결과(다음 단계, 기능 실행 여부). 받아들여지면 그 자리에서
 * CLOSING으로 넘어가므로, 부모가 닫힘을 반영하기 전에 들어온 두 번째 탭은
 * 실행되지 않는다 — 한 번의 탭에 기능 callback은 정확히 한 번.
 */
internal fun GalleryQuickSelectPhase.onSelect(): Pair<GalleryQuickSelectPhase, Boolean> =
    if (acceptsSelection) GalleryQuickSelectPhase.CLOSING to true else this to false

/**
 * 손 자산과 기존 기능의 1:1 매핑. 블록 좌표는 원본 PNG(1254×1254 px) 안에서
 * 실측한 사각형 종이 블록의 경계다. [slotFromBottom] 0이 맨 아래(엄지에 가장
 * 가까운 손잡이 쪽)이고, 부채는 그 위로 펼쳐진다.
 */
internal enum class GalleryQuickSelectItem(
    val slotFromBottom: Int,
    val blockLeftPx: Float,
    val blockTopPx: Float,
    val blockRightPx: Float,
    val blockBottomPx: Float
) {
    CAMERA(0, 243f, 405f, 692f, 718f),
    FUTURE_MAILBOX(1, 113f, 432f, 590f, 750f),
    POND(2, 243f, 403f, 692f, 713f),
    SHEEP_RANCH(3, 280f, 377f, 723f, 722f),
    RACE(4, 162f, 413f, 653f, 725f);

    val blockCenterXFraction: Float get() = (blockLeftPx + blockRightPx) / 2f / QUICK_SELECT_HAND_SOURCE_PX
    val blockCenterYFraction: Float get() = (blockTopPx + blockBottomPx) / 2f / QUICK_SELECT_HAND_SOURCE_PX
    val blockHalfWidthDp: Float get() = (blockRightPx - blockLeftPx) / 2f * QUICK_SELECT_DP_PER_SOURCE_PX
}

internal const val QUICK_SELECT_HAND_COUNT = 5
internal const val QUICK_SELECT_HAND_SOURCE_PX = 1254f

/** 손 PNG 한 장(정사각형)의 표시 크기. */
internal const val QUICK_SELECT_HAND_SIZE_DP = 180f
internal const val QUICK_SELECT_DP_PER_SOURCE_PX = QUICK_SELECT_HAND_SIZE_DP / QUICK_SELECT_HAND_SOURCE_PX

/** pivot ~ 블록 중심 거리. 멀수록 손끼리 각도 차이가 작아져 블록이 덜 기운다. */
internal const val QUICK_SELECT_FAN_RADIUS_DP = 440f

/**
 * 이웃한 두 손 사이 각도. 작은 화면에서는 최소값까지 줄인다 — 최소값은 이웃과
 * 겹치지 않는 터치 높이가 48dp 가까이 남는 한계다.
 */
internal const val QUICK_SELECT_MAX_SPACING_DEG = 9f
internal const val QUICK_SELECT_MIN_SPACING_DEG = 7.6f

/** 맨 아래 손의 블록 중심 ~ 컨테이너 바닥. */
internal const val QUICK_SELECT_BOTTOM_BLOCK_GAP_DP = 64f

/** 맨 위 손의 블록 중심 위로 남겨둘 여유(손가락·블록 윗부분). */
internal const val QUICK_SELECT_TOP_RESERVE_DP = 72f

/** 가운데 손(0°)의 블록 중심 ~ 컨테이너 오른쪽 끝. 손목 잘린 면이 화면 밖에 남는 거리. */
internal const val QUICK_SELECT_MIDDLE_BLOCK_INSET_DP = 92f

/** 터치 영역: 블록 위아래로 조금 넓힌 높이의 절반, 블록 좌우 여유. */
internal const val QUICK_SELECT_TOUCH_HALF_HEIGHT_DP = 24f
internal const val QUICK_SELECT_TOUCH_PAD_X_DP = 6f

/** 닫힌 묶음: pivot 쪽으로 물러난 거리, 묶음 안에서 손끼리 살짝 어긋난 각도, 크기. */
internal const val QUICK_SELECT_SLIDE_DP = 72f
internal const val QUICK_SELECT_CLOSED_BUNDLE_STEP_DEG = 1.2f
internal const val QUICK_SELECT_CLOSED_SCALE = 0.8f

/** 닫힌 묶음만 열린 부채의 맨 아래 손보다 더 내려앉는 거리(88일차 실기기 QA). */
internal const val QUICK_SELECT_CLOSED_DROP_DP = 24f

/** 펼치기 직전 묶음 전체가 아래로 젖혀지는 각도. */
internal const val QUICK_SELECT_TILT_DEG = 5f

/** 손별 펼침 시작의 어긋남(전체 펼침 진행도 대비). 세지 못할 만큼 짧게. */
internal const val QUICK_SELECT_STAGGER = 0.08f

internal data class QuickSelectPoint(val x: Float, val y: Float)

private fun Float.toRadians(): Float = (this * Math.PI / 180.0).toFloat()
private fun Float.toDegrees(): Float = (this * 180.0 / Math.PI).toFloat()

/**
 * 컨테이너 높이에 맞춘 이웃 손 사이 각도. 맨 아래~맨 위 블록 중심의 세로
 * 거리는 2R·sin(2Δ)이므로, 남는 높이에 들어가는 Δ를 구해 [MIN, MAX]로 자른다.
 */
internal fun quickSelectSpacingDegrees(containerHeightDp: Float): Float {
    val available = containerHeightDp - QUICK_SELECT_BOTTOM_BLOCK_GAP_DP - QUICK_SELECT_TOP_RESERVE_DP
    val ratio = (available / (2f * QUICK_SELECT_FAN_RADIUS_DP)).coerceIn(0f, 1f)
    val fit = asin(ratio).toDegrees() / 2f
    return fit.coerceIn(QUICK_SELECT_MIN_SPACING_DEG, QUICK_SELECT_MAX_SPACING_DEG)
}

/** 완전히 펼쳤을 때 [slotFromBottom] 손의 각도. 가운데(slot 2)가 0°. */
internal fun quickSelectSlotAngle(slotFromBottom: Int, spacingDeg: Float): Float =
    (slotFromBottom - (QUICK_SELECT_HAND_COUNT - 1) / 2f) * spacingDeg

/** 공통 pivot. 맨 아래 손 블록이 바닥에서 [QUICK_SELECT_BOTTOM_BLOCK_GAP_DP] 위에 온다. */
internal fun quickSelectPivot(containerWidthDp: Float, containerHeightDp: Float, spacingDeg: Float): QuickSelectPoint {
    val bottomAngle = quickSelectSlotAngle(0, spacingDeg).toRadians()
    return QuickSelectPoint(
        x = containerWidthDp - QUICK_SELECT_MIDDLE_BLOCK_INSET_DP + QUICK_SELECT_FAN_RADIUS_DP,
        y = containerHeightDp - QUICK_SELECT_BOTTOM_BLOCK_GAP_DP + QUICK_SELECT_FAN_RADIUS_DP * sin(bottomAngle)
    )
}

/** [angleDeg] 방향, pivot에서 [radiusDp] 떨어진 블록 중심. 양수 각도는 위쪽. */
internal fun quickSelectBlockCenter(pivot: QuickSelectPoint, radiusDp: Float, angleDeg: Float): QuickSelectPoint {
    val a = angleDeg.toRadians()
    return QuickSelectPoint(pivot.x - radiusDp * cos(a), pivot.y - radiusDp * sin(a))
}

/** 전체 펼침 진행도 [spread]에서 한 손의 펼침 진행도. 아래 손부터 아주 조금씩 먼저 움직인다. */
internal fun quickSelectHandSpread(spread: Float, slotFromBottom: Int): Float {
    val start = QUICK_SELECT_STAGGER * slotFromBottom
    val span = 1f - QUICK_SELECT_STAGGER * (QUICK_SELECT_HAND_COUNT - 1)
    return ((spread - start) / span).coerceIn(0f, 1f)
}

/**
 * 연출 중 한 손의 각도. 닫힌 묶음에서는 모두 맨 아래 각도 근처에 겹쳐 있다가
 * (살짝씩 어긋나 묶음으로 읽힘) 펼침 진행도에 따라 제 자리로 간다.
 * [tilt]는 펼치기 직전 묶음 전체가 아래로 젖혀지는 정도(0~1).
 */
internal fun quickSelectHandAngle(slotFromBottom: Int, spacingDeg: Float, spread: Float, tilt: Float): Float {
    val collapsed = quickSelectSlotAngle(0, spacingDeg) + slotFromBottom * QUICK_SELECT_CLOSED_BUNDLE_STEP_DEG
    val open = quickSelectSlotAngle(slotFromBottom, spacingDeg)
    val p = quickSelectHandSpread(spread, slotFromBottom)
    return collapsed + (open - collapsed) * p - QUICK_SELECT_TILT_DEG * tilt
}

/** 등장 진행도 [slide](0 = 닫힌 손잡이, 1 = 쑤욱 들어옴)에서의 반지름. */
internal fun quickSelectHandRadius(slide: Float): Float =
    QUICK_SELECT_FAN_RADIUS_DP - QUICK_SELECT_SLIDE_DP * (1f - slide)

/**
 * 연출 중 블록 중심. 반지름을 따라 쑤욱 들어오는 동안 닫힌 자리에서만 더
 * 내려가 있던 [QUICK_SELECT_CLOSED_DROP_DP]도 함께 거둔다.
 */
internal fun quickSelectAnimatedBlockCenter(pivot: QuickSelectPoint, slide: Float, angleDeg: Float): QuickSelectPoint {
    val center = quickSelectBlockCenter(pivot, quickSelectHandRadius(slide), angleDeg)
    return center.copy(y = center.y + QUICK_SELECT_CLOSED_DROP_DP * (1f - slide))
}

internal fun quickSelectHandScale(slide: Float): Float =
    QUICK_SELECT_CLOSED_SCALE + (1f - QUICK_SELECT_CLOSED_SCALE) * slide

/**
 * 이웃한 손의 터치 영역이 겹치지 않는 최대 반높이. 두 영역은 각도 Δ만큼
 * 벌어진 반지름 위에 있으므로, pivot에 가장 가까운 안쪽 끝(반지름 r_in)에서
 * h ≤ r_in·tan(Δ/2)면 어디서도 겹치지 않는다.
 */
internal fun quickSelectTouchHalfHeightDp(spacingDeg: Float): Float {
    val maxBlockHalfWidth = GalleryQuickSelectItem.entries.maxOf { it.blockHalfWidthDp }
    val innerRadius = QUICK_SELECT_FAN_RADIUS_DP - maxBlockHalfWidth - QUICK_SELECT_TOUCH_PAD_X_DP
    val limit = innerRadius * tan((spacingDeg / 2f).toRadians()) - 0.5f
    return min(QUICK_SELECT_TOUCH_HALF_HEIGHT_DP, limit)
}
