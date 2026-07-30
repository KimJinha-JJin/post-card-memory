package com.postcardmemory.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.exifinterface.media.ExifInterface
import com.postcardmemory.ui.components.PostcardDateFormat
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

object PostcardRenderSpec {
    const val LOGICAL_SIZE = 2048f
    const val OUTPUT_SIZE = 2048
    const val STAMP_BORDER_WIDTH = 18f
    private const val SPECKLE_PATTERN_SEED = 20240704L
    private const val SPECKLE_COUNT = 900
    private const val STAMP_BASE_PHOTO_SIZE = 1000f
    private const val STAMP_PHOTO_ZONE_CENTER_Y = 802.5f
    private const val POLAROID_ANCHOR_X = 1024f
    private const val POLAROID_ANCHOR_Y = 805f
    private const val POLAROID_PAPER_HALF_WIDTH = 630f
    private const val POLAROID_PAPER_HALF_HEIGHT = 685f
    private const val POLAROID_PHOTO_HALF_WIDTH = 570f
    private const val POLAROID_PHOTO_TOP_OFFSET = 625f
    private const val POLAROID_PHOTO_BOTTOM_OFFSET = 515f
    private const val TAPED_FILM_ANCHOR_X = 1024f
    private const val TAPED_FILM_ANCHOR_Y = 720f
    private const val TAPED_FILM_HALF_WIDTH = 760f
    private const val TAPED_FILM_HALF_HEIGHT = 290f
    private const val TAPED_FILM_PHOTO_SIDE_INSET = 50f
    private const val TAPED_FILM_PHOTO_VERTICAL_INSET = 100f
    private const val TAPED_FILM_SPROCKET_SIZE = 26f
    private const val TAPED_FILM_SPROCKET_GAP = 22f
    private const val TAPED_FILM_TAPE_WIDTH = 210f
    private const val TAPED_FILM_TAPE_HEIGHT = 92f
    private const val LETTER_ANCHOR_X = 1024f
    private const val LETTER_ANCHOR_Y = 710f
    private const val LETTER_PHOTO_HALF_WIDTH = 928f
    private const val LETTER_PHOTO_HALF_HEIGHT = 570f
    private const val LETTER_DIVIDER_Y = 1312f
    private const val LETTER_DIVIDER_LEFT = 96f
    private const val LETTER_DIVIDER_RIGHT = 1952f

    enum class LayoutStyle {
        STAMP,
        POLAROID,
        TAPED_FILM,
        LETTER
    }

    enum class PhotoFrameStyle {
        PINKING,
        POLAROID,
        TAPED_FILM,
        LETTER
    }

    data class RenderLayout(
        val stampBounds: RectF,
        val messagePanel: RectF,
        val datePanel: RectF,
        val photoBounds: RectF = stampBounds,
        val photoFrameStyle: PhotoFrameStyle = PhotoFrameStyle.PINKING,
        val compactMessage: Boolean = false,
        val compactDate: Boolean = false,
        val darkMessageOverlay: Boolean = false,
        val photoOffsetX: Float = 0f,
        val photoOffsetY: Float = 0f,
        val photoZoom: Float = 1f,
        /** 사진과 문구 사이 얇은 구분선(편지지 레이아웃 전용). 다른 레이아웃은 null이라 그리지 않는다. */
        val dividerY: Float? = null
    )

    fun resolveLayoutStyle(
        layoutStyle: String
    ): LayoutStyle {
        return when (layoutStyle) {
            "POLAROID" -> LayoutStyle.POLAROID
            "TAPED_FILM" -> LayoutStyle.TAPED_FILM
            "LETTER" -> LayoutStyle.LETTER
            else -> LayoutStyle.STAMP
        }
    }

