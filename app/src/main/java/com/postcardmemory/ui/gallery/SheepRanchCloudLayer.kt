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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private data class PixelCloudSpec(
    val blockDp: Float,
    val speedDpPerSecond: Float,
    val yFraction: Float,
    val alpha: Float,
    val startFraction: Float,
    val floatPeriodSeconds: Float,
    val floatAmplitudeDp: Float,
    val shape: List<Pair<Int, Int>>
)

private val PixelCloudWarmWhite = Color(0xFFFFFBF0)
private val PixelCloudCream = Color(0xFFF3E8D4)
private val PixelCloudBlueGray = Color(0xFFD8E2E6)

private val cloudSpecs = listOf(
    PixelCloudSpec(
        blockDp = 5f,
        speedDpPerSecond = 5.5f,
        yFraction = 0.11f,
        alpha = 0.09f,
        startFraction = 0.12f,
        floatPeriodSeconds = 11f,
        floatAmplitudeDp = 1.2f,
        shape = listOf(
            2 to 0, 3 to 0,
            1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1,
            0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2,
            1 to 3, 2 to 3, 3 to 3, 4 to 3
        )
    ),
    PixelCloudSpec(
        blockDp = 6f,
        speedDpPerSecond = 8.5f,
        yFraction = 0.19f,
        alpha = 0.13f,
        startFraction = 0.58f,
        floatPeriodSeconds = 9f,
        floatAmplitudeDp = 1.6f,
        shape = listOf(
            3 to 0, 4 to 0,
            1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1,
            0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2, 7 to 2,
            2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 3
        )
    ),
    PixelCloudSpec(
        blockDp = 7f,
        speedDpPerSecond = 12f,
        yFraction = 0.30f,
        alpha = 0.16f,
        startFraction = 0.86f,
        floatPeriodSeconds = 7f,
        floatAmplitudeDp = 1.4f,
        shape = listOf(
            2 to 0, 3 to 0, 6 to 0,
            1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 1,
            0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2, 7 to 2, 8 to 2,
            1 to 3, 2 to 3, 3 to 3, 5 to 3, 6 to 3, 7 to 3
        )
    ),
    PixelCloudSpec(
        blockDp = 4f,
        speedDpPerSecond = 15f,
        yFraction = 0.24f,
        alpha = 0.11f,
        startFraction = 0.34f,
        floatPeriodSeconds = 6.5f,
        floatAmplitudeDp = 1f,
        shape = listOf(
            2 to 0, 3 to 0, 4 to 0,
            1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1,
            0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2,
            2 to 3, 3 to 3, 4 to 3
        )
    )
)

@Composable
fun SheepRanchCloudLayer(
    modifier: Modifier = Modifier
) {
    var nowMillis by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis { frameMillis ->
                nowMillis = frameMillis
            }
        }
    }

    Canvas(modifier = modifier) {
        val oneDpPx = 1.dp.toPx()
        val elapsedSeconds = nowMillis / 1000f

        cloudSpecs.forEachIndexed { index, spec ->
            val blockPx = spec.blockDp.dp.toPx()
            val maxColumn = spec.shape.maxOf { it.first }
            val maxRow = spec.shape.maxOf { it.second }
            val cloudWidth = (maxColumn + 1) * blockPx
            val cloudHeight = (maxRow + 1) * blockPx
            val travelWidth = size.width + cloudWidth * 2f
            val speedPx = spec.speedDpPerSecond.dp.toPx()
            val baseX = (size.width * spec.startFraction + elapsedSeconds * speedPx) %
                    travelWidth
            val rawX = baseX - cloudWidth
            val rawY = size.height * spec.yFraction +
                    sin(
                        (elapsedSeconds / spec.floatPeriodSeconds +
                                index * 0.19f) *
                                2f *
                                PI.toFloat()
                    ) *
                    spec.floatAmplitudeDp.dp.toPx()
            val snappedX = (rawX / oneDpPx).roundToInt() * oneDpPx
            val snappedY = (rawY / oneDpPx).roundToInt() * oneDpPx

            spec.shape.forEach { block ->
                val color = when ((block.first + block.second + index) % 3) {
                    0 -> PixelCloudWarmWhite
                    1 -> PixelCloudCream
                    else -> PixelCloudBlueGray
                }
                drawRect(
                    color = color.copy(alpha = spec.alpha),
                    topLeft = Offset(
                        snappedX + block.first * blockPx,
                        snappedY + block.second * blockPx
                    ),
                    size = Size(blockPx, blockPx)
                )
            }

            drawRect(
                color = PixelCloudCream.copy(alpha = spec.alpha * 0.45f),
                topLeft = Offset(snappedX + blockPx, snappedY + cloudHeight),
                size = Size(cloudWidth - blockPx * 2f, blockPx)
            )
        }
    }
}
