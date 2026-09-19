package com.postcardmemory.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
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

        writeOrDeletePartialFile(outputFile) { outputStream ->
            finalBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                92,
                outputStream
            )
        }

        return outputFile
    }

    /**
     * [outputFile]에 [write]로 내용을 쓰되, 쓰다가 실패하면 **만들다 만
     * 파일을 남기지 않는다**.
     *
     * 예전에는 compress가 false를 돌려주거나 도중에 예외가 나면 0바이트이거나
     * 반쯤 쓰인 JPEG가 postcards/ 에 그대로 남았다. 그 파일은 DB가 참조하지
     * 않으므로 어느 화면에도 나타나지 않고, 지워주는 주체도 없는 고아가 된다.
     *
     * [write]가 false를 돌려주는 것도 실패로 본다(Bitmap.compress의 실패
     * 신호). 예외는 정리 후 그대로 다시 던져 호출부가 실패를 알 수 있게 한다.
     *
     * internal: Bitmap 없이 순수 JUnit에서 이 정리 규칙만 직접 검증하기 위함.
     */
    internal fun writeOrDeletePartialFile(
        outputFile: File,
        write: (OutputStream) -> Boolean
    ) {
        try {
            FileOutputStream(outputFile).use { outputStream ->
                val saved = write(outputStream)

                if (!saved) {
                    throw IllegalStateException(
                        "정사각형 사진을 저장하지 못했습니다."
                    )
                }

                outputStream.flush()
            }
        } catch (failure: Throwable) {
            outputFile.delete()
            throw failure
        }
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