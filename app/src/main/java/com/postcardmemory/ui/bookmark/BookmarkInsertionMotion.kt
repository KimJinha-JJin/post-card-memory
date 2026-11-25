package com.postcardmemory.ui.bookmark

import androidx.compose.animation.core.CubicBezierEasing

/** 엽서가 들리고(0~180) → 책갈피가 뒤로 들어가고(180~880) → 엽서가 내려앉아(710~930). */
internal const val BOOKMARK_INSERT_TOTAL_MS = 930
private const val LIFT_END_MS = 180f
private const val SLIDE_MS = 700f
private const val SETTLE_START_MS = 710f

private val LiftEasing = CubicBezierEasing(.2f, 0f, .2f, 1f)
private val SlideEasing = CubicBezierEasing(.3f, 0f, .2f, 1f)

/** [lift] 0 = 바닥, 1 = 다 들림. [slide] 0 = 엽서 옆, 1 = 엽서 뒤에 다 끼움. */
internal data class BookmarkInsertionFrame(val lift: Float, val slide: Float)

internal fun bookmarkInsertionFrame(elapsedMs: Float): BookmarkInsertionFrame {
    val raised = LiftEasing.transform((elapsedMs / LIFT_END_MS).coerceIn(0f, 1f))
    val settled = LiftEasing.transform(
        ((elapsedMs - SETTLE_START_MS) / (BOOKMARK_INSERT_TOTAL_MS - SETTLE_START_MS)).coerceIn(0f, 1f)
    )
    return BookmarkInsertionFrame(
        lift = raised * (1f - settled),
        slide = SlideEasing.transform(((elapsedMs - LIFT_END_MS) / SLIDE_MS).coerceIn(0f, 1f))
    )
}