    fun layoutFor(
        layoutStyle: String,
        stampPhotoScale: Float = 1f,
        polaroidPhotoScale: Float = 1f,
        stampPhotoOffsetX: Float = 0f,
        stampPhotoOffsetY: Float = 0f,
        polaroidPhotoOffsetX: Float = 0f,
        polaroidPhotoOffsetY: Float = 0f,
        tapedFilmPhotoOffsetX: Float = 0f,
        tapedFilmPhotoOffsetY: Float = 0f,
        stampPhotoZoom: Float = 1f,
        polaroidPhotoZoom: Float = 1f,
        tapedFilmPhotoZoom: Float = 1f
    ): RenderLayout {
        return when (resolveLayoutStyle(layoutStyle)) {
            LayoutStyle.STAMP -> {
                val clampedScale =
                    stampPhotoScale.coerceIn(0.7f, 1.3f)
                val photoSize =
                    STAMP_BASE_PHOTO_SIZE * clampedScale
                val half = photoSize / 2f
                val centerX = LOGICAL_SIZE / 2f
                val centerY = STAMP_PHOTO_ZONE_CENTER_Y

                RenderLayout(
                    stampBounds = RectF(
                        centerX - half,
                        centerY - half,
                        centerX + half,
                        centerY + half
                    ),
                    messagePanel = RectF(220f, 1505f, 1828f, 1748f),
                    datePanel = RectF(544f, 1846f, 1504f, 1932f),
                    photoOffsetX = stampPhotoOffsetX,
                    photoOffsetY = stampPhotoOffsetY,
                    photoZoom = stampPhotoZoom
                )
            }

            LayoutStyle.POLAROID -> {
                val clampedScale =
                    polaroidPhotoScale.coerceIn(0.75f, 1.05f)

                RenderLayout(
                    stampBounds = RectF(
                        POLAROID_ANCHOR_X -
                                POLAROID_PAPER_HALF_WIDTH * clampedScale,
                        POLAROID_ANCHOR_Y -
                                POLAROID_PAPER_HALF_HEIGHT * clampedScale,
                        POLAROID_ANCHOR_X +
                                POLAROID_PAPER_HALF_WIDTH * clampedScale,
                        POLAROID_ANCHOR_Y +
                                POLAROID_PAPER_HALF_HEIGHT * clampedScale
                    ),
                    photoBounds = RectF(
                        POLAROID_ANCHOR_X -
                                POLAROID_PHOTO_HALF_WIDTH * clampedScale,
                        POLAROID_ANCHOR_Y -
                                POLAROID_PHOTO_TOP_OFFSET * clampedScale,
                        POLAROID_ANCHOR_X +
                                POLAROID_PHOTO_HALF_WIDTH * clampedScale,
                        POLAROID_ANCHOR_Y +
                                POLAROID_PHOTO_BOTTOM_OFFSET * clampedScale
                    ),
                    photoFrameStyle = PhotoFrameStyle.POLAROID,
                    messagePanel = RectF(220f, 1565f, 1828f, 1798f),
                    datePanel = RectF(544f, 1860f, 1504f, 1946f),
                    photoOffsetX = polaroidPhotoOffsetX,
                    photoOffsetY = polaroidPhotoOffsetY,
                    photoZoom = polaroidPhotoZoom
                )
            }

            LayoutStyle.TAPED_FILM -> {
                val clampedScale =
                    stampPhotoScale.coerceIn(0.85f, 1.15f)
                val halfWidth =
                    TAPED_FILM_HALF_WIDTH * clampedScale
                val halfHeight =
                    TAPED_FILM_HALF_HEIGHT * clampedScale
                val sideInset =
                    TAPED_FILM_PHOTO_SIDE_INSET * clampedScale
                val verticalInset =
                    TAPED_FILM_PHOTO_VERTICAL_INSET * clampedScale

                val filmBounds =
                    RectF(
                        TAPED_FILM_ANCHOR_X - halfWidth,
                        TAPED_FILM_ANCHOR_Y - halfHeight,
                        TAPED_FILM_ANCHOR_X + halfWidth,
                        TAPED_FILM_ANCHOR_Y + halfHeight
                    )
                val photoBounds =
                    RectF(
                        filmBounds.left + sideInset,
                        filmBounds.top + verticalInset,
                        filmBounds.right - sideInset,
                        filmBounds.bottom - verticalInset
                    )

                RenderLayout(
                    stampBounds = filmBounds,
                    photoBounds = photoBounds,
                    photoFrameStyle = PhotoFrameStyle.TAPED_FILM,
                    messagePanel = RectF(220f, 1300f, 1828f, 1560f),
                    datePanel = RectF(544f, 1620f, 1504f, 1716f),
                    photoOffsetX = tapedFilmPhotoOffsetX,
                    photoOffsetY = tapedFilmPhotoOffsetY,
                    photoZoom = tapedFilmPhotoZoom
                )
            }

            // 편지지: 사진은 상단 약 60~65%, 얇은 구분선 아래로 기존 레이아웃보다
            // 넓은 문구 영역, 날짜는 하단 오른쪽. Postcard/Room에 새 컬럼을
            // 추가하지 않도록 TAPED_FILM과 동일하게 stampPhoto* 값을 재사용한다.
            LayoutStyle.LETTER -> {
                val clampedScale =
                    stampPhotoScale.coerceIn(0.85f, 1.15f)
                val halfWidth =
                    LETTER_PHOTO_HALF_WIDTH * clampedScale
                val halfHeight =
                    LETTER_PHOTO_HALF_HEIGHT * clampedScale

                val photoBounds =
                    RectF(
                        LETTER_ANCHOR_X - halfWidth,
                        LETTER_ANCHOR_Y - halfHeight,
                        LETTER_ANCHOR_X + halfWidth,
                        LETTER_ANCHOR_Y + halfHeight
                    )

                RenderLayout(
                    stampBounds = photoBounds,
                    photoBounds = photoBounds,
                    photoFrameStyle = PhotoFrameStyle.LETTER,
                    // 다른 레이아웃보다 세로로 넓은 문구 영역.
                    messagePanel = RectF(96f, 1352f, 1952f, 1840f),
                    // 우측 정렬처럼 보이도록 패널 자체를 오른쪽으로 좁게 배치.
                    datePanel = RectF(1300f, 1868f, 1952f, 1952f),
                    photoOffsetX = stampPhotoOffsetX,
                    photoOffsetY = stampPhotoOffsetY,
                    photoZoom = stampPhotoZoom,
                    dividerY = LETTER_DIVIDER_Y
                )
            }
        }
    }

    fun drawBaseContent(
        canvas: Canvas,
        sourceBitmap: Bitmap,
        backgroundColorArgb: Long,
        backgroundPattern: String,
        message: String,
        messageFont: String,
        layoutStyle: String,
        capturedAt: Long,
        dateFormat: String,
        targetSize: Float = LOGICAL_SIZE,
        messageTextScale: Float = 1f,
        dateTextScale: Float = 1f,
        backgroundPatternDensity: Float = 1f,
        stampPhotoScale: Float = 1f,
        polaroidPhotoScale: Float = 1f,
        photoEdgeBlur: Float = 0f,
        stampPhotoOffsetX: Float = 0f,
        stampPhotoOffsetY: Float = 0f,
        polaroidPhotoOffsetX: Float = 0f,
        polaroidPhotoOffsetY: Float = 0f,
        tapedFilmPhotoOffsetX: Float = 0f,
        tapedFilmPhotoOffsetY: Float = 0f,
        stampPhotoZoom: Float = 1f,
        polaroidPhotoZoom: Float = 1f,
        tapedFilmPhotoZoom: Float = 1f
    ) {
        val scale =
            targetSize / LOGICAL_SIZE

        canvas.save()
        canvas.scale(scale, scale)

        val layout =
            layoutFor(
                layoutStyle = layoutStyle,
                stampPhotoScale = stampPhotoScale,
                polaroidPhotoScale = polaroidPhotoScale,
                stampPhotoOffsetX = stampPhotoOffsetX,
                stampPhotoOffsetY = stampPhotoOffsetY,
                polaroidPhotoOffsetX = polaroidPhotoOffsetX,
                polaroidPhotoOffsetY = polaroidPhotoOffsetY,
                tapedFilmPhotoOffsetX = tapedFilmPhotoOffsetX,
                tapedFilmPhotoOffsetY = tapedFilmPhotoOffsetY,
                stampPhotoZoom = stampPhotoZoom,
                polaroidPhotoZoom = polaroidPhotoZoom,
                tapedFilmPhotoZoom = tapedFilmPhotoZoom
            )

        drawBackground(
            canvas = canvas,
            backgroundColorArgb = backgroundColorArgb,
            backgroundPattern = backgroundPattern,
            patternDensity = backgroundPatternDensity
        )
        when (layout.photoFrameStyle) {
            PhotoFrameStyle.PINKING -> {
                drawStampPhoto(
                    canvas = canvas,
                    sourceBitmap = sourceBitmap,
                    stampBounds = layout.stampBounds,
                    edgeBlur = photoEdgeBlur,
                    offsetX = layout.photoOffsetX,
                    offsetY = layout.photoOffsetY,
                    zoom = layout.photoZoom
                )
            }

            PhotoFrameStyle.POLAROID -> {
                drawPolaroidPhoto(
                    canvas = canvas,
                    sourceBitmap = sourceBitmap,
                    paperBounds = layout.stampBounds,
                    photoBounds = layout.photoBounds,
                    edgeBlur = photoEdgeBlur,
                    offsetX = layout.photoOffsetX,
                    offsetY = layout.photoOffsetY,
                    zoom = layout.photoZoom
                )
            }

            PhotoFrameStyle.TAPED_FILM -> {
                drawTapedFilmPhoto(
                    canvas = canvas,
                    sourceBitmap = sourceBitmap,
                    filmBounds = layout.stampBounds,
                    photoBounds = layout.photoBounds,
                    edgeBlur = photoEdgeBlur,
                    offsetX = layout.photoOffsetX,
                    offsetY = layout.photoOffsetY,
                    zoom = layout.photoZoom
                )
            }

            PhotoFrameStyle.LETTER -> {
                drawLetterPhoto(
                    canvas = canvas,
                    sourceBitmap = sourceBitmap,
                    photoBounds = layout.photoBounds,
                    edgeBlur = photoEdgeBlur,
                    offsetX = layout.photoOffsetX,
                    offsetY = layout.photoOffsetY,
                    zoom = layout.photoZoom
                )
            }
        }
        layout.dividerY?.let { dividerY ->
            drawLetterDivider(
                canvas = canvas,
                dividerY = dividerY,
                left = LETTER_DIVIDER_LEFT,
                right = LETTER_DIVIDER_RIGHT
            )
        }
        drawMessage(
            canvas = canvas,
            message = message,
            messageFont = messageFont,
            messagePanel = layout.messagePanel,
            darkOverlay = layout.darkMessageOverlay,
            compact = layout.compactMessage,
            textScale = messageTextScale
        )
        drawDate(
            canvas = canvas,
            capturedAt = capturedAt,
            dateFormat = dateFormat,
            datePanel = layout.datePanel,
            compact = layout.compactDate,
            textScale = dateTextScale
        )

        canvas.restore()
    }

    fun decodeSourceBitmap(
        sourceFile: File
    ): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source =
                ImageDecoder.createSource(sourceFile)

            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val decodedBitmap =
                BitmapFactory.decodeFile(sourceFile.absolutePath)
                    ?: throw IOException("사진을 불러오지 못했어.")
            val orientation =
                ExifInterface(sourceFile.absolutePath)
                    .getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )

            rotateBitmapUsingExif(
                bitmap = decodedBitmap,
                orientation = orientation
            )
        }
    }

    fun drawCenterCroppedBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        destinationRect: RectF,
        edgeBlur: Float = 0f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        zoom: Float = 1f
    ) {
        val sourceWidth =
            bitmap.width.toFloat()
        val sourceHeight =
            bitmap.height.toFloat()

        if (sourceWidth <= 0f || sourceHeight <= 0f) {
            throw IOException("사진 크기를 확인하지 못했어.")
        }

        val destinationRatio =
            destinationRect.width() /
                    destinationRect.height()
        val sourceRatio =
            sourceWidth /
                    sourceHeight

        val sourceWidthInt =
            bitmap.width
        val sourceHeightInt =
            bitmap.height
        val clampedOffsetX = offsetX.coerceIn(-1f, 1f)
        val clampedOffsetY = offsetY.coerceIn(-1f, 1f)
        val clampedZoom = zoom.coerceIn(1f, 3f)

        // Base crop is the largest window matching destinationRatio that
        // still fits inside the source bitmap; zoom then shrinks it further
        // (both dimensions, keeping the ratio), which is what opens up pan
        // room on whichever axis had none at zoom = 1.
        val baseCroppedWidth: Float
        val baseCroppedHeight: Float

        if (sourceRatio > destinationRatio) {
            baseCroppedHeight = sourceHeight
            baseCroppedWidth = sourceHeight * destinationRatio
        } else {
            baseCroppedWidth = sourceWidth
            baseCroppedHeight = sourceWidth / destinationRatio
        }

        val croppedWidth = baseCroppedWidth / clampedZoom
        val croppedHeight = baseCroppedHeight / clampedZoom

        val slackX = (sourceWidth - croppedWidth).coerceAtLeast(0f)
        val slackY = (sourceHeight - croppedHeight).coerceAtLeast(0f)

        val left =
            (slackX / 2f + clampedOffsetX * (slackX / 2f))
                .coerceIn(0f, slackX)
        val top =
            (slackY / 2f + clampedOffsetY * (slackY / 2f))
                .coerceIn(0f, slackY)

        val sourceRect =
            Rect(
                left.toInt()
                    .coerceIn(0, sourceWidthInt),
                top.toInt()
                    .coerceIn(0, sourceHeightInt),
                (left + croppedWidth).toInt()
                    .coerceIn(0, sourceWidthInt),
                (top + croppedHeight).toInt()
                    .coerceIn(0, sourceHeightInt)
            )

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )

        canvas.drawBitmap(
            bitmap,
            sourceRect,
            destinationRect,
            paint
        )

        val clampedEdgeBlur =
            edgeBlur.coerceIn(0f, 1f)

        if (clampedEdgeBlur > 0f) {
            drawEdgeBlurOverlay(
                canvas = canvas,
                bitmap = bitmap,
                sourceRect = sourceRect,
                destinationRect = destinationRect,
                intensity = clampedEdgeBlur,
                paint = paint
            )
        }
    }

    private fun drawEdgeBlurOverlay(
        canvas: Canvas,
        bitmap: Bitmap,
        sourceRect: Rect,
        destinationRect: RectF,
        intensity: Float,
        paint: Paint
    ) {
        val croppedWidth =
            sourceRect.width()
        val croppedHeight =
            sourceRect.height()

        if (croppedWidth <= 0 || croppedHeight <= 0) {
            return
        }

        val visibleBitmap =
            Bitmap.createBitmap(
                bitmap,
                sourceRect.left,
                sourceRect.top,
                croppedWidth,
                croppedHeight
            )

        val blurredBitmap =
            createFullyBlurredBitmap(
                bitmap = visibleBitmap
            )

        val saveCount =
            canvas.saveLayer(
                destinationRect.left,
                destinationRect.top,
                destinationRect.right,
                destinationRect.bottom,
                null
            )

        val blurredPaint =
            Paint(paint).apply {
                alpha =
                    (intensity * 255f)
                        .roundToInt()
                        .coerceIn(0, 255)
            }

        canvas.drawBitmap(
            blurredBitmap,
            null,
            destinationRect,
            blurredPaint
        )

        // Shorter-side radius keeps the fade even on all edges, not just corners.
        val vignetteRadius =
            min(
                destinationRect.width(),
                destinationRect.height()
            ) / 2f

        val gradient =
            RadialGradient(
                destinationRect.centerX(),
                destinationRect.centerY(),
                vignetteRadius,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.BLACK
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )

        val maskPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = gradient
                xfermode =
                    PorterDuffXfermode(
                        PorterDuff.Mode.DST_IN
                    )
            }

        canvas.drawRect(
            destinationRect,
            maskPaint
        )

        canvas.restoreToCount(saveCount)

        if (visibleBitmap !== bitmap) {
            visibleBitmap.recycle()
        }

        if (blurredBitmap !== visibleBitmap) {
            blurredBitmap.recycle()
        }
    }

    // Repeated halving looks smoother than one big downsample/upsample jump.
    private fun createFullyBlurredBitmap(
        bitmap: Bitmap
    ): Bitmap {
        val halvingSteps = 6
        val width = bitmap.width
        val height = bitmap.height

        var current = bitmap
        val intermediates = mutableListOf<Bitmap>()

        repeat(halvingSteps) {
            val nextWidth =
                (current.width / 2)
                    .coerceAtLeast(1)
            val nextHeight =
                (current.height / 2)
                    .coerceAtLeast(1)

            if (
                nextWidth == current.width &&
                nextHeight == current.height
            ) {
                return@repeat
            }

            val smaller =
                Bitmap.createScaledBitmap(
                    current,
                    nextWidth,
                    nextHeight,
                    true
                )

            if (current !== bitmap) {
                intermediates.add(current)
            }

            current = smaller
        }

        val blurred =
            Bitmap.createScaledBitmap(
                current,
                width,
                height,
                true
            )

        if (current !== bitmap) {
            intermediates.add(current)
        }

        intermediates.forEach { candidate ->
            if (candidate !== blurred) {
                candidate.recycle()
            }
        }

        return blurred
    }

    fun createPinkingPath(
        bounds: RectF,
        inset: Float = 0f
    ): Path {
        val left = bounds.left + inset
        val top = bounds.top + inset
        val right = bounds.right - inset
        val bottom = bounds.bottom - inset
        val width = (right - left).coerceAtLeast(1f)
        val height = (bottom - top).coerceAtLeast(1f)
        val shortestSide = min(width, height)
        val toothDepth = (shortestSide * 0.032f).coerceAtLeast(2f)
        val cornerCut = toothDepth * 1.5f
        val horizontalLength = (width - cornerCut * 2f).coerceAtLeast(1f)
        val verticalLength = (height - cornerCut * 2f).coerceAtLeast(1f)
        val horizontalTeeth =
            max(8, (horizontalLength / (toothDepth * 2.3f)).toInt())
        val verticalTeeth =
            max(8, (verticalLength / (toothDepth * 2.3f)).toInt())
        val horizontalStep = horizontalLength / horizontalTeeth
        val verticalStep = verticalLength / verticalTeeth

        return Path().apply {
            moveTo(left + cornerCut, top)

            repeat(horizontalTeeth) { index ->
                val startX = left + cornerCut + index * horizontalStep
                lineTo(startX + horizontalStep / 2f, top + toothDepth)
                lineTo(startX + horizontalStep, top)
            }

            lineTo(right, top + cornerCut)

            repeat(verticalTeeth) { index ->
                val startY = top + cornerCut + index * verticalStep
                lineTo(right - toothDepth, startY + verticalStep / 2f)
                lineTo(right, startY + verticalStep)
            }

            lineTo(right - cornerCut, bottom)

            repeat(horizontalTeeth) { index ->
                val startX = right - cornerCut - index * horizontalStep
                lineTo(startX - horizontalStep / 2f, bottom - toothDepth)
                lineTo(startX - horizontalStep, bottom)
            }

            lineTo(left, bottom - cornerCut)

            repeat(verticalTeeth) { index ->
                val startY = bottom - cornerCut - index * verticalStep
                lineTo(left + toothDepth, startY - verticalStep / 2f)
                lineTo(left, startY - verticalStep)
            }

            close()
        }
    }

    private fun drawBackground(
        canvas: Canvas,
        backgroundColorArgb: Long,
        backgroundPattern: String,
        patternDensity: Float = 1f
    ) {
        canvas.drawColor(backgroundColorArgb.toInt())

        drawBackgroundPattern(
            canvas = canvas,
            backgroundPattern = backgroundPattern,
            backgroundColorArgb = backgroundColorArgb,
            patternDensity = patternDensity
        )

        val innerBorderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(115, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }

        canvas.drawRect(
            32f,
            32f,
            LOGICAL_SIZE - 32f,
            LOGICAL_SIZE - 32f,
            innerBorderPaint
        )
    }

    private fun drawBackgroundPattern(
        canvas: Canvas,
        backgroundPattern: String,
        backgroundColorArgb: Long,
        patternDensity: Float = 1f
    ) {
        if (backgroundPattern == "NONE") {
            return
        }

        val patternColor =
            getPatternColor(backgroundColorArgb)
        val density =
            patternDensity.coerceIn(0.7f, 1.5f)

        when (backgroundPattern) {
            "CHECKER" -> drawCheckerPattern(canvas, patternColor, density)
            "DOTS" -> drawDotsPattern(canvas, patternColor, density)
            "STRIPES" -> drawStripePattern(canvas, patternColor, density)
            "WAVES" -> drawWavePattern(canvas, patternColor, density)
            "GRID" -> drawGridPattern(canvas, patternColor, density)
            "CROSSHATCH" -> drawCrosshatchPattern(canvas, patternColor, density)
            "SPECKLE" -> drawSpecklePattern(canvas, patternColor, density)
        }
    }

    private fun drawDotsPattern(
        canvas: Canvas,
        color: Int,
        density: Float = 1f
    ) {
        val cellSize = 260f / density
        val horizontalCount = (LOGICAL_SIZE / cellSize).toInt() + 3
        val verticalCount = (LOGICAL_SIZE / cellSize).toInt() + 3
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }

        for (row in -1..verticalCount) {
            val staggerOffset =
                if (row % 2 == 0) 0f else cellSize / 2f

            for (column in -1..horizontalCount) {
                canvas.drawCircle(
                    column * cellSize + staggerOffset,
                    row * cellSize,
                    if ((row + column) % 2 == 0) 13f else 8f,
                    paint
                )
            }
        }
    }

    private fun drawStripePattern(
        canvas: Canvas,
        color: Int,
        density: Float = 1f
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
        val spacing = 64f / density
        var offset = -LOGICAL_SIZE

        while (offset < LOGICAL_SIZE * 2f) {
            canvas.drawLine(
                offset,
                0f,
                offset + LOGICAL_SIZE,
                LOGICAL_SIZE,
                paint
            )
            offset += spacing
        }
    }

    private fun drawWavePattern(
        canvas: Canvas,
        color: Int,
        density: Float = 1f
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
        val waveLength = 120f / density
        val amplitude = 16f
        val rowSpacing = 70f / density
        var y = 0f

        while (y < LOGICAL_SIZE + rowSpacing) {
            val path =
                Path().apply {
                    moveTo(0f, y)

                    var x = 0f

                    while (x < LOGICAL_SIZE) {
                        quadTo(
                            x + waveLength / 4f,
                            y - amplitude,
                            x + waveLength / 2f,
                            y
                        )
                        quadTo(
                            x + waveLength * 3f / 4f,
                            y + amplitude,
                            x + waveLength,
                            y
                        )
                        x += waveLength
                    }
                }

            canvas.drawPath(path, paint)
            y += rowSpacing
        }
    }

    private fun drawGridPattern(
        canvas: Canvas,
        color: Int,
        density: Float = 1f
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
        val spacing = 96f / density
        var x = 0f

        while (x <= LOGICAL_SIZE) {
            canvas.drawLine(x, 0f, x, LOGICAL_SIZE, paint)
            x += spacing
        }

        var y = 0f

        while (y <= LOGICAL_SIZE) {
            canvas.drawLine(0f, y, LOGICAL_SIZE, y, paint)
            y += spacing
        }
    }

    private fun drawCrosshatchPattern(
        canvas: Canvas,
        color: Int,
        density: Float = 1f
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
        val spacing = 80f / density
        var offset = -LOGICAL_SIZE

        while (offset < LOGICAL_SIZE * 2f) {
            canvas.drawLine(
                offset,
                0f,
                offset + LOGICAL_SIZE,
                LOGICAL_SIZE,
                paint
            )
            canvas.drawLine(
                offset,
                LOGICAL_SIZE,
                offset + LOGICAL_SIZE,
                0f,
                paint
            )
            offset += spacing
        }
    }

    private fun drawSpecklePattern(
        canvas: Canvas,
        color: Int,
        density: Float = 1f
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
        val random = Random(SPECKLE_PATTERN_SEED)
        val speckleCount =
            (SPECKLE_COUNT * density).toInt()

        repeat(speckleCount) {
            canvas.drawCircle(
                random.nextFloat() * LOGICAL_SIZE,
                random.nextFloat() * LOGICAL_SIZE,
                3f + random.nextFloat() * 4f,
                paint
            )
        }
    }

    private fun drawStampPhoto(
        canvas: Canvas,
        sourceBitmap: Bitmap,
        stampBounds: RectF,
        edgeBlur: Float = 0f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        zoom: Float = 1f
    ) {
        val stampPath =
            createPinkingPath(
                bounds = stampBounds,
                inset = STAMP_BORDER_WIDTH / 2f
            )
        val shadowBounds =
            RectF(
                stampBounds.left + 22f,
                stampBounds.top + 26f,
                stampBounds.right + 22f,
                stampBounds.bottom + 26f
            )
        val shadowPath =
            createPinkingPath(
                bounds = shadowBounds,
                inset = STAMP_BORDER_WIDTH / 2f
            )
        val shadowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(85, 18, 12, 28)
                style = Paint.Style.FILL
            }

        canvas.drawPath(shadowPath, shadowPaint)
        canvas.save()
        canvas.clipPath(stampPath)
        drawCenterCroppedBitmap(
            canvas = canvas,
            bitmap = sourceBitmap,
            destinationRect = stampBounds,
            edgeBlur = edgeBlur,
            offsetX = offsetX,
            offsetY = offsetY,
            zoom = zoom
        )
        canvas.restore()

        val whiteBorderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = STAMP_BORDER_WIDTH
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }

        canvas.drawPath(stampPath, whiteBorderPaint)
    }

    private fun drawPolaroidPhoto(
        canvas: Canvas,
        sourceBitmap: Bitmap,
        paperBounds: RectF,
        photoBounds: RectF,
        edgeBlur: Float = 0f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        zoom: Float = 1f
    ) {
        val paperCornerRadius = 18f
        val shadowBounds =
            RectF(
                paperBounds.left + 18f,
                paperBounds.top + 22f,
                paperBounds.right + 18f,
                paperBounds.bottom + 22f
            )
        val shadowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(60, 18, 12, 28)
                style = Paint.Style.FILL
            }

        canvas.drawRoundRect(
            shadowBounds,
            paperCornerRadius,
            paperCornerRadius,
            shadowPaint
        )

        val paperPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 253, 248)
                style = Paint.Style.FILL
            }

        canvas.drawRoundRect(
            paperBounds,
            paperCornerRadius,
            paperCornerRadius,
            paperPaint
        )

        val paperOutlinePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(45, 40, 30, 35)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

        canvas.drawRoundRect(
            paperBounds,
            paperCornerRadius,
            paperCornerRadius,
            paperOutlinePaint
        )

        val photoCornerRadius = 8f
        val photoPath =
            Path().apply {
                addRoundRect(
                    photoBounds,
                    photoCornerRadius,
                    photoCornerRadius,
                    Path.Direction.CW
                )
            }

        canvas.save()
        canvas.clipPath(photoPath)
        drawCenterCroppedBitmap(
            canvas = canvas,
            bitmap = sourceBitmap,
            destinationRect = photoBounds,
            edgeBlur = edgeBlur,
            offsetX = offsetX,
            offsetY = offsetY,
            zoom = zoom
        )
        canvas.restore()

        val photoBorderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(45, 30, 24, 34)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

        canvas.drawRoundRect(
            photoBounds,
            photoCornerRadius,
            photoCornerRadius,
            photoBorderPaint
        )
    }

    private fun drawTapedFilmPhoto(
        canvas: Canvas,
        sourceBitmap: Bitmap,
        filmBounds: RectF,
        photoBounds: RectF,
        edgeBlur: Float = 0f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        zoom: Float = 1f
    ) {
        val frameCornerRadius = 16f

        val shadowBounds =
            RectF(
                filmBounds.left + 18f,
                filmBounds.top + 22f,
                filmBounds.right + 18f,
                filmBounds.bottom + 22f
            )
        val shadowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(70, 18, 12, 28)
                style = Paint.Style.FILL
            }

        canvas.drawRoundRect(shadowBounds, frameCornerRadius, frameCornerRadius, shadowPaint)

        val filmPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(28, 24, 26)
                style = Paint.Style.FILL
            }

        canvas.drawRoundRect(filmBounds, frameCornerRadius, frameCornerRadius, filmPaint)

        canvas.save()
        canvas.clipRect(photoBounds)
        drawCenterCroppedBitmap(
            canvas = canvas,
            bitmap = sourceBitmap,
            destinationRect = photoBounds,
            edgeBlur = edgeBlur,
            offsetX = offsetX,
            offsetY = offsetY,
            zoom = zoom
        )
        canvas.restore()

        val photoBorderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(160, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }

        canvas.drawRect(photoBounds, photoBorderPaint)

        drawFilmSprocketRow(
            canvas = canvas,
            filmBounds = filmBounds,
            centerY = (filmBounds.top + photoBounds.top) / 2f
        )
        drawFilmSprocketRow(
            canvas = canvas,
            filmBounds = filmBounds,
            centerY = (photoBounds.bottom + filmBounds.bottom) / 2f
        )

        drawMaskingTape(
            canvas = canvas,
            centerX = filmBounds.left + TAPED_FILM_TAPE_WIDTH * 0.55f,
            centerY = filmBounds.top
        )
        drawMaskingTape(
            canvas = canvas,
            centerX = filmBounds.right - TAPED_FILM_TAPE_WIDTH * 0.55f,
            centerY = filmBounds.top
        )
    }

    private fun drawFilmSprocketRow(
        canvas: Canvas,
        filmBounds: RectF,
        centerY: Float
    ) {
        val holePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(250, 248, 244)
                style = Paint.Style.FILL
            }

        val margin = TAPED_FILM_PHOTO_SIDE_INSET
        val usableWidth = filmBounds.width() - margin * 2f
        val step = TAPED_FILM_SPROCKET_SIZE + TAPED_FILM_SPROCKET_GAP
        val holeCount = (usableWidth / step).toInt().coerceAtLeast(1)
        val totalHolesWidth = holeCount * step - TAPED_FILM_SPROCKET_GAP
        var x = filmBounds.left + margin + (usableWidth - totalHolesWidth) / 2f

        repeat(holeCount) {
            canvas.drawRoundRect(
                RectF(
                    x,
                    centerY - TAPED_FILM_SPROCKET_SIZE / 2f,
                    x + TAPED_FILM_SPROCKET_SIZE,
                    centerY + TAPED_FILM_SPROCKET_SIZE / 2f
                ),
                6f,
                6f,
                holePaint
            )
            x += step
        }
    }

    private fun drawMaskingTape(
        canvas: Canvas,
        centerX: Float,
        centerY: Float
    ) {
        val tapeBounds =
            RectF(
                centerX - TAPED_FILM_TAPE_WIDTH / 2f,
                centerY - TAPED_FILM_TAPE_HEIGHT / 2f,
                centerX + TAPED_FILM_TAPE_WIDTH / 2f,
                centerY + TAPED_FILM_TAPE_HEIGHT / 2f
            )
        val tapePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(150, 214, 201, 178)
                style = Paint.Style.FILL
            }

        canvas.drawRoundRect(tapeBounds, 4f, 4f, tapePaint)

        val tapeEdgePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(90, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

        canvas.drawRoundRect(tapeBounds, 4f, 4f, tapeEdgePaint)
    }

    private fun drawLetterPhoto(
        canvas: Canvas,
        sourceBitmap: Bitmap,
        photoBounds: RectF,
        edgeBlur: Float = 0f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        zoom: Float = 1f
    ) {
        val shadowBounds =
            RectF(
                photoBounds.left + 14f,
                photoBounds.top + 18f,
                photoBounds.right + 14f,
                photoBounds.bottom + 18f
            )
        val shadowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(55, 18, 12, 28)
                style = Paint.Style.FILL
            }

        canvas.drawRect(shadowBounds, shadowPaint)

        canvas.save()
        canvas.clipRect(photoBounds)
        drawCenterCroppedBitmap(
            canvas = canvas,
            bitmap = sourceBitmap,
            destinationRect = photoBounds,
            edgeBlur = edgeBlur,
            offsetX = offsetX,
            offsetY = offsetY,
            zoom = zoom
        )
        canvas.restore()

        val borderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(210, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }

        canvas.drawRect(photoBounds, borderPaint)
    }

    /** 사진과 문구 사이의 얇은 구분선. 오래된 편지지처럼 은은한 갈색 계열로 그린다. */
    private fun drawLetterDivider(
        canvas: Canvas,
        dividerY: Float,
        left: Float,
        right: Float
    ) {
        val dividerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(130, 96, 76, 64)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

        canvas.drawLine(left, dividerY, right, dividerY, dividerPaint)
    }

    private fun drawMessage(
        canvas: Canvas,
        message: String,
        messageFont: String,
        messagePanel: RectF,
        darkOverlay: Boolean = false,
        compact: Boolean = false,
        textScale: Float = 1f
    ) {
        val normalizedMessage =
            message.trim()

        if (normalizedMessage.isBlank()) {
            return
        }

        if (!darkOverlay) {
            canvas.drawRoundRect(
                RectF(
                    messagePanel.left + 12f,
                    messagePanel.top + 14f,
                    messagePanel.right + 12f,
                    messagePanel.bottom + 14f
                ),
                34f,
                34f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(55, 20, 14, 26)
                    style = Paint.Style.FILL
                }
            )
        }

        canvas.drawRoundRect(
            messagePanel,
            34f,
            34f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    if (darkOverlay) {
                        Color.argb(188, 24, 18, 30)
                    } else {
                        Color.argb(222, 255, 252, 247)
                    }
                style = Paint.Style.FILL
            }
        )

        val baseTextSize =
            when {
                compact && normalizedMessage.length <= 20 -> 50f
                compact && normalizedMessage.length <= 45 -> 46f
                compact -> 40f
                normalizedMessage.length <= 20 -> 62f
                normalizedMessage.length <= 45 -> 56f
                normalizedMessage.length <= 75 -> 50f
                else -> 44f
            }
        val clampedTextScale =
            textScale.coerceIn(0.6f, 1.4f)
        val textSize =
            baseTextSize * clampedTextScale
        val textPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    if (darkOverlay) {
                        Color.WHITE
                    } else {
                        Color.rgb(47, 37, 43)
                    }
                this.textSize = textSize
                typeface = resolveMessageTypeface(messageFont)
            }
        val horizontalPadding =
            if (compact) 100f else 130f
        val textWidth =
            (messagePanel.width() - horizontalPadding)
                .toInt()
                .coerceAtLeast(1)
        val lineSpacingExtra =
            if (compact) 7f else 10f
        val existingMaxLines =
            if (compact) 3 else 4

        val measureLayout =
            StaticLayout.Builder
                .obtain(
                    normalizedMessage,
                    0,
                    normalizedMessage.length,
                    textPaint,
                    textWidth
                )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(lineSpacingExtra, 1.08f)
                .build()
        val lineHeight =
            if (measureLayout.lineCount > 0) {
                (
                    measureLayout.getLineBottom(0) -
                            measureLayout.getLineTop(0)
                ).toFloat()
            } else {
                textSize * 1.2f
            }.coerceAtLeast(1f)

        val baseIndicatorSize =
            if (compact) 34f else 40f
        val indicatorSize =
            baseIndicatorSize * clampedTextScale
        val verticalPadding = 8f

        val availableHeightForBody =
            (
                messagePanel.height() -
                        verticalPadding * 2f
            ).coerceAtLeast(lineHeight)
        val maxVisibleLines =
            (availableHeightForBody / lineHeight)
                .toInt()
                .coerceAtLeast(1)
        val finalMaxLines =
            minOf(existingMaxLines, maxVisibleLines)

        val textLayout =
            StaticLayout.Builder
                .obtain(
                    normalizedMessage,
                    0,
                    normalizedMessage.length,
                    textPaint,
                    textWidth
                )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(lineSpacingExtra, 1.08f)
                .setMaxLines(finalMaxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setEllipsizedWidth(textWidth)
                .build()

        val bodyTop =
            messagePanel.top + verticalPadding
        val bodyBottom =
            (
                messagePanel.bottom - verticalPadding
            ).coerceAtLeast(bodyTop + lineHeight)

        val textX =
            messagePanel.centerX() - textWidth / 2f
        val textY =
            (bodyTop + bodyBottom) / 2f -
                    textLayout.height / 2f

        canvas.save()
        canvas.clipRect(
            messagePanel.left,
            bodyTop,
            messagePanel.right,
            bodyBottom
        )
        canvas.translate(textX, textY)
        textLayout.draw(canvas)
        canvas.restore()

        val indicatorPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    if (darkOverlay) {
                        Color.WHITE
                    } else {
                        Color.rgb(47, 37, 43)
                    }
                this.textSize = indicatorSize
                textAlign = Paint.Align.RIGHT
            }

        canvas.drawText(
            "▼",
            messagePanel.right - 30f,
            messagePanel.bottom - 26f,
            indicatorPaint
        )
    }

    private fun drawDate(
        canvas: Canvas,
        capturedAt: Long,
        dateFormat: String,
        datePanel: RectF,
        compact: Boolean = false,
        textScale: Float = 1f
    ) {
        val dateText =
            formatDate(
                capturedAt = capturedAt,
                dateFormat = dateFormat
            )
        val panelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(205, 255, 252, 247)
                style = Paint.Style.FILL
            }

        canvas.drawRoundRect(
            datePanel,
            if (compact) 22f else 28f,
            if (compact) 22f else 28f,
            panelPaint
        )

        val baseDateTextSize =
            if (compact) 30f else 34f
        var renderTextSize =
            baseDateTextSize *
                    textScale.coerceIn(0.6f, 1.8f)
        val dateHorizontalPadding =
            if (compact) 32f else 40f
        val availableDateWidth =
            (datePanel.width() - dateHorizontalPadding)
                .coerceAtLeast(1f)
        val measuredWidth =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = renderTextSize
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }.measureText(dateText)

        if (measuredWidth > availableDateWidth) {
            renderTextSize *= availableDateWidth / measuredWidth
        }

        val datePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(70, 58, 68)
                textSize = renderTextSize
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
        val metrics =
            datePaint.fontMetrics
        val baseline =
            datePanel.centerY() -
                    (metrics.ascent + metrics.descent) / 2f

        canvas.save()
        canvas.clipRect(datePanel)
        canvas.drawText(
            dateText,
            datePanel.centerX(),
            baseline,
            datePaint
        )
        canvas.restore()
    }

    private fun formatDate(
        capturedAt: Long,
        dateFormat: String
    ): String {
        val resolvedFormat =
            PostcardDateFormat
                .entries
                .firstOrNull { entry -> entry.name == dateFormat }
                ?: PostcardDateFormat.DOT

        return resolvedFormat.format(capturedAt)
    }

    private fun resolveMessageTypeface(
        messageFont: String
    ): Typeface {
        return when (messageFont) {
            "DEFAULT" ->
                Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            "SANS_SERIF" ->
                Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

            "SERIF" ->
                Typeface.create(Typeface.SERIF, Typeface.NORMAL)

            "MONOSPACE" ->
                Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

            "CURSIVE" ->
                Typeface.create("cursive", Typeface.NORMAL)

            else ->
                Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
    }

    private fun getPatternColor(
        backgroundColorArgb: Long
    ): Int {
        val color = backgroundColorArgb.toInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val brightness =
            (red * 0.299f + green * 0.587f + blue * 0.114f) / 255f

        return if (brightness < 0.48f) {
            Color.argb(87, 255, 255, 255)
        } else {
            Color.argb(46, 61, 41, 88)
        }
    }

    private fun drawCheckerPattern(
        canvas: Canvas,
        color: Int,
        density: Float = 1f
    ) {
        val tileSize = 175f / density
        val horizontalCount = (LOGICAL_SIZE / tileSize).toInt() + 2
        val verticalCount = (LOGICAL_SIZE / tileSize).toInt() + 2
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }

        for (row in 0..verticalCount) {
            for (column in 0..horizontalCount) {
                if ((row + column) % 2 == 0) {
                    canvas.drawRect(
                        column * tileSize,
                        row * tileSize,
                        (column + 1) * tileSize,
                        (row + 1) * tileSize,
                        paint
                    )
                }
            }
        }
    }

    private fun rotateBitmapUsingExif(
        bitmap: Bitmap,
        orientation: Int
    ): Bitmap {
        val matrix =
            android.graphics.Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
                matrix.setScale(-1f, 1f)

            ExifInterface.ORIENTATION_ROTATE_180 ->
                matrix.setRotate(180f)

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 ->
                matrix.setRotate(90f)

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 ->
                matrix.setRotate(270f)

            else -> return bitmap
        }

        val rotatedBitmap =
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }

        return rotatedBitmap
    }
}
