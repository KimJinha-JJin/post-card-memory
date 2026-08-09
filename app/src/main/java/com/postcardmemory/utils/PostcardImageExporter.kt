package com.postcardmemory.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
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
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.postcardmemory.R
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.PostcardDateFormat
import com.postcardmemory.ui.detail.MASKING_TAPE_DOT_RADIUS_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_GRID_LINE_WIDTH_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_HEART_SIZE_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_PATTERN_ALPHA
import com.postcardmemory.ui.detail.MASKING_TAPE_PATTERN_PITCH_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_PLAIN_FIBER_ALPHA
import com.postcardmemory.ui.detail.MASKING_TAPE_STAR_SIZE_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_STRIPE_PITCH_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_STRIPE_WIDTH_RATIO
import com.postcardmemory.ui.detail.MaskingTapeEdgeStyle
import com.postcardmemory.ui.detail.MaskingTapePatternKind
import com.postcardmemory.ui.detail.maskingTapeOutlinePoints
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
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
    private const val STICKER_CORNER_RADIUS_RATIO =
        16f / 120f
    /** TextStickerContent와 같은 두 겹(흰 STROKE → 컬러 FILL) 그리기 기법에서 쓰는 외곽선 두께 비율. */
    private const val TEXT_STICKER_OUTLINE_WIDTH_RATIO = 0.14f
    private const val STICKER_BORDER_WIDTH_RATIO =
        3f / 120f

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

    /**
     * fontSizeRatio는 다른 오버레이의 sizeRatio(박스 너비 비율)와 달리
     * "글자 크기"의 postcard 너비 대비 비율이다 — 텍스트 스티커는 문자열
     * 길이에 따라 박스 가로세로 비율이 제각각이라 박스 너비만으로는 export
     * 해상도에서 같은 글자 크기를 재현할 수 없다. 대신 화면과 export가
     * 동일한 fontSizePx로 각자 Paint.getTextBounds를 다시 측정해 그리므로
     * (TextStickerContent와 drawTextStickerOverlay), 두 렌더 경로가 항상
     * 같은 측정 결과를 얻는다.
     */
    data class TextStickerOverlay(
        val text: String,
        val normalizedX: Float,
        val normalizedY: Float,
        val fontSizeRatio: Float,
        val rotationDegrees: Float = 0f,
        val colorArgb: Long
    )

    /**
     * widthRatio/heightRatio는 다른 오버레이의 sizeRatio와 달리 가로·세로를
     * 따로 갖는다 — 마스킹테이프는 도장·스티커와 달리 정사각형이 아닌
     * 가로로 긴 형태라 하나의 비율만으로는 원래 모양을 재현할 수 없다.
     */
    data class MaskingTapeOverlay(
        val normalizedX: Float,
        val normalizedY: Float,
        val widthRatio: Float,
        val heightRatio: Float,
        val rotationDegrees: Float = 0f,
        val edgeStyle: MaskingTapeEdgeStyle,
        val baseColorArgb: Long,
        val patternColorArgb: Long,
        val patternKind: MaskingTapePatternKind,
        val alpha: Float,
        val isPhoto: Boolean = false,
        val photoUri: Uri? = null
    )

    fun exportToGallery(
        context: Context,
        postcard: Postcard,
        stickerOverlays: List<StickerOverlay> = emptyList(),
        sealOverlays: List<SealOverlay> = emptyList(),
        doodleStrokes: List<DoodleStroke> = emptyList(),
        textStickerOverlays: List<TextStickerOverlay> = emptyList(),
        maskingTapeOverlays: List<MaskingTapeOverlay> = emptyList()
    ): Result<Uri> {
        return runCatching {
            val outputBitmap =
                createPostcardBitmap(
                    context = context,
                    postcard = postcard,
                    stickerOverlays = stickerOverlays,
                    sealOverlays = sealOverlays,
                    doodleStrokes = doodleStrokes,
                    textStickerOverlays = textStickerOverlays,
                    maskingTapeOverlays = maskingTapeOverlays
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
        sealOverlays: List<SealOverlay> = emptyList(),
        doodleStrokes: List<DoodleStroke> = emptyList(),
        textStickerOverlays: List<TextStickerOverlay> = emptyList(),
        maskingTapeOverlays: List<MaskingTapeOverlay> = emptyList()
    ): Result<File> {
        return runCatching {
            val outputBitmap =
                createPostcardBitmap(
                    context = context,
                    postcard = postcard,
                    stickerOverlays = stickerOverlays,
                    sealOverlays = sealOverlays,
                    doodleStrokes = doodleStrokes,
                    textStickerOverlays = textStickerOverlays,
                    maskingTapeOverlays = maskingTapeOverlays
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
        sealOverlays: List<SealOverlay> = emptyList(),
        doodleStrokes: List<DoodleStroke> = emptyList(),
        textStickerOverlays: List<TextStickerOverlay> = emptyList(),
        maskingTapeOverlays: List<MaskingTapeOverlay> = emptyList()
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

            // 마스킹테이프는 "사진 위에 붙이는 얇은 다꾸 재료"라는 제품
            // 의미상 사진 바로 위, 스티커·도장·텍스트 스티커보다는 아래에
            // 그린다 — 미리보기(DetailScreen)도 photoMaskingTapes.forEach를
            // photoStickers.forEach보다 먼저 호출해 같은 순서를 유지한다.
            for (overlay in maskingTapeOverlays) {
                drawMaskingTapeOverlay(
                    context = context,
                    canvas = canvas,
                    tapeOverlay = overlay
                )
            }

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

            for (overlay in textStickerOverlays) {
                drawTextStickerOverlay(
                    canvas = canvas,
                    textStickerOverlay = overlay
                )
            }

            // 낙서는 사진·스티커·도장보다 항상 위에 그린다 — 미리보기(DetailScreen의
            // Canvas)와 동일하게 PostcardRenderSpec.drawDoodleStrokes 하나만 공유해서
            // 호출하므로 좌표·굵기 계산이 두 경로에서 갈라지지 않는다.
            PostcardRenderSpec.drawDoodleStrokes(
                canvas = canvas,
                strokes = doodleStrokes,
                targetSize = OUTPUT_SIZE.toFloat()
            )

            return outputBitmap
        } finally {
            if (!sourceBitmap.isRecycled) {
                sourceBitmap.recycle()
            }
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

    /**
     * 화면(MaskingTapeContent, ui/components/MaskingTapeShapes.kt)과 같은
     * 비율 상수(MASKING_TAPE_* in ui/detail/MaskingTapeItem.kt)로 모양·패턴을
     * 다시 계산해 그린다 — 두 렌더 경로가 같은 값을 참조하므로 화면과 저장
     * 이미지에서 테이프 모양·패턴이 어긋나지 않는다.
     */
    private fun drawMaskingTapeOverlay(
        context: Context,
        canvas: Canvas,
        tapeOverlay: MaskingTapeOverlay
    ) {
        val widthPx =
            (tapeOverlay.widthRatio * OUTPUT_SIZE).coerceAtLeast(1f)
        val heightPx =
            (tapeOverlay.heightRatio * OUTPUT_SIZE).coerceAtLeast(1f)
        val left = tapeOverlay.normalizedX * OUTPUT_SIZE
        val top = tapeOverlay.normalizedY * OUTPUT_SIZE
        val bounds = RectF(left, top, left + widthPx, top + heightPx)

        if (bounds.width() <= 0f || bounds.height() <= 0f) return

        canvas.save()
        try {
            canvas.rotate(
                tapeOverlay.rotationDegrees,
                bounds.centerX(),
                bounds.centerY()
            )
            canvas.clipPath(maskingTapeCutPath(bounds, tapeOverlay.edgeStyle))

            if (tapeOverlay.isPhoto) {
                val photoBitmap =
                    tapeOverlay.photoUri?.let {
                        runCatching {
                            MaskingTapePhotoDecoder.decodeSampledBitmap(context, it)
                        }.getOrNull()
                    }

                if (photoBitmap != null) {
                    try {
                        drawTiledPhotoOverlay(canvas, bounds, photoBitmap, tapeOverlay.alpha)
                    } finally {
                        if (!photoBitmap.isRecycled) {
                            photoBitmap.recycle()
                        }
                    }
                } else {
                    val fallbackPaint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = colorWithAlpha(tapeOverlay.baseColorArgb, tapeOverlay.alpha)
                        }
                    canvas.drawRect(bounds, fallbackPaint)
                }
            } else {
                val basePaint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = colorWithAlpha(tapeOverlay.baseColorArgb, tapeOverlay.alpha)
                    }
                canvas.drawRect(bounds, basePaint)
                drawMaskingTapePatternOverlay(canvas, bounds, tapeOverlay.patternColorArgb, tapeOverlay.patternKind)
            }
        } finally {
            canvas.restore()
        }
    }

    /**
     * Compose(MaskingTapeShapes.kt의 maskingTapeClipPath)와 같은 좌표 함수
     * (maskingTapeOutlinePoints)를 그대로 옮겨 그린다 — 두 렌더 경로가 항상
     * 같은 외곽선을 얻는다.
     */
    private fun maskingTapeCutPath(bounds: RectF, edgeStyle: MaskingTapeEdgeStyle): Path {
        val points =
            maskingTapeOutlinePoints(bounds.width(), bounds.height(), edgeStyle)

        if (points.isEmpty()) {
            return Path().apply { addRect(bounds, Path.Direction.CW) }
        }

        return Path().apply {
            moveTo(bounds.left + points[0].first, bounds.top + points[0].second)
            for (i in 1 until points.size) {
                lineTo(bounds.left + points[i].first, bounds.top + points[i].second)
            }
            close()
        }
    }

    private fun drawTiledPhotoOverlay(
        canvas: Canvas,
        bounds: RectF,
        bitmap: Bitmap,
        alpha: Float
    ) {
        val tileSize = bounds.height()

        if (tileSize <= 0f || bitmap.width <= 0 || bitmap.height <= 0) return

        val matrix =
            Matrix().apply {
                setScale(tileSize / bitmap.width, tileSize / bitmap.height)
                postTranslate(bounds.left, bounds.top)
            }

        val shader =
            BitmapShader(
                bitmap,
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT
            ).apply { setLocalMatrix(matrix) }

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
                this.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
            }

        canvas.drawRect(bounds, paint)
    }

    private fun drawMaskingTapePatternOverlay(
        canvas: Canvas,
        bounds: RectF,
        patternColorArgb: Long,
        patternKind: MaskingTapePatternKind
    ) {
        when (patternKind) {
            MaskingTapePatternKind.GRID -> drawTapeGridOverlay(canvas, bounds, patternColorArgb)
            MaskingTapePatternKind.DOT -> drawTapeDotsOverlay(canvas, bounds, patternColorArgb)
            MaskingTapePatternKind.STAR -> drawTapeStarsOverlay(canvas, bounds, patternColorArgb)
            MaskingTapePatternKind.HEART -> drawTapeHeartsOverlay(canvas, bounds, patternColorArgb)
            MaskingTapePatternKind.STRIPE -> drawTapeStripesOverlay(canvas, bounds, patternColorArgb)
            MaskingTapePatternKind.PLAIN -> drawTapeFiberOverlay(canvas, bounds, patternColorArgb)
        }
    }

    private fun drawTapeGridOverlay(canvas: Canvas, bounds: RectF, patternColorArgb: Long) {
        val height = bounds.height()
        val pitch = height * MASKING_TAPE_PATTERN_PITCH_RATIO
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorWithAlpha(patternColorArgb, MASKING_TAPE_PATTERN_ALPHA)
                strokeWidth = height * MASKING_TAPE_GRID_LINE_WIDTH_RATIO
            }

        var x = bounds.left + pitch / 2f
        while (x < bounds.right) {
            canvas.drawLine(x, bounds.top, x, bounds.bottom, paint)
            x += pitch
        }

        var y = bounds.top + pitch / 2f
        while (y < bounds.bottom) {
            canvas.drawLine(bounds.left, y, bounds.right, y, paint)
            y += pitch
        }
    }

    private fun drawTapeDotsOverlay(canvas: Canvas, bounds: RectF, patternColorArgb: Long) {
        val height = bounds.height()
        val pitch = height * MASKING_TAPE_PATTERN_PITCH_RATIO
        val radius = height * MASKING_TAPE_DOT_RADIUS_RATIO
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorWithAlpha(patternColorArgb, MASKING_TAPE_PATTERN_ALPHA)
                style = Paint.Style.FILL
            }

        forEachTapeGridPointOverlay(bounds, pitch) { x, y ->
            canvas.drawCircle(x, y, radius, paint)
        }
    }

    private fun drawTapeStarsOverlay(canvas: Canvas, bounds: RectF, patternColorArgb: Long) {
        val height = bounds.height()
        val pitch = height * MASKING_TAPE_PATTERN_PITCH_RATIO
        val starSize = height * MASKING_TAPE_STAR_SIZE_RATIO
        val half = starSize / 2f
        val diag = half * 0.6f
        val strokeWidth = starSize * 0.16f
        val thickPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorWithAlpha(patternColorArgb, MASKING_TAPE_PATTERN_ALPHA)
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
            }
        val thinPaint =
            Paint(thickPaint).apply {
                this.strokeWidth = strokeWidth * 0.7f
            }

        forEachTapeGridPointOverlay(bounds, pitch) { x, y ->
            canvas.drawLine(x - half, y, x + half, y, thickPaint)
            canvas.drawLine(x, y - half, x, y + half, thickPaint)
            canvas.drawLine(x - diag, y - diag, x + diag, y + diag, thinPaint)
            canvas.drawLine(x - diag, y + diag, x + diag, y - diag, thinPaint)
        }
    }

    private fun drawTapeHeartsOverlay(canvas: Canvas, bounds: RectF, patternColorArgb: Long) {
        val height = bounds.height()
        val pitch = height * MASKING_TAPE_PATTERN_PITCH_RATIO
        val heartSize = height * MASKING_TAPE_HEART_SIZE_RATIO
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorWithAlpha(patternColorArgb, MASKING_TAPE_PATTERN_ALPHA)
                style = Paint.Style.FILL
            }

        forEachTapeGridPointOverlay(bounds, pitch) { x, y ->
            canvas.drawPath(tapeHeartPathOverlay(x, y, heartSize), paint)
        }
    }

    private fun tapeHeartPathOverlay(
        centerX: Float,
        centerY: Float,
        heartSize: Float
    ): Path {
        val half = heartSize / 2f
        val top = centerY - half * 0.5f

        return Path().apply {
            moveTo(centerX, centerY + half)
            cubicTo(
                centerX - half * 1.3f, centerY,
                centerX - half * 0.6f, top - half * 0.6f,
                centerX, top
            )
            cubicTo(
                centerX + half * 0.6f, top - half * 0.6f,
                centerX + half * 1.3f, centerY,
                centerX, centerY + half
            )
            close()
        }
    }

    private fun drawTapeStripesOverlay(canvas: Canvas, bounds: RectF, patternColorArgb: Long) {
        val height = bounds.height()
        val pitch = height * MASKING_TAPE_STRIPE_PITCH_RATIO
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorWithAlpha(patternColorArgb, MASKING_TAPE_PATTERN_ALPHA)
                strokeWidth = height * MASKING_TAPE_STRIPE_WIDTH_RATIO
            }
        val diagonalSpan = bounds.width() + height

        var offset = -height
        while (offset < diagonalSpan) {
            canvas.drawLine(
                bounds.left + offset,
                bounds.top,
                bounds.left + offset + height,
                bounds.bottom,
                paint
            )
            offset += pitch
        }
    }

    /** 반투명 아이보리처럼 도안 없이 은은한 종이 결만 살짝 비치게 한다. */
    private fun drawTapeFiberOverlay(canvas: Canvas, bounds: RectF, patternColorArgb: Long) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorWithAlpha(patternColorArgb, MASKING_TAPE_PLAIN_FIBER_ALPHA)
                strokeWidth = bounds.height() * 0.03f
            }

        listOf(0.32f, 0.68f).forEach { yRatio ->
            val y = bounds.top + bounds.height() * yRatio
            canvas.drawLine(
                bounds.left + bounds.width() * 0.08f,
                y,
                bounds.left + bounds.width() * 0.92f,
                y,
                paint
            )
        }
    }

    /** 테이프 영역 안에서 격자 형태로 촘촘히 엇갈린 도안 반복 위치를 순회한다(체크무늬 배치). */
    private inline fun forEachTapeGridPointOverlay(
        bounds: RectF,
        pitch: Float,
        action: (x: Float, y: Float) -> Unit
    ) {
        var row = 0
        var y = bounds.top + pitch / 2f
        while (y < bounds.bottom) {
            val rowOffset = if (row % 2 == 0) 0f else pitch / 2f
            var x = bounds.left + pitch / 2f + rowOffset
            while (x < bounds.right) {
                action(x, y)
                x += pitch
            }
            y += pitch
            row++
        }
    }

    private fun colorWithAlpha(argb: Long, alphaFraction: Float): Int {
        val rgb = argb.toInt() and 0x00FFFFFF
        val alphaByte = (alphaFraction.coerceIn(0f, 1f) * 255f).toInt()
        return (alphaByte shl 24) or rgb
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

    /**
     * 화면(TextStickerContent)과 동일하게 Paint.getTextBounds로 이 함수
     * 안에서 직접 측정해 그린다 — 미리 계산된 폭을 받아 맞춰 그리지 않고
     * 똑같은 절차를 다시 밟으므로, 화면과 export의 외곽선 두께·글자 위치가
     * 항상 같은 방식으로 계산된다.
     */
    private fun drawTextStickerOverlay(
        canvas: Canvas,
        textStickerOverlay: TextStickerOverlay
    ) {
        val fontSizePx =
            textStickerOverlay.fontSizeRatio * OUTPUT_SIZE

        if (fontSizePx <= 0f) return

        val fillPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = textStickerOverlay.colorArgb.toInt()
                textSize = fontSizePx
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }

        val strokeWidthPx = fontSizePx * TEXT_STICKER_OUTLINE_WIDTH_RATIO

        val textBounds = Rect()
        fillPaint.getTextBounds(
            textStickerOverlay.text,
            0,
            textStickerOverlay.text.length,
            textBounds
        )

        val left = textStickerOverlay.normalizedX * OUTPUT_SIZE
        val top = textStickerOverlay.normalizedY * OUTPUT_SIZE
        val boxWidth = textBounds.width() + strokeWidthPx
        val boxHeight = textBounds.height() + strokeWidthPx

        if (boxWidth <= 0f || boxHeight <= 0f) return

        val strokePaint =
            Paint(fillPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeJoin = Paint.Join.ROUND
                strokeMiter = 2f
                color = Color.WHITE
            }

        val originX = left - textBounds.left + strokeWidthPx / 2f
        val originY = top - textBounds.top + strokeWidthPx / 2f

        canvas.save()
        canvas.rotate(
            textStickerOverlay.rotationDegrees,
            left + boxWidth / 2f,
            top + boxHeight / 2f
        )
        canvas.drawText(textStickerOverlay.text, originX, originY, strokePaint)
        canvas.drawText(textStickerOverlay.text, originX, originY, fillPaint)
        canvas.restore()
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
                PostcardDateFormat.formatIso(capturedAtMillis)

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
