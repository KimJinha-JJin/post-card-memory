package com.postcardmemory.ui.detail

import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.postcardmemory.utils.PhotoStickerHalftoneRenderer
import kotlin.math.roundToInt

/**
 * 편집 미리보기에서 종이 스타일 사진 스티커를 그린다. 저장·공유 이미지의
 * PostcardImageExporter.drawPaperStyledSticker와 같은 PhotoStickerPaperSpec을
 * 읽어 같은 순서(종이 → 옅은 가장자리 선 → 창 안의 사진)로 그린다.
 *
 * 이 modifier 바깥 레이어가 이미 회전·대칭을 적용하므로(기존 제스처 좌표계를
 * 그대로 유지하기 위해), 종이는 대칭을 되돌려 그리고 사진 창은 거울상 위치에
 * 둔다 — 결과적으로 종이는 뒤집히지 않고 사진만 뒤집힌다.
 *
 * 사진 내용(AsyncImage)은 창 크기로 측정·배치된 뒤 창 경계로 잘린다.
 */
internal fun Modifier.photoStickerPaper(
    spec: PhotoStickerPaperSpec,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    selectionColor: Color?
): Modifier =
    this
        .drawWithCache {
            // 오린 외곽 path는 크기가 바뀔 때만 다시 만든다(이동·회전 중에는 재사용).
            val outlinePath =
                spec.paperOutline?.let { outline ->
                    Path().apply {
                        outline.forEachIndexed { index, point ->
                            val x = point.x * size.width
                            val y = point.y * size.height
                            if (index == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                }
            // 사진 위 망점 영역(뒤집힌 좌표계 기준 거울상 사진 창).
            val photoHalftoneClip =
                spec.halftone?.let {
                    val window =
                        spec.photoWindow.mirrored(flipHorizontal, flipVertical)
                    android.graphics.Path().apply {
                        addRect(
                            window.left * size.width,
                            window.top * size.height,
                            window.right * size.width,
                            window.bottom * size.height,
                            android.graphics.Path.Direction.CW
                        )
                    }
                }

            onDrawWithContent {
                scale(
                    scaleX = if (flipHorizontal) -1f else 1f,
                    scaleY = if (flipVertical) -1f else 1f,
                    pivot = center
                ) {
                    drawPhotoStickerPaper(spec, outlinePath)
                }

                drawContent()

                val halftone = spec.halftone
                if (halftone != null && photoHalftoneClip != null) {
                    drawIntoCanvas { canvas ->
                        PhotoStickerHalftoneRenderer.draw(
                            canvas = canvas.nativeCanvas,
                            clip = photoHalftoneClip,
                            originX = 0f,
                            originY = 0f,
                            side = size.width,
                            pitch = halftone.pitch,
                            angleDegrees = halftone.photoAngleDegrees,
                            inkArgb = halftone.photoInkArgb,
                            alpha = halftone.photoAlpha
                        )
                    }
                }

                if (selectionColor != null) {
                    val strokeWidth = 3.dp.toPx()
                    scale(
                        scaleX = if (flipHorizontal) -1f else 1f,
                        scaleY = if (flipVertical) -1f else 1f,
                        pivot = center
                    ) {
                        if (outlinePath != null) {
                            drawPath(
                                path = outlinePath,
                                color = selectionColor,
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            val radius =
                                (spec.paperCornerRadius * size.width - strokeWidth / 2f)
                                    .coerceAtLeast(0f)
                            drawRoundRect(
                                color = selectionColor,
                                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                                size = Size(
                                    size.width - strokeWidth,
                                    size.height - strokeWidth
                                ),
                                cornerRadius = CornerRadius(radius, radius),
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }
                }
            }
        }
        .layout { measurable, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val window =
                spec.photoWindow.mirrored(
                    flipHorizontal = flipHorizontal,
                    flipVertical = flipVertical
                )
            val left = (window.left * width).roundToInt()
            val top = (window.top * height).roundToInt()
            val right = (window.right * width).roundToInt()
            val bottom = (window.bottom * height).roundToInt()
            val placeable =
                measurable.measure(
                    Constraints.fixed(
                        width = (right - left).coerceAtLeast(0),
                        height = (bottom - top).coerceAtLeast(0)
                    )
                )

            layout(width, height) {
                placeable.place(left, top)
            }
        }
        .then(
            spec.photoClipOutline?.let { outline ->
                Modifier.clip(
                    photoClipShape(
                        outline = outline.mirrored(flipHorizontal, flipVertical),
                        window = spec.photoWindow.mirrored(flipHorizontal, flipVertical)
                    )
                )
            } ?: Modifier.clipToBounds()
        )

/** 사진 창(자식 레이아웃) 좌표로 옮긴 찢긴 인쇄층 다각형. */
private fun photoClipShape(
    outline: List<NormalizedPoint>,
    window: NormalizedRect
): GenericShape =
    GenericShape { size, _ ->
        val windowWidth = window.right - window.left
        val windowHeight = window.bottom - window.top
        outline.forEachIndexed { index, point ->
            val x = (point.x - window.left) / windowWidth * size.width
            val y = (point.y - window.top) / windowHeight * size.height
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

private fun DrawScope.drawPhotoStickerPaper(
    spec: PhotoStickerPaperSpec,
    outlinePath: Path?
) {
    val side = size.width
    val cornerRadius = spec.paperCornerRadius * side
    val edgeLineWidth = spec.edgeLineWidth * side

    if (outlinePath != null) {
        drawPath(
            path = outlinePath,
            color = Color(spec.paperArgb)
        )
        spec.halftone?.let { halftone ->
            drawPath(
                path = outlinePath,
                color = Color(halftone.marginInkArgb)
                    .copy(alpha = halftone.marginTintAlpha)
            )
            drawIntoCanvas { canvas ->
                PhotoStickerHalftoneRenderer.draw(
                    canvas = canvas.nativeCanvas,
                    clip = outlinePath.asAndroidPath(),
                    originX = 0f,
                    originY = 0f,
                    side = side,
                    pitch = halftone.pitch,
                    angleDegrees = halftone.marginAngleDegrees,
                    inkArgb = halftone.marginInkArgb,
                    alpha = halftone.marginAlpha
                )
            }
        }
        if (spec.cutCoreWidth > 0f) {
            // 외곽 안쪽에만 보이도록 외곽으로 잘라 두 배 폭 선을 긋는다.
            clipPath(outlinePath) {
                drawPath(
                    path = outlinePath,
                    color = Color(PHOTO_STICKER_CUT_CORE_ARGB),
                    style = Stroke(width = spec.cutCoreWidth * side * 2f)
                )
            }
        }
        drawPath(
            path = outlinePath,
            color = Color(PHOTO_STICKER_PAPER_EDGE_ARGB),
            style = Stroke(width = edgeLineWidth)
        )
        return
    }

    drawRoundRect(
        color = Color(spec.paperArgb),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )

    val edgeRadius =
        (cornerRadius - edgeLineWidth / 2f).coerceAtLeast(0f)
    drawRoundRect(
        color = Color(PHOTO_STICKER_PAPER_EDGE_ARGB),
        topLeft = Offset(edgeLineWidth / 2f, edgeLineWidth / 2f),
        size = Size(
            size.width - edgeLineWidth,
            size.height - edgeLineWidth
        ),
        cornerRadius = CornerRadius(edgeRadius, edgeRadius),
        style = Stroke(width = edgeLineWidth)
    )
}
