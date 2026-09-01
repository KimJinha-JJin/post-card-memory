package com.postcardmemory.ui.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** 연못 모드의 포인트 색 — 앱의 코랄/골드 체계와 부딪히지 않는 차분한 청록. 토글 아이콘과 파문에 공용으로 쓴다. */
internal val PondAccentTeal = Color(0xFF4A8F8C)

/**
 * 발사·이동 경로·경계 충돌·주변 카드 반응에서 생긴 파문을 그리는 가벼운 오버레이.
 * 파문이 하나도 없으면 프레임을 요청하지 않아 배터리를 쓰지 않는다.
 *
 * 파문 좌표는 윈도우 좌표계로 저장되므로, 이 오버레이 자신의 윈도우상 좌상단
 * 좌표(originInWindow)를 빼서 캔버스 로컬 좌표로 변환한 뒤 그린다.
 */
@Composable
fun PondRippleOverlay(
    controller: PondController,
    originInWindow: Offset,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val rippleCount = controller.ripples.size

    LaunchedEffect(rippleCount) {
        if (rippleCount == 0) return@LaunchedEffect

        while (true) {
            withFrameMillis { }
            val current = System.currentTimeMillis()
            nowMillis = current
            controller.pruneRipples(current)
            if (controller.ripples.isEmpty()) break
        }
    }

    Canvas(modifier = modifier) {
        controller.ripples.forEach { ripple ->
            val age = (nowMillis - ripple.bornAtMillis).coerceAtLeast(0L)
            val progress = (age.toFloat() / ripple.lifespanMillis).coerceIn(0f, 1f)
            val radius = ripple.maxRadiusPx * (0.15f + 0.85f * progress)
            val alpha = (1f - progress) * 0.4f

            if (alpha > 0.01f) {
                drawCircle(
                    color = PondAccentTeal.copy(alpha = alpha),
                    radius = radius,
                    center = ripple.center - originInWindow,
                    style = Stroke(width = 2.5f)
                )
            }
        }
    }
}
