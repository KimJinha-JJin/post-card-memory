package com.postcardmemory.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.postcardmemory.utils.PostcardRenderSpec
import java.io.File
import kotlin.math.max

/**
 * 갤러리 우표 사진의 C2 얕은 천공 테두리. 계산은 저장·공유 exporter가 쓰는
 * [PostcardRenderSpec.createPinkingPath] 한 곳에만 두고, 여기서는 그 경로를
 * Compose Path로 감싸기만 한다 — 화면과 내보내기 이미지의 톱니 모양이 갈라지지 않게.
 */
private fun createPinkingPath(
    size: Size,
    inset: Float = 0f
): Path =
    PostcardRenderSpec
        .createPinkingPath(
            bounds = RectF(0f, 0f, size.width, size.height),
            inset = inset
        )
        .asComposePath()

object PinkingPhotoShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            createPinkingPath(size)
        )
    }
}

@Composable
fun StampPhoto(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 2f
) {
    val whiteOutlineColor =
        outlineColor.copy(
            red = 1f,
            green = 1f,
            blue = 1f
        )

    Box(
        modifier =
            modifier.aspectRatio(1f)
    ) {
        AsyncImage(
            model = File(imagePath),
            contentDescription =
                contentDescription,
            contentScale =
                ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(PinkingPhotoShape)
        )

        Canvas(
            modifier =
                Modifier.matchParentSize()
        ) {
            val strokeWidthPx =
                outlineWidth.dp.toPx()

            val borderPath =
                createPinkingPath(
                    size = size
                )

            // Same contour for photo and paper: a clipped double stroke leaves an inner rim.
            clipPath(borderPath) {
                drawPath(
                    path = borderPath,
                    color =
                        whiteOutlineColor,
                    style = Stroke(
                        width =
                            strokeWidthPx * 2f,
                        join =
                            StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun PinkingPhotoGuide(
    modifier: Modifier = Modifier,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 4f,
    haloColor: Color = Color.White,
    haloWidth: Float = 8f
) {
    val whiteOutlineColor =
        outlineColor.copy(
            red = 1f,
            green = 1f,
            blue = 1f
        )

    Canvas(
        modifier = modifier
    ) {
        val outlineWidthPx =
            outlineWidth.dp.toPx()

        val haloWidthPx =
            haloWidth.dp.toPx()

        val inset =
            max(
                outlineWidthPx,
                haloWidthPx
            ) / 2f

        val guidePath =
            createPinkingPath(
                size = size,
                inset = inset
            )

        drawPath(
            path = guidePath,
            color =
                haloColor.copy(
                    alpha = 0.85f
                ),
            style = Stroke(
                width =
                    haloWidthPx,
                join =
                    StrokeJoin.Round
            )
        )

        drawPath(
            path = guidePath,
            color =
                whiteOutlineColor,
            style = Stroke(
                width =
                    outlineWidthPx,
                join =
                    StrokeJoin.Round
            )
        )
    }
}
