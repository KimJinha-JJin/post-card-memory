package com.postcardmemory.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val FLOAT_BASE_PERIOD_MS = 2600
private const val FLOAT_PERIOD_VARIANCE_MS = 900
private val FLOAT_AMPLITUDE = 3.dp

private val SCATTER_DISTANCE = 34.dp
private const val SCATTER_ROTATION_DEGREES = 18f
private const val SCATTER_STAGGER_MAX_MS = 120
private const val SCATTER_HOLD_MS = 180L

/**
 * 셔플(흔들기) 트리거가 바뀔 때마다 카드가 잠시 흩어졌다 원래 자리로 돌아오는
 * 애니메이션을 재생한다. shakeTrigger가 0(초기값)일 때는 재생하지 않는다.
 */
@Composable
fun StampCard(
    postcard: Postcard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    shakeTrigger: Int = 0,
    onLongClick: () -> Unit = {}
) {
    val seed = remember(postcard.id) { abs(postcard.id.hashCode()) }

    val rotation = remember(seed) {
        (seed % 70 - 35) / 10f
    }

    val dateFormatter = remember {
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )
    }

    // 평상시 아주 미세하게 둥둥 떠 있는 효과 — 카드마다 위상·주기를 살짝 달리해
    // 기계적으로 동시에 움직이는 느낌을 피한다.
    val floatPhase = remember(seed) { (seed % 1000) / 1000f }
    val floatPeriodMs = remember(seed) {
        FLOAT_BASE_PERIOD_MS + (seed % FLOAT_PERIOD_VARIANCE_MS)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "stampFloat")
    val floatAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = floatPeriodMs,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "stampFloatAngle"
    )

    val floatAmplitudePx = with(LocalDensity.current) {
        FLOAT_AMPLITUDE.toPx()
    }
    val scatterDistancePx = with(LocalDensity.current) {
        SCATTER_DISTANCE.toPx()
    }

    // 휴대폰을 흔들면 잠시 사방으로 흩어졌다가 원래 그리드 위치로 복귀한다.
    val scatterOffsetX = remember { Animatable(0f) }
    val scatterOffsetY = remember { Animatable(0f) }
    val scatterRotation = remember { Animatable(0f) }
    val staggerDelayMs = remember(seed) { seed % SCATTER_STAGGER_MAX_MS }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger <= 0) return@LaunchedEffect

        delay(staggerDelayMs.milliseconds)

        val targetX = (Random.nextFloat() * 2f - 1f) * scatterDistancePx
        val targetY = (Random.nextFloat() * 2f - 1f) * scatterDistancePx
        val targetRotation =
            (Random.nextFloat() * 2f - 1f) * SCATTER_ROTATION_DEGREES

        coroutineScope {
            launch {
                scatterOffsetX.animateTo(
                    targetX,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                scatterOffsetY.animateTo(
                    targetY,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                scatterRotation.animateTo(
                    targetRotation,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }

        delay(SCATTER_HOLD_MS.milliseconds)

        coroutineScope {
            launch {
                scatterOffsetX.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                scatterOffsetY.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                scatterRotation.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                translationX = scatterOffsetX.value
                translationY =
                    sin(floatAngle + floatPhase * 2f * PI.toFloat()) *
                            floatAmplitudePx +
                            scatterOffsetY.value
                rotationZ = rotation + scatterRotation.value
            }
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PaperSurface)
            .border(1.dp, PaperDivider, RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            StampPhoto(
                imagePath = postcard.imagePath,
                contentDescription = postcard.title,
                modifier = Modifier.fillMaxWidth(),
                outlineColor = Color.White,
                outlineWidth = 3f
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(PinkingPhotoShape)
                        .background(
                            color = BrutalCoral.copy(alpha = 0.3f)
                        )
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(14.dp)
                        .background(
                            color = BrutalCoral,
                            shape = CircleShape
                        )
                )
            }
        }

        Text(
            text = dateFormatter.format(
                Date(postcard.capturedAt)
            ),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = GraphiteAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
