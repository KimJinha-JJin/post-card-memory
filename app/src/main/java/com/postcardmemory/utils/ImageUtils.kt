package com.postcardmemory.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageUtils {

    data class OrientedImageSize(
        val width: Int,
        val height: Int
    )

    /**
     * 사진 전체를 불러오지 않고
     * 회전 방향이 적용된 가로·세로 크기만 확인한다.
     */
    fun getOrientedImageSize(
        sourceFile: File
    ): OrientedImageSize {
        require(sourceFile.exists()) {
            "촬영된 사진 파일을 찾을 수 없습니다."
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(
            sourceFile.absolutePath,
            options
        )

        if (
            options.outWidth <= 0 ||
            options.outHeight <= 0
        ) {
            throw IllegalStateException(
                "사진 크기를 확인하지 못했습니다."
            )
        }

        val exif = ExifInterface(
            sourceFile.absolutePath
        )

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val widthAndHeightSwapped =
            orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                    orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                    orientation == ExifInterface.ORIENTATION_TRANSVERSE

        return if (widthAndHeightSwapped) {
            OrientedImageSize(
                width = options.outHeight,
                height = options.outWidth
            )
        } else {
            OrientedImageSize(
                width = options.outWidth,
                height = options.outHeight
            )
        }
    }

    /**
     * 기존 방식과의 호환을 위한 중앙 정사각형 자르기 함수.
     *
     * 아직 CameraViewModel이 기존 함수를 사용하고 있어도
     * 정상적으로 빌드되고 동작한다.
     */
    fun cropToStampRatio(
        context: Context,
        sourceFile: File
    ): File {
        val imageSize = getOrientedImageSize(
            sourceFile
        )

        val viewportSize = min(
            imageSize.width,
            imageSize.height
        ).toFloat()

        return cropToStampRatio(
            context = context,
            sourceFile = sourceFile,
            zoom = 1f,
            offsetX = 0f,
            offsetY = 0f,
            viewportSize = viewportSize
        )
    }

    /**
     * 사용자가 화면에서 선택한 확대 비율과 이동 위치를
     * 원본 사진 좌표로 환산하여 정사각형으로 저장한다.
     */
    fun cropToStampRatio(
        context: Context,
        sourceFile: File,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        viewportSize: Float
    ): File {
        require(sourceFile.exists()) {
            "촬영된 사진 파일을 찾을 수 없습니다."
        }

        require(viewportSize > 0f) {
            "사진 자르기 영역의 크기가 올바르지 않습니다."
        }

        val exif = ExifInterface(
            sourceFile.absolutePath
        )

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val originalBitmap = BitmapFactory.decodeFile(
            sourceFile.absolutePath
        ) ?: throw IllegalStateException(
            "사진을 불러올 수 없습니다."
        )

        val rotatedBitmap = rotateBitmapUsingExif(
            bitmap = originalBitmap,
            orientation = orientation
        )

        val safeZoom = zoom.coerceIn(
            minimumValue = 1f,
            maximumValue = 4f
        )

        val imageWidth = rotatedBitmap.width.toFloat()
        val imageHeight = rotatedBitmap.height.toFloat()

        /*
         * 사진이 정사각형 편집 영역을 빈틈없이 채우는
         * 기본 확대 비율을 계산한다.
         */
        val baseScale = max(
            viewportSize / imageWidth,
            viewportSize / imageHeight
        )

        val totalScale = baseScale * safeZoom

        /*
         * 화면의 정사각형 한 변이
         * 원본 사진에서는 몇 픽셀인지 계산한다.
         */
        val sourceCropSize = (
                viewportSize / totalScale
                )
            .roundToInt()
            .coerceAtLeast(1)
            .coerceAtMost(
                min(
                    rotatedBitmap.width,
                    rotatedBitmap.height
                )
            )

        /*
         * 화면에서 사진을 오른쪽으로 옮기면
         * 원본에서는 더 왼쪽 부분을 선택하게 된다.
         */
        val sourceCenterX =
            imageWidth / 2f -
                    offsetX / totalScale

        val sourceCenterY =
            imageHeight / 2f -
                    offsetY / totalScale

        val maximumCropX =
            rotatedBitmap.width -
                    sourceCropSize

        val maximumCropY =
            rotatedBitmap.height -
                    sourceCropSize

        val cropX = (
                sourceCenterX -
                        sourceCropSize / 2f
                )
            .roundToInt()
            .coerceIn(
                minimumValue = 0,
                maximumValue = maximumCropX
            )

        val cropY = (
                sourceCenterY -
                        sourceCropSize / 2f
                )
            .roundToInt()
            .coerceIn(
                minimumValue = 0,
                maximumValue = maximumCropY
            )

        val squareBitmap = Bitmap.createBitmap(
            rotatedBitmap,
            cropX,
            cropY,
            sourceCropSize,
            sourceCropSize
        )

        val finalBitmap =
            if (squareBitmap.width > 1080) {
                Bitmap.createScaledBitmap(
                    squareBitmap,
                    1080,
                    1080,
                    true
                )
            } else {
                squareBitmap
            }

        val directory = File(
            context.filesDir,
            "postcards"
        )

        if (!directory.exists()) {
            val created = directory.mkdirs()

            if (!created && !directory.exists()) {
                throw IllegalStateException(
                    "사진 저장 폴더를 만들지 못했습니다."
                )
            }
        }

        val outputFile = File(
            directory,
            "postcard_${System.currentTimeMillis()}.jpg"
        )

        FileOutputStream(outputFile).use { outputStream ->
            val saved = finalBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                92,
                outputStream
            )

            if (!saved) {
                throw IllegalStateException(
                    "정사각형 사진을 저장하지 못했습니다."
                )
            }

            outputStream.flush()
        }

        return outputFile
    }

    /**
     * 사진의 EXIF 방향 정보에 맞게
     * 비트맵을 실제 방향으로 회전하거나 반전한다.
     */
    private fun rotateBitmapUsingExif(
        bitmap: Bitmap,
        orientation: Int
    ): Bitmap {
        val matrix = Matrix()

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

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}