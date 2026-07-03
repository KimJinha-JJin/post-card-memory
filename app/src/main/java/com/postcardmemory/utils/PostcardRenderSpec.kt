package com.postcardmemory.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object PostcardRenderSpec {
    const val LOGICAL_SIZE = 2048f
    const val OUTPUT_SIZE = 2048
    const val STAMP_BORDER_WIDTH = 18f

    enum class LayoutStyle {
        STANDARD,
        PHOTO_FOCUS,
        AIRY,
        MAGAZINE
    }

    data class RenderLayout(
        val stampBounds: RectF,
        val messagePanel: RectF,
        val datePanel: RectF,
        val compactMessage: Boolean = false,
        val compactDate: Boolean = false,
        val darkMessageOverlay: Boolean = false
    )

    fun resolveLayoutStyle(
        layoutStyle: String
    ): LayoutStyle {
        return when (layoutStyle) {
            "PHOTO_FOCUS" -> LayoutStyle.PHOTO_FOCUS
            "AIRY" -> LayoutStyle.AIRY
            "MAGAZINE" -> LayoutStyle.MAGAZINE
            else -> LayoutStyle.STANDARD
        }
    }

    fun layoutFor(
        layoutStyle: String
    ): RenderLayout {
        return when (resolveLayoutStyle(layoutStyle)) {
            LayoutStyle.STANDARD ->
                RenderLayout(
                    stampBounds = RectF(394f, 180f, 1654f, 1440f),
                    messagePanel = RectF(220f, 1505f, 1828f, 1748f),
                    datePanel = RectF(544f, 1846f, 1504f, 1932f)
                )

            LayoutStyle.PHOTO_FOCUS ->
                RenderLayout(
                    stampBounds = RectF(264f, 110f, 1784f, 1630f),
                    messagePanel = RectF(250f, 1670f, 1798f, 1838f),
                    datePanel = RectF(574f, 1900f, 1474f, 1972f),
                    compactMessage = true,
                    compactDate = true
                )

            LayoutStyle.AIRY ->
                RenderLayout(
                    stampBounds = RectF(534f, 250f, 1514f, 1230f),
                    messagePanel = RectF(320f, 1390f, 1728f, 1690f),
                    datePanel = RectF(544f, 1810f, 1504f, 1896f)
                )

            LayoutStyle.MAGAZINE ->
                RenderLayout(
                    stampBounds = RectF(194f, 120f, 1854f, 1780f),
                    messagePanel = RectF(270f, 1370f, 1778f, 1660f),
                    datePanel = RectF(544f, 1846f, 1504f, 1932f),
                    darkMessageOverlay = true
                )
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
        targetSize: Float = LOGICAL_SIZE
    ) {
        val scale =
            targetSize / LOGICAL_SIZE

        canvas.save()
        canvas.scale(scale, scale)

        val layout =
            layoutFor(layoutStyle)

        drawBackground(
            canvas = canvas,
            backgroundColorArgb = backgroundColorArgb,
            backgroundPattern = backgroundPattern
        )
        drawStampPhoto(
            canvas = canvas,
            sourceBitmap = sourceBitmap,
            stampBounds = layout.stampBounds
        )
        drawMessage(
            canvas = canvas,
            message = message,
            messageFont = messageFont,
            messagePanel = layout.messagePanel,
            darkOverlay = layout.darkMessageOverlay,
            compact = layout.compactMessage
        )
        drawDate(
            canvas = canvas,
            capturedAt = capturedAt,
            dateFormat = dateFormat,
            datePanel = layout.datePanel,
            compact = layout.compactDate
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
        destinationRect: RectF
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

        val sourceRect =
            if (sourceRatio > destinationRatio) {
                val croppedWidth =
                    sourceHeight * destinationRatio
                val left =
                    (sourceWidth - croppedWidth) / 2f

                Rect(
                    left.toInt(),
                    0,
                    (left + croppedWidth).toInt(),
                    sourceHeight.toInt()
                )
            } else {
                val croppedHeight =
                    sourceWidth / destinationRatio
                val top =
                    (sourceHeight - croppedHeight) / 2f

                Rect(
                    0,
                    top.toInt(),
                    sourceWidth.toInt(),
                    (top + croppedHeight).toInt()
                )
            }

        canvas.drawBitmap(
            bitmap,
            sourceRect,
            destinationRect,
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )
        )
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
        backgroundPattern: String
    ) {
        canvas.drawColor(backgroundColorArgb.toInt())

        drawBackgroundPattern(
            canvas = canvas,
            backgroundPattern = backgroundPattern,
            backgroundColorArgb = backgroundColorArgb
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
        backgroundColorArgb: Long
    ) {
        if (backgroundPattern == "NONE") {
            return
        }

        val patternColor =
            getPatternColor(backgroundColorArgb)

        if (backgroundPattern == "CHECKER") {
            drawCheckerPattern(canvas, patternColor)
            return
        }

        val cellSize = 290f
        val horizontalCount = (LOGICAL_SIZE / cellSize).toInt() + 3
        val verticalCount = (LOGICAL_SIZE / cellSize).toInt() + 3

        for (row in -1..verticalCount) {
            val staggerOffset =
                if (row % 2 == 0) 0f else cellSize / 2f

            for (column in -1..horizontalCount) {
                val centerX = column * cellSize + staggerOffset
                val centerY = row * cellSize

                when (backgroundPattern) {
                    "DOTS" ->
                        canvas.drawCircle(
                            centerX,
                            centerY,
                            if ((row + column) % 2 == 0) 34f else 22f,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = patternColor
                                style = Paint.Style.FILL
                            }
                        )

                    "STARS" ->
                        canvas.drawPath(
                            createStarPath(centerX, centerY, 62f, 27f),
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = patternColor
                                style = Paint.Style.FILL
                            }
                        )

                    "HEARTS" ->
                        canvas.drawPath(
                            createHeartPath(centerX, centerY, 56f),
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = patternColor
                                style = Paint.Style.FILL
                            }
                        )

                    "CHERRY_BLOSSOMS" ->
                        drawCherryBlossomPattern(
                            canvas = canvas,
                            centerX = centerX,
                            centerY = centerY,
                            radius = 52f,
                            color = patternColor
                        )

                    "TRIANGLES" ->
                        canvas.drawPath(
                            Path().apply {
                                moveTo(centerX, centerY - 56f)
                                lineTo(centerX - 51f, centerY + 42f)
                                lineTo(centerX + 51f, centerY + 42f)
                                close()
                            },
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = patternColor
                                style = Paint.Style.STROKE
                                strokeWidth = 13f
                                strokeJoin = Paint.Join.ROUND
                            }
                        )

                    "SQUARES" -> {
                        canvas.save()
                        if ((row + column) % 2 != 0) {
                            canvas.rotate(45f, centerX, centerY)
                        }
                        canvas.drawRoundRect(
                            RectF(
                                centerX - 48f,
                                centerY - 48f,
                                centerX + 48f,
                                centerY + 48f
                            ),
                            14f,
                            14f,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = patternColor
                                style = Paint.Style.STROKE
                                strokeWidth = 12f
                                strokeJoin = Paint.Join.ROUND
                            }
                        )
                        canvas.restore()
                    }
                }
            }
        }
    }

    private fun drawStampPhoto(
        canvas: Canvas,
        sourceBitmap: Bitmap,
        stampBounds: RectF
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
            destinationRect = stampBounds
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

    private fun drawMessage(
        canvas: Canvas,
        message: String,
        messageFont: String,
        messagePanel: RectF,
        darkOverlay: Boolean = false,
        compact: Boolean = false
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

        val textSize =
            when {
                compact && normalizedMessage.length <= 20 -> 50f
                compact && normalizedMessage.length <= 45 -> 46f
                compact -> 40f
                normalizedMessage.length <= 20 -> 62f
                normalizedMessage.length <= 45 -> 56f
                normalizedMessage.length <= 75 -> 50f
                else -> 44f
            }
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
                .setLineSpacing(
                    if (compact) 7f else 10f,
                    1.08f
                )
                .setMaxLines(if (compact) 3 else 4)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setEllipsizedWidth(textWidth)
                .build()
        val textX =
            messagePanel.centerX() - textWidth / 2f
        val textY =
            messagePanel.centerY() - textLayout.height / 2f

        canvas.save()
        canvas.translate(textX, textY)
        textLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawDate(
        canvas: Canvas,
        capturedAt: Long,
        dateFormat: String,
        datePanel: RectF,
        compact: Boolean = false
    ) {
        val dateText =
            formatDate(
                capturedAt = capturedAt,
                dateFormat = dateFormat
            )
        val datePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(70, 58, 68)
                textSize = if (compact) 30f else 34f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
        val metrics =
            datePaint.fontMetrics
        val baseline =
            datePanel.centerY() -
                    (metrics.ascent + metrics.descent) / 2f

        canvas.drawText(
            dateText,
            datePanel.centerX(),
            baseline,
            datePaint
        )
    }

    private fun formatDate(
        capturedAt: Long,
        dateFormat: String
    ): String {
        val pattern: String
        val locale: Locale
        val uppercase: Boolean

        when (dateFormat) {
            "KOREAN" -> {
                pattern = "yyyy년 M월 d일"
                locale = Locale.KOREAN
                uppercase = false
            }

            "ENGLISH_LONG" -> {
                pattern = "MMMM d, yyyy"
                locale = Locale.ENGLISH
                uppercase = true
            }

            "ENGLISH_SHORT" -> {
                pattern = "dd MMM yyyy"
                locale = Locale.ENGLISH
                uppercase = true
            }

            else -> {
                pattern = "yyyy.MM.dd"
                locale = Locale.KOREAN
                uppercase = false
            }
        }

        val formattedDate =
            SimpleDateFormat(pattern, locale)
                .format(Date(capturedAt))

        return if (uppercase) {
            formattedDate.uppercase(locale)
        } else {
            formattedDate
        }
    }

    private fun resolveMessageTypeface(
        messageFont: String
    ): Typeface {
        return when (messageFont) {
            "DEFAULT" -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            "SANS_SERIF" -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            "SERIF" -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            "MONOSPACE" -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            "CURSIVE" -> Typeface.create("cursive", Typeface.NORMAL)
            else -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
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
        color: Int
    ) {
        val tileSize = 175f
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

    private fun drawCherryBlossomPattern(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        color: Int
    ) {
        val petalPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }

        repeat(5) { index ->
            canvas.save()
            canvas.rotate(index * 72f, centerX, centerY)
            canvas.drawOval(
                RectF(
                    centerX - radius * 0.42f,
                    centerY - radius * 1.08f,
                    centerX + radius * 0.42f,
                    centerY + radius * 0.12f
                ),
                petalPaint
            )
            canvas.restore()
        }

        canvas.drawCircle(
            centerX,
            centerY,
            radius * 0.27f,
            petalPaint
        )
    }

    private fun createStarPath(
        centerX: Float,
        centerY: Float,
        outerRadius: Float,
        innerRadius: Float
    ): Path {
        return Path().apply {
            repeat(10) { index ->
                val radius =
                    if (index % 2 == 0) outerRadius else innerRadius
                val angle =
                    -PI / 2.0 + index * PI / 5.0
                val pointX =
                    centerX + cos(angle).toFloat() * radius
                val pointY =
                    centerY + sin(angle).toFloat() * radius

                if (index == 0) {
                    moveTo(pointX, pointY)
                } else {
                    lineTo(pointX, pointY)
                }
            }

            close()
        }
    }

    private fun createHeartPath(
        centerX: Float,
        centerY: Float,
        radius: Float
    ): Path {
        return Path().apply {
            moveTo(centerX, centerY + radius)
            cubicTo(
                centerX - radius * 1.35f,
                centerY + radius * 0.2f,
                centerX - radius,
                centerY - radius * 0.95f,
                centerX,
                centerY - radius * 0.28f
            )
            cubicTo(
                centerX + radius,
                centerY - radius * 0.95f,
                centerX + radius * 1.35f,
                centerY + radius * 0.2f,
                centerX,
                centerY + radius
            )
            close()
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
