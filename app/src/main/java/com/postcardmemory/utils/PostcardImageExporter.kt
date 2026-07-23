package com.postcardmemory.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.exifinterface.media.ExifInterface
import com.postcardmemory.R
import com.postcardmemory.data.Postcard
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object PostcardImageExporter {

    private const val OUTPUT_SIZE = 2048
    private const val SHARE_CACHE_DIR_NAME = "shared_postcards"

    /**
     * 공유 캐시 보관 정책. 엽서 PNG는 몇 MB 수준이라 24시간·20개면 기기
     * 저장공간에 부담이 거의 없으면서도, chooser에서 오래 머물거나 곧바로
     * 재공유하는 정상적인 사용 패턴에서 파일이 사라지는 일은 없을 만큼
     * 충분히 넉넉하다.
     */
    private const val SHARE_CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000L
    private const val SHARE_CACHE_MAX_FILES = 20
    private const val STAMP_BORDER_WIDTH = 18f
    private const val STICKER_CORNER_RADIUS_RATIO =
        16f / 120f
    private const val STICKER_BORDER_WIDTH_RATIO =
        3f / 120f

    private enum class ExportLayoutStyle {
        STANDARD,
        PHOTO_FOCUS,
        AIRY,
        MAGAZINE
    }

    data class StickerOverlay(
        val uri: Uri,
        val normalizedX: Float,
        val normalizedY: Float,
        val sizeRatio: Float,
        val originalUri: Uri? = null,
        val isBackgroundRemoved: Boolean = false,
        val rotationDegrees: Float = 0f,
        val flipHorizontal: Boolean = false,
        val flipVertical: Boolean = false
    )

    data class SealOverlay(
        val type: String,
        val normalizedX: Float,
        val normalizedY: Float,
        val sizeRatio: Float,
        val rotationDegrees: Float = 0f,
        val colorArgb: Long,
        val capturedAtMillis: Long? = null
    )

    fun exportToGallery(
        context: Context,
        postcard: Postcard,
        stickerOverlays: List<StickerOverlay> = emptyList(),
        sealOverlays: List<SealOverlay> = emptyList()
    ): Result<Uri> {
        return runCatching {
            val outputBitmap =
                createPostcardBitmap(
                    context = context,
                    postcard = postcard,
                    stickerOverlays = stickerOverlays,
                    sealOverlays = sealOverlays
                )

            try {
                saveBitmapToGallery(
                    context = context,
                    bitmap = outputBitmap
                )
            } finally {
                if (!outputBitmap.isRecycled) {
                    outputBitmap.recycle()
                }
            }
        }
    }

    fun exportForSharing(
        context: Context,
        postcard: Postcard,
        stickerOverlays: List<StickerOverlay> = emptyList(),
        sealOverlays: List<SealOverlay> = emptyList()
    ): Result<File> {
        return runCatching {
            val outputBitmap =
                createPostcardBitmap(
                    context = context,
                    postcard = postcard,
                    stickerOverlays = stickerOverlays,
                    sealOverlays = sealOverlays
                )

            try {
                saveBitmapForSharing(
                    context = context,
                    postcardId = postcard.id,
                    bitmap = outputBitmap
                )
            } finally {
                if (!outputBitmap.isRecycled) {
                    outputBitmap.recycle()
                }
            }
        }
    }

    /**
     * 초 단위 timestamp만 쓰면 같은 초에 두 번 공유할 때 파일명이 완전히
     * 같아져 두 번째 공유가 첫 번째 공유 파일을 덮어써 버린다. postcardId +
     * 밀리초 + UUID 조합이라 같은 밀리초에 호출돼도 사실상 충돌하지 않는다.
     */
    internal fun shareFileNameFor(
        postcardId: Long,
        timestampMillis: Long = System.currentTimeMillis(),
        uuid: String = UUID.randomUUID().toString()
    ): String = "postcard_${postcardId}_${timestampMillis}_$uuid.png"

    private fun saveBitmapForSharing(
        context: Context,
        postcardId: Long,
        bitmap: Bitmap
    ): File {
        val shareDir =
            File(context.cacheDir, SHARE_CACHE_DIR_NAME)

        if (
            !shareDir.exists() &&
            !shareDir.mkdirs()
        ) {
            throw IOException(
                "공유 폴더를 만들지 못했습니다."
            )
        }

        val shareFileName =
            shareFileNameFor(postcardId = postcardId)
        val shareFile =
            File(shareDir, shareFileName)

        try {
            FileOutputStream(shareFile).use { outputStream ->
                val saved =
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        outputStream
                    )

                if (!saved) {
                    throw IOException(
                        "공유용 이미지 파일 저장에 실패했습니다."
                    )
                }
            }

            if (
                !shareFile.exists() ||
                shareFile.length() == 0L
            ) {
                throw IOException(
                    "공유용 이미지 파일이 비어 있습니다."
                )
            }
        } catch (exception: Exception) {
            // 실패한 빈/부분 파일을 캐시에 남기지 않는다.
            if (shareFile.exists()) {
                shareFile.delete()
            }
            throw exception
        }

        // 방금 만든 파일은 절대 정리 대상에 포함하지 않는다. chooser가 아직
        // 열려 있거나 외부 앱이 방금 넘긴 직전 공유 파일을 읽는 중일 수
        // 있으므로, 여기서 "현재 파일 이름과 다르면 전부 삭제" 같은 즉시
        // 전체 삭제는 하지 않고 TTL·최대 보관 개수 정책으로만 제한적으로
        // 정리한다. 정리 실패는 이번 공유 성공 여부에 영향을 주지 않는다.
        runCatching {
            cleanupOldShareFiles(
                shareDir = shareDir,
                currentFileName = shareFileName
            )
        }

        return shareFile
    }

    /**
     * TTL을 넘긴 공유 캐시 파일을 지우고, TTL 이내라도 개수가 너무 많으면
     * 오래된 파일부터 최대 보관 개수만큼만 남긴다. currentFileName(이번
     * 공유로 방금 생성한 파일)은 항상 제외한다. internal은 순수 JUnit
     * 테스트를 위함(같은 패키지에서 TemporaryFolder로 검증).
     */
    internal fun cleanupOldShareFiles(
        shareDir: File,
        currentFileName: String,
        ttlMillis: Long = SHARE_CACHE_TTL_MILLIS,
        maxFiles: Int = SHARE_CACHE_MAX_FILES,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val otherFiles =
            shareDir.listFiles { file ->
                file.isFile && file.name != currentFileName
            } ?: return

        val (expired, fresh) =
            otherFiles.partition { file ->
                nowMillis - file.lastModified() > ttlMillis
            }

        expired.forEach { file ->
            runCatching { file.delete() }
        }

        fresh
            .sortedByDescending { file -> file.lastModified() }
            .drop(maxFiles)
            .forEach { file ->
                runCatching { file.delete() }
            }
    }

    private fun createPostcardBitmap(
        context: Context,
        postcard: Postcard,
        stickerOverlays: List<StickerOverlay>,
        sealOverlays: List<SealOverlay> = emptyList()
    ): Bitmap {
        val sourceFile =
            File(postcard.imagePath)

        if (!sourceFile.exists()) {
            throw IOException(
                "원본 사진 파일을 찾지 못했습니다."
            )
        }

        val sourceBitmap =
            PostcardRenderSpec.decodeSourceBitmap(
                sourceFile
            )

        try {
            val outputBitmap =
                Bitmap.createBitmap(
                    OUTPUT_SIZE,
                    OUTPUT_SIZE,
                    Bitmap.Config.ARGB_8888
                )

            val canvas =
                Canvas(outputBitmap)

            PostcardRenderSpec.drawBaseContent(
                canvas = canvas,
                sourceBitmap = sourceBitmap,
                backgroundColorArgb =
                    postcard.backgroundColorArgb,
                backgroundPattern =
                    postcard.backgroundPattern,
                message = postcard.message,
                messageFont = postcard.messageFont,
                layoutStyle = postcard.layoutStyle,
                capturedAt = postcard.capturedAt,
                dateFormat = postcard.dateFormat,
                targetSize = OUTPUT_SIZE.toFloat(),
                messageTextScale = postcard.messageTextScale,
                dateTextScale = postcard.dateTextScale,
                backgroundPatternDensity = postcard.backgroundPatternDensity,
                stampPhotoScale = postcard.stampPhotoScale,
                polaroidPhotoScale = postcard.polaroidPhotoScale,
                photoEdgeBlur = postcard.photoEdgeBlur,
                stampPhotoOffsetX = postcard.stampPhotoOffsetX,
                stampPhotoOffsetY = postcard.stampPhotoOffsetY,
                polaroidPhotoOffsetX = postcard.polaroidPhotoOffsetX,
                polaroidPhotoOffsetY = postcard.polaroidPhotoOffsetY,
                tapedFilmPhotoOffsetX = postcard.tapedFilmPhotoOffsetX,
                tapedFilmPhotoOffsetY = postcard.tapedFilmPhotoOffsetY,
                stampPhotoZoom = postcard.stampPhotoZoom,
                polaroidPhotoZoom = postcard.polaroidPhotoZoom,
                tapedFilmPhotoZoom = postcard.tapedFilmPhotoZoom
            )

            for (overlay in stickerOverlays) {
                drawStickerOverlay(
                    context = context,
                    canvas = canvas,
                    stickerOverlay = overlay
                )
            }

            for (overlay in sealOverlays) {
                drawSealOverlay(
                    context = context,
                    canvas = canvas,
                    sealOverlay = overlay
                )
            }

            return outputBitmap
        } finally {
            if (!sourceBitmap.isRecycled) {
                sourceBitmap.recycle()
            }
        }
    }

    private fun resolveLayoutStyle(
        layoutStyle: String
    ): ExportLayoutStyle {
        return when (layoutStyle) {
            "PHOTO_FOCUS" ->
                ExportLayoutStyle.PHOTO_FOCUS

            "AIRY" ->
                ExportLayoutStyle.AIRY

            "MAGAZINE" ->
                ExportLayoutStyle.MAGAZINE

            else ->
                ExportLayoutStyle.STANDARD
        }
    }

    private fun resolveMessageTypeface(
        messageFont: String
    ): Typeface {
        return when (messageFont) {
            "DEFAULT" ->
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.NORMAL
                )

            "SANS_SERIF" ->
                Typeface.create(
                    Typeface.SANS_SERIF,
                    Typeface.NORMAL
                )

            "SERIF" ->
                Typeface.create(
                    Typeface.SERIF,
                    Typeface.NORMAL
                )

            "MONOSPACE" ->
                Typeface.create(
                    Typeface.MONOSPACE,
                    Typeface.NORMAL
                )

            "CURSIVE" ->
                Typeface.create(
                    "cursive",
                    Typeface.NORMAL
                )

            else ->
                Typeface.create(
                    Typeface.SERIF,
                    Typeface.NORMAL
                )
        }
    }

    private fun drawBackground(
        canvas: Canvas,
        postcard: Postcard
    ) {
        canvas.drawColor(
            postcard.backgroundColorArgb.toInt()
        )

        drawBackgroundPattern(
            canvas = canvas,
            backgroundPattern = postcard.backgroundPattern,
            backgroundColorArgb = postcard.backgroundColorArgb
        )

        val innerBorderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    Color.argb(
                        115,
                        255,
                        255,
                        255
                    )

                style =
                    Paint.Style.STROKE

                strokeWidth = 8f
            }

        canvas.drawRect(
            32f,
            32f,
            OUTPUT_SIZE - 32f,
            OUTPUT_SIZE - 32f,
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
            getPatternColor(
                backgroundColorArgb
            )

        if (backgroundPattern == "CHECKER") {
            drawCheckerPattern(
                canvas = canvas,
                color = patternColor
            )
            return
        }

        val cellSize = 290f
        val horizontalCount =
            (OUTPUT_SIZE / cellSize).toInt() + 3
        val verticalCount =
            (OUTPUT_SIZE / cellSize).toInt() + 3

        for (row in -1..verticalCount) {
            val staggerOffset =
                if (row % 2 == 0) {
                    0f
                } else {
                    cellSize / 2f
                }

            for (column in -1..horizontalCount) {
                val centerX =
                    column * cellSize + staggerOffset
                val centerY =
                    row * cellSize

                when (backgroundPattern) {
                    "DOTS" -> {
                        val radius =
                            if ((row + column) % 2 == 0) {
                                34f
                            } else {
                                22f
                            }

                        canvas.drawCircle(
                            centerX,
                            centerY,
                            radius,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = patternColor
                                style = Paint.Style.FILL
                            }
                        )
                    }

                    "STARS" -> {
                        canvas.drawPath(
                            createStarPath(
                                centerX = centerX,
                                centerY = centerY,
                                outerRadius = 62f,
                                innerRadius = 27f
                            ),
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = patternColor
                                style = Paint.Style.FILL
                            }
                        )
                    }

                    "HEARTS" -> {
                        canvas.drawPath(
                            createHeartPath(
                                centerX = centerX,
                                centerY = centerY,
                                radius = 56f
                            ),
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = patternColor
                                style = Paint.Style.FILL
                            }
                        )
                    }

                    "CHERRY_BLOSSOMS" -> {
                        drawCherryBlossomPattern(
                            canvas = canvas,
                            centerX = centerX,
                            centerY = centerY,
                            radius = 52f,
                            color = patternColor
                        )
                    }

                    "TRIANGLES" -> {
                        val trianglePath =
                            Path().apply {
                                moveTo(
                                    centerX,
                                    centerY - 56f
                                )
                                lineTo(
                                    centerX - 51f,
                                    centerY + 42f
                                )
                                lineTo(
                                    centerX + 51f,
                                    centerY + 42f
                                )
                                close()
                            }

                        canvas.drawPath(
                            trianglePath,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = patternColor
                                style = Paint.Style.STROKE
                                strokeWidth = 13f
                                strokeJoin = Paint.Join.ROUND
                            }
                        )
                    }

                    "SQUARES" -> {
                        canvas.save()

                        if ((row + column) % 2 != 0) {
                            canvas.rotate(
                                45f,
                                centerX,
                                centerY
                            )
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

    private fun drawCheckerPattern(
        canvas: Canvas,
        color: Int
    ) {
        val tileSize = 175f
        val horizontalCount =
            (OUTPUT_SIZE / tileSize).toInt() + 2
        val verticalCount =
            (OUTPUT_SIZE / tileSize).toInt() + 2

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
            canvas.rotate(
                index * 72f,
                centerX,
                centerY
            )
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
                    if (index % 2 == 0) {
                        outerRadius
                    } else {
                        innerRadius
                    }

                val angle =
                    -PI / 2.0 + index * PI / 5.0

                val pointX =
                    centerX +
                            cos(angle).toFloat() * radius

                val pointY =
                    centerY +
                            sin(angle).toFloat() * radius

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
            moveTo(
                centerX,
                centerY + radius
            )

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

    private fun getPatternColor(
        backgroundColorArgb: Long
    ): Int {
        val color =
            backgroundColorArgb.toInt()

        val red =
            Color.red(color)

        val green =
            Color.green(color)

        val blue =
            Color.blue(color)

        val brightness =
            (
                    red * 0.299f +
                            green * 0.587f +
                            blue * 0.114f
                    ) / 255f

        return if (brightness < 0.48f) {
            Color.argb(
                87,
                255,
                255,
                255
            )
        } else {
            Color.argb(
                46,
                61,
                41,
                88
            )
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
                color =
                    Color.argb(
                        85,
                        18,
                        12,
                        28
                    )

                style =
                    Paint.Style.FILL
            }

        canvas.drawPath(
            shadowPath,
            shadowPaint
        )

        canvas.save()

        canvas.clipPath(
            stampPath
        )

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

        canvas.drawPath(
            stampPath,
            whiteBorderPaint
        )
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
            val panelShadowPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color =
                        Color.argb(
                            55,
                            20,
                            14,
                            26
                        )

                    style =
                        Paint.Style.FILL
                }

            canvas.drawRoundRect(
                RectF(
                    messagePanel.left + 12f,
                    messagePanel.top + 14f,
                    messagePanel.right + 12f,
                    messagePanel.bottom + 14f
                ),
                34f,
                34f,
                panelShadowPaint
            )
        }

        val panelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    if (darkOverlay) {
                        Color.argb(
                            188,
                            24,
                            18,
                            30
                        )
                    } else {
                        Color.argb(
                            222,
                            255,
                            252,
                            247
                        )
                    }

                style =
                    Paint.Style.FILL
            }

        canvas.drawRoundRect(
            messagePanel,
            34f,
            34f,
            panelPaint
        )

        val textSize =
            when {
                compact &&
                        normalizedMessage.length <= 20 ->
                    50f

                compact &&
                        normalizedMessage.length <= 45 ->
                    46f

                compact ->
                    40f

                normalizedMessage.length <= 20 ->
                    62f

                normalizedMessage.length <= 45 ->
                    56f

                normalizedMessage.length <= 75 ->
                    50f

                else ->
                    44f
            }

        val textPaint =
            TextPaint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    if (darkOverlay) {
                        Color.WHITE
                    } else {
                        Color.rgb(
                            47,
                            37,
                            43
                        )
                    }

                this.textSize =
                    textSize

                typeface =
                    resolveMessageTypeface(
                        messageFont
                    )
            }

        val horizontalPadding =
            if (compact) {
                100f
            } else {
                130f
            }

        val textWidth =
            (
                    messagePanel.width() -
                            horizontalPadding
                    )
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
                .setAlignment(
                    Layout.Alignment.ALIGN_CENTER
                )
                .setIncludePad(false)
                .setLineSpacing(
                    if (compact) {
                        7f
                    } else {
                        10f
                    },
                    1.08f
                )
                .setMaxLines(
                    if (compact) {
                        3
                    } else {
                        4
                    }
                )
                .setEllipsize(
                    TextUtils.TruncateAt.END
                )
                .setEllipsizedWidth(
                    textWidth
                )
                .build()

        val textX =
            messagePanel.centerX() -
                    textWidth / 2f

        val textY =
            messagePanel.centerY() -
                    textLayout.height / 2f

        canvas.save()

        canvas.translate(
            textX,
            textY
        )

        textLayout.draw(
            canvas
        )

        canvas.restore()
    }


    private fun formatDateForExport(
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
            SimpleDateFormat(
                pattern,
                locale
            ).format(
                Date(capturedAt)
            )

        return if (uppercase) {
            formattedDate.uppercase(locale)
        } else {
            formattedDate
        }
    }


    private fun drawDate(
        canvas: Canvas,
        capturedAt: Long,
        dateFormat: String,
        datePanel: RectF,
        compact: Boolean = false
    ) {
        val dateText =
            formatDateForExport(
                capturedAt = capturedAt,
                dateFormat = dateFormat
            )

        val datePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    Color.rgb(
                        70,
                        58,
                        68
                    )

                textSize =
                    if (compact) {
                        30f
                    } else {
                        34f
                    }

                textAlign = Paint.Align.CENTER

                typeface =
                    Typeface.create(
                        Typeface.SERIF,
                        Typeface.NORMAL
                    )
            }

        val metrics =
            datePaint.fontMetrics

        val baseline =
            datePanel.centerY() -
                    (
                            metrics.ascent +
                                    metrics.descent
                            ) / 2f

        canvas.drawText(
            dateText,
            datePanel.centerX(),
            baseline,
            datePaint
        )
    }


    private fun createPinkingPath(
        bounds: RectF,
        inset: Float = 0f
    ): Path {
        val left =
            bounds.left + inset

        val top =
            bounds.top + inset

        val right =
            bounds.right - inset

        val bottom =
            bounds.bottom - inset

        val width =
            (right - left)
                .coerceAtLeast(1f)

        val height =
            (bottom - top)
                .coerceAtLeast(1f)

        val shortestSide =
            min(
                width,
                height
            )

        val toothDepth =
            (
                    shortestSide *
                            0.032f
                    ).coerceAtLeast(2f)

        val cornerCut =
            toothDepth * 1.5f

        val horizontalLength =
            (
                    width -
                            cornerCut * 2f
                    ).coerceAtLeast(1f)

        val verticalLength =
            (
                    height -
                            cornerCut * 2f
                    ).coerceAtLeast(1f)

        val horizontalTeeth =
            max(
                8,
                (
                        horizontalLength /
                                (toothDepth * 2.3f)
                        ).toInt()
            )

        val verticalTeeth =
            max(
                8,
                (
                        verticalLength /
                                (toothDepth * 2.3f)
                        ).toInt()
            )

        val horizontalStep =
            horizontalLength /
                    horizontalTeeth

        val verticalStep =
            verticalLength /
                    verticalTeeth

        return Path().apply {
            moveTo(
                left + cornerCut,
                top
            )

            repeat(horizontalTeeth) { index ->
                val startX =
                    left +
                            cornerCut +
                            index * horizontalStep

                lineTo(
                    startX +
                            horizontalStep / 2f,
                    top + toothDepth
                )

                lineTo(
                    startX + horizontalStep,
                    top
                )
            }

            lineTo(
                right,
                top + cornerCut
            )

            repeat(verticalTeeth) { index ->
                val startY =
                    top +
                            cornerCut +
                            index * verticalStep

                lineTo(
                    right - toothDepth,
                    startY +
                            verticalStep / 2f
                )

                lineTo(
                    right,
                    startY + verticalStep
                )
            }

            lineTo(
                right - cornerCut,
                bottom
            )

            repeat(horizontalTeeth) { index ->
                val startX =
                    right -
                            cornerCut -
                            index * horizontalStep

                lineTo(
                    startX -
                            horizontalStep / 2f,
                    bottom - toothDepth
                )

                lineTo(
                    startX - horizontalStep,
                    bottom
                )
            }

            lineTo(
                left,
                bottom - cornerCut
            )

            repeat(verticalTeeth) { index ->
                val startY =
                    bottom -
                            cornerCut -
                            index * verticalStep

                lineTo(
                    left + toothDepth,
                    startY -
                            verticalStep / 2f
                )

                lineTo(
                    left,
                    startY - verticalStep
                )
            }

            close()
        }
    }

    private fun drawStickerOverlay(
        context: Context,
        canvas: Canvas,
        stickerOverlay: StickerOverlay
    ) {
        val decodedStickerBitmap =
            runCatching {
                decodeStickerBitmap(
                    context = context,
                    stickerUri = stickerOverlay.uri
                )
            }.getOrNull()
        val decodedOriginalBitmap =
            if (
                decodedStickerBitmap == null &&
                stickerOverlay.isBackgroundRemoved &&
                stickerOverlay.originalUri != null
            ) {
                runCatching {
                    decodeStickerBitmap(
                        context = context,
                        stickerUri =
                            stickerOverlay.originalUri
                    )
                }.getOrNull()
            } else {
                null
            }
        val stickerBitmap =
            decodedStickerBitmap
                ?: decodedOriginalBitmap
                ?: return
        val drawAsBackgroundRemoved =
            stickerOverlay.isBackgroundRemoved &&
                    decodedStickerBitmap != null

        try {
            val stickerSide =
                (stickerOverlay.sizeRatio * OUTPUT_SIZE)
                    .coerceIn(
                        1f,
                        OUTPUT_SIZE.toFloat()
                    )
            val left =
                stickerOverlay.normalizedX
                    .coerceIn(0f, 1f) *
                        OUTPUT_SIZE
            val top =
                stickerOverlay.normalizedY
                    .coerceIn(0f, 1f) *
                        OUTPUT_SIZE
            val stickerBounds =
                RectF(
                    left.coerceIn(
                        0f,
                        OUTPUT_SIZE - stickerSide
                    ),
                    top.coerceIn(
                        0f,
                        OUTPUT_SIZE - stickerSide
                    ),
                    left.coerceIn(
                        0f,
                        OUTPUT_SIZE - stickerSide
                    ) + stickerSide,
                    top.coerceIn(
                        0f,
                        OUTPUT_SIZE - stickerSide
                    ) + stickerSide
                )

            if (
                stickerBounds.width() <= 0f ||
                stickerBounds.height() <= 0f
            ) {
                return
            }

            val stickerSize =
                min(
                    stickerBounds.width(),
                    stickerBounds.height()
                )
            val cornerRadius =
                stickerSize *
                        STICKER_CORNER_RADIUS_RATIO

            canvas.save()
            canvas.rotate(
                stickerOverlay.rotationDegrees,
                stickerBounds.centerX(),
                stickerBounds.centerY()
            )
            canvas.scale(
                if (stickerOverlay.flipHorizontal) -1f else 1f,
                if (stickerOverlay.flipVertical) -1f else 1f,
                stickerBounds.centerX(),
                stickerBounds.centerY()
            )

            try {
                if (drawAsBackgroundRemoved) {
                    drawFitCenteredBitmap(
                        canvas = canvas,
                        bitmap = stickerBitmap,
                        destinationRect = stickerBounds
                    )
                } else {
                    val stickerPath =
                        Path().apply {
                            addRoundRect(
                                stickerBounds,
                                cornerRadius,
                                cornerRadius,
                                Path.Direction.CW
                            )
                        }

                    canvas.save()
                    try {
                        canvas.clipPath(stickerPath)

                        drawCenterCroppedBitmap(
                            canvas = canvas,
                            bitmap = stickerBitmap,
                            destinationRect = stickerBounds
                        )
                    } finally {
                        canvas.restore()
                    }

                    val borderWidth =
                        stickerSize *
                                STICKER_BORDER_WIDTH_RATIO
                    val borderBounds =
                        RectF(stickerBounds).apply {
                            inset(
                                borderWidth / 2f,
                                borderWidth / 2f
                            )
                        }
                    val borderRadius =
                        (cornerRadius - borderWidth / 2f)
                            .coerceAtLeast(0f)
                    val borderPaint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK
                            style = Paint.Style.STROKE
                            strokeWidth = borderWidth
                        }

                    canvas.drawRoundRect(
                        borderBounds,
                        borderRadius,
                        borderRadius,
                        borderPaint
                    )
                }
            } finally {
                canvas.restore()
            }
        } finally {
            if (!stickerBitmap.isRecycled) {
                stickerBitmap.recycle()
            }
        }
    }

    private fun drawSealOverlay(
        context: Context,
        canvas: Canvas,
        sealOverlay: SealOverlay
    ) {
        val sealSide =
            (sealOverlay.sizeRatio * OUTPUT_SIZE)
                .coerceIn(1f, OUTPUT_SIZE.toFloat())

        // 도장은 미리보기에서 가장자리 걸침이 허용되므로(최소 가시 영역
        // 정책, DetailScreen.correctSealOffsetForMinimumVisibility 참고)
        // 여기서 [0,1]/[0, OUTPUT_SIZE-side]로 다시 완전히 안쪽으로 밀어
        // 넣지 않는다 — normalizedX/Y는 이미 그 정책으로 안전하게 보정된
        // 값이며, 이 함수가 임의로 재클램프하면 화면에서 허용된 위치가
        // Export에서 다시 이동해버린다.
        val left = sealOverlay.normalizedX * OUTPUT_SIZE
        val top = sealOverlay.normalizedY * OUTPUT_SIZE

        val sealBounds =
            RectF(left, top, left + sealSide, top + sealSide)

        if (
            sealBounds.width() <= 0f ||
            sealBounds.height() <= 0f
        ) {
            return
        }

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sealOverlay.colorArgb.toInt()
                style = Paint.Style.STROKE
            }

        canvas.save()
        canvas.rotate(
            sealOverlay.rotationDegrees,
            sealBounds.centerX(),
            sealBounds.centerY()
        )

        when (sealOverlay.type) {
            "CIRCLE_POSTMARK" ->
                drawCirclePostmarkOverlay(
                    canvas,
                    sealBounds,
                    paint,
                    sealOverlay.capturedAtMillis
                )

            "WAVE_CANCEL" ->
                drawWaveCancelOverlay(canvas, sealBounds, paint)

            "AIR_MAIL" ->
                drawAirMailOverlay(canvas, sealBounds, paint)

            "STAR" ->
                drawSealStarOverlay(canvas, sealBounds, paint)

            "DOG_PAW",
            "PIGEON_TRACK",
            "HEART",
            "STAR_STAMP" ->
                drawImageSealOverlay(
                    context,
                    canvas,
                    sealBounds,
                    sealOverlay
                )
        }

        canvas.restore()
    }

    private fun sealImageDrawableRes(type: String): Int? =
        when (type) {
            "DOG_PAW" -> R.drawable.seal_dog_paw
            "PIGEON_TRACK" -> R.drawable.seal_pigeon_track
            "HEART" -> R.drawable.seal_heart
            "STAR_STAMP" -> R.drawable.seal_star
            else -> null
        }

    /** 미니 스탬프(강아지·비둘기·하트·별)는 코드로 그리지 않고 res/drawable 실루엣 이미지를 잉크색으로 틴트해서 그린다. */
    private fun drawImageSealOverlay(
        context: Context,
        canvas: Canvas,
        bounds: RectF,
        sealOverlay: SealOverlay
    ) {
        val resId =
            sealImageDrawableRes(sealOverlay.type)
                ?: return

        val bitmap =
            runCatching {
                BitmapFactory.decodeResource(
                    context.resources,
                    resId
                )
            }.getOrNull() ?: return

        try {
            val paint =
                Paint(
                    Paint.ANTI_ALIAS_FLAG or
                            Paint.FILTER_BITMAP_FLAG
                ).apply {
                    colorFilter =
                        PorterDuffColorFilter(
                            sealOverlay.colorArgb.toInt(),
                            PorterDuff.Mode.SRC_IN
                        )
                }

            drawFitCenteredBitmap(
                canvas = canvas,
                bitmap = bitmap,
                destinationRect = bounds,
                paint = paint
            )
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun drawCirclePostmarkOverlay(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint,
        capturedAtMillis: Long?
    ) {
        val strokeWidth = min(bounds.width(), bounds.height()) * 0.035f
        paint.strokeWidth = strokeWidth

        val outerRadius =
            min(bounds.width(), bounds.height()) / 2f - strokeWidth
        val innerRadius = outerRadius * 0.72f
        val cx = bounds.centerX()
        val cy = bounds.centerY()

        canvas.drawCircle(cx, cy, outerRadius, paint)

        val innerPaint =
            Paint(paint).apply { this.strokeWidth = strokeWidth * 0.7f }
        canvas.drawCircle(cx, cy, innerRadius, innerPaint)

        val tickPaint =
            Paint(paint).apply { this.strokeWidth = strokeWidth * 0.6f }
        val tickCount = 16

        for (i in 0 until tickCount) {
            val angle = (2 * PI * i / tickCount).toFloat()
            val fromRadius = outerRadius * 0.86f
            val toRadius = outerRadius * 0.96f

            canvas.drawLine(
                cx + fromRadius * cos(angle),
                cy + fromRadius * sin(angle),
                cx + toRadius * cos(angle),
                cy + toRadius * sin(angle),
                tickPaint
            )
        }

        if (capturedAtMillis != null) {
            val textPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = paint.color
                    typeface = Typeface.create(
                        Typeface.SANS_SERIF,
                        Typeface.BOLD
                    )
                    textSize = innerRadius * 0.42f
                    textAlign = Paint.Align.CENTER
                }

            val dateText =
                SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                    .format(Date(capturedAtMillis))

            val textY =
                cy - (textPaint.descent() + textPaint.ascent()) / 2f

            canvas.drawText(dateText, cx, textY, textPaint)
        }
    }

    private fun drawWaveCancelOverlay(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint
    ) {
        val strokeWidth = min(bounds.width(), bounds.height()) * 0.035f
        paint.strokeWidth = strokeWidth

        val lineCount = 4
        val waveHeight = bounds.height() * 0.08f
        val waveWidth = bounds.width() * 0.22f
        val spacing = bounds.height() / (lineCount + 1)

        for (lineIndex in 1..lineCount) {
            val y = bounds.top + spacing * lineIndex
            val path = Path()
            path.moveTo(bounds.left, y)

            var x = bounds.left
            var up = true
            while (x < bounds.right) {
                val nextX = (x + waveWidth).coerceAtMost(bounds.right)
                path.quadTo(
                    x + waveWidth / 2f,
                    y + if (up) -waveHeight else waveHeight,
                    nextX,
                    y
                )
                x = nextX
                up = !up
            }

            canvas.drawPath(path, paint)
        }
    }

    private fun drawAirMailOverlay(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint
    ) {
        val strokeWidth = min(bounds.width(), bounds.height()) * 0.035f
        paint.strokeWidth = strokeWidth

        val cornerRadius =
            min(bounds.width(), bounds.height()) * 0.12f
        val inset = strokeWidth
        val rect =
            RectF(
                bounds.left + inset,
                bounds.top + inset,
                bounds.right - inset,
                bounds.bottom - inset
            )

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        val textPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = paint.color
                typeface = Typeface.create(
                    Typeface.SANS_SERIF,
                    Typeface.BOLD
                )
                textSize = bounds.height() * 0.24f
                textAlign = Paint.Align.CENTER
            }

        val textY =
            bounds.centerY() -
                    (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(
            "AIR MAIL",
            bounds.centerX(),
            textY,
            textPaint
        )
    }

    private fun drawSealStarOverlay(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint
    ) {
        val strokeWidth = min(bounds.width(), bounds.height()) * 0.035f
        paint.strokeWidth = strokeWidth

        val outerRadius =
            min(bounds.width(), bounds.height()) / 2f - strokeWidth
        val cx = bounds.centerX()
        val cy = bounds.centerY()

        canvas.drawCircle(cx, cy, outerRadius, paint)

        val starOuterRadius = outerRadius * 0.72f
        val starInnerRadius = starOuterRadius * 0.42f
        val points = 5
        val path = Path()

        for (i in 0 until points * 2) {
            val radius =
                if (i % 2 == 0) starOuterRadius else starInnerRadius
            val angle = (PI / points * i - PI / 2).toFloat()
            val x = cx + radius * cos(angle)
            val y = cy + radius * sin(angle)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        val starPaint =
            Paint(paint).apply { this.strokeWidth = strokeWidth * 0.8f }
        canvas.drawPath(path, starPaint)
    }

    private fun drawFitCenteredBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        destinationRect: RectF,
        paint: Paint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
        )
    ) {
        val sourceWidth =
            bitmap.width.toFloat()
        val sourceHeight =
            bitmap.height.toFloat()

        if (
            sourceWidth <= 0f ||
            sourceHeight <= 0f
        ) {
            throw IOException(
                "비트맵 크기를 확인할 수 없어."
            )
        }

        val scale =
            min(
                destinationRect.width() / sourceWidth,
                destinationRect.height() / sourceHeight
            )
        val fittedWidth =
            sourceWidth * scale
        val fittedHeight =
            sourceHeight * scale
        val left =
            destinationRect.left +
                    (destinationRect.width() - fittedWidth) /
                    2f
        val top =
            destinationRect.top +
                    (destinationRect.height() - fittedHeight) /
                    2f

        canvas.drawBitmap(
            bitmap,
            null,
            RectF(
                left,
                top,
                left + fittedWidth,
                top + fittedHeight
            ),
            paint
        )
    }

    private fun drawCenterCroppedBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        destinationRect: RectF
    ) {
        val sourceWidth =
            bitmap.width.toFloat()

        val sourceHeight =
            bitmap.height.toFloat()

        if (
            sourceWidth <= 0f ||
            sourceHeight <= 0f
        ) {
            throw IOException(
                "사진 크기를 확인하지 못했습니다."
            )
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
                    sourceHeight *
                            destinationRatio

                val left =
                    (
                            sourceWidth -
                                    croppedWidth
                            ) / 2f

                Rect(
                    left.toInt(),
                    0,
                    (
                            left +
                                    croppedWidth
                            ).toInt(),
                    sourceHeight.toInt()
                )
            } else {
                val croppedHeight =
                    sourceWidth /
                            destinationRatio

                val top =
                    (
                            sourceHeight -
                                    croppedHeight
                            ) / 2f

                Rect(
                    0,
                    top.toInt(),
                    sourceWidth.toInt(),
                    (
                            top +
                                    croppedHeight
                            ).toInt()
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

    private fun decodeBitmap(
        sourceFile: File
    ): Bitmap {
        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.P
        ) {
            val source =
                ImageDecoder.createSource(
                    sourceFile
                )

            ImageDecoder.decodeBitmap(
                source
            ) { decoder, _, _ ->
                decoder.allocator =
                    ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            BitmapFactory.decodeFile(
                sourceFile.absolutePath
            ) ?: throw IOException(
                "사진을 불러오지 못했습니다."
            )
        }
    }

    private fun decodeStickerBitmap(
        context: Context,
        stickerUri: Uri
    ): Bitmap {
        val resolver =
            context.contentResolver

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.P
        ) {
            val source =
                if (stickerUri.scheme == "file") {
                    ImageDecoder.createSource(
                        File(
                            stickerUri.path
                                ?: throw IOException(
                                    "스티커 파일 경로를 찾지 못했습니다."
                                )
                        )
                    )
                } else {
                    ImageDecoder.createSource(
                        resolver,
                        stickerUri
                    )
                }

            ImageDecoder.decodeBitmap(
                source
            ) { decoder, _, _ ->
                decoder.allocator =
                    ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val decodedBitmap =
                if (stickerUri.scheme == "file") {
                    BitmapFactory.decodeFile(
                        stickerUri.path
                            ?: throw IOException(
                                "스티커 파일 경로를 찾지 못했습니다."
                            )
                    )
                } else {
                    resolver
                        .openInputStream(stickerUri)
                        ?.use { inputStream ->
                            BitmapFactory.decodeStream(
                                inputStream
                            )
                        }
                }
                    ?: throw IOException(
                        "스티커 사진을 불러오지 못했습니다."
                    )

            val orientation =
                if (stickerUri.scheme == "file") {
                    stickerUri.path
                        ?.let { path ->
                            ExifInterface(path)
                                .getAttributeInt(
                                    ExifInterface
                                        .TAG_ORIENTATION,
                                    ExifInterface
                                        .ORIENTATION_NORMAL
                                )
                        }
                } else {
                    resolver
                        .openInputStream(stickerUri)
                        ?.use { inputStream ->
                            ExifInterface(inputStream)
                                .getAttributeInt(
                                    ExifInterface
                                        .TAG_ORIENTATION,
                                    ExifInterface
                                        .ORIENTATION_NORMAL
                                )
                        }
                }
                    ?: ExifInterface.ORIENTATION_NORMAL

            rotateBitmapUsingExif(
                bitmap = decodedBitmap,
                orientation = orientation
            )
        }
    }

    private fun rotateBitmapUsingExif(
        bitmap: Bitmap,
        orientation: Int
    ): Bitmap {
        val matrix =
            Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(
                    -1f,
                    1f
                )
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.setRotate(
                    180f
                )
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(
                    180f
                )
                matrix.postScale(
                    -1f,
                    1f
                )
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(
                    90f
                )
                matrix.postScale(
                    -1f,
                    1f
                )
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.setRotate(
                    90f
                )
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(
                    270f
                )
                matrix.postScale(
                    -1f,
                    1f
                )
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.setRotate(
                    270f
                )
            }

            else -> {
                return bitmap
            }
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

    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap
    ): Uri {
        val resolver =
            context.contentResolver

        val fileName =
            "postcard_memory_" +
                    System.currentTimeMillis() +
                    ".png"

        val contentValues =
            ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    fileName
                )

                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/png"
                )

                put(
                    MediaStore.Images.Media.DATE_TAKEN,
                    System.currentTimeMillis()
                )

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
                ) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/PostcardMemory"
                    )

                    put(
                        MediaStore.Images.Media.IS_PENDING,
                        1
                    )
                }
            }

        val collectionUri =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                MediaStore.Images.Media
                    .getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
            } else {
                MediaStore.Images.Media
                    .EXTERNAL_CONTENT_URI
            }

        val imageUri =
            resolver.insert(
                collectionUri,
                contentValues
            ) ?: throw IOException(
                "갤러리에 이미지를 만들지 못했습니다."
            )

        try {
            resolver
                .openOutputStream(imageUri)
                ?.use { outputStream ->
                    val saved =
                        bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            outputStream
                        )

                    if (!saved) {
                        throw IOException(
                            "이미지 파일 저장에 실패했습니다."
                        )
                    }
                }
                ?: throw IOException(
                    "이미지 저장 공간을 열지 못했습니다."
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                val completedValues =
                    ContentValues().apply {
                        put(
                            MediaStore.Images.Media.IS_PENDING,
                            0
                        )
                    }

                resolver.update(
                    imageUri,
                    completedValues,
                    null,
                    null
                )
            }

            return imageUri
        } catch (exception: Exception) {
            resolver.delete(
                imageUri,
                null,
                null
            )

            throw exception
        }
    }
}
