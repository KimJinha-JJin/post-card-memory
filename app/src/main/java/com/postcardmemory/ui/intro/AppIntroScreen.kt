package com.postcardmemory.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private const val INTRO_FILL_DURATION_MS = 1800
private const val INTRO_SETTLE_DELAY_MS = 150L

/**
 * 앱 시작 직후 아주 짧게 보이는 인트로. 실제 갤러리 데이터 로딩과 연결된
 * 진행률이 아니라(Room Flow 초기 방출이 거의 즉시 끝나 의미 있는 진행률을
 * 만들 수 없다), 고정 길이의 짧은 시각 애니메이션이다.
 */
@Composable
fun AppIntroScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(INTRO_FILL_DURATION_MS, easing = LinearEasing)
        )
        delay(INTRO_SETTLE_DELAY_MS)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "작은 편지가 도착하고 있어",
                fontSize = 12.sp,
                color = InkSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AppIntroProgress(progress = progress.value)
        }
    }
}

@Composable
private fun AppIntroProgress(progress: Float) {
    val percent = (progress * 100f).roundToInt().coerceIn(0, 100)

    Row(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .clearAndSetSemantics {
                contentDescription = "엽서함을 여는 중"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
        ) {
            val density = LocalDensity.current
            var envelopeWidth by remember { mutableStateOf(0.dp) }
            val travel = (maxWidth - envelopeWidth).coerceAtLeast(0.dp)
            val envelopeOffset = travel * progress

            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerY = size.height * 0.82f
                val strokeWidthPx = 1.4.dp.toPx()
                drawLine(
                    color = PaperDivider,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = InkSecondary,
                    start = Offset(0f, centerY),
                    end = Offset(size.width * progress, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = "✉︎",
                fontSize = 14.sp,
                color = InkSecondary,
                modifier = Modifier
                    .offset(x = envelopeOffset)
                    .onGloballyPositioned {
                        envelopeWidth = with(density) { it.size.width.toDp() }
                    }
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "$percent%",
            fontSize = 13.sp,
            color = InkSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(30.dp)
        )
    }
}
