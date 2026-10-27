package com.postcardmemory.utils

import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette

/** 사진에서 배경색 후보를 뽑아내는 유틸리티 */
object PhotoColorExtractor {

    data class ExtractedColor(
        val label: String,
        val colorArgb: Long
    )

    private const val TARGET_SAMPLE_SIZE_PX = 200
    private const val MAX_COLORS = 5

    fun extractColors(imagePath: String): List<ExtractedColor> {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(imagePath, boundsOptions)

        if (
            boundsOptions.outWidth <= 0 ||
            boundsOptions.outHeight <= 0
        ) {
            return emptyList()
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = boundsOptions.outWidth,
                height = boundsOptions.outHeight,
                targetSize = TARGET_SAMPLE_SIZE_PX
            )
        }

        val bitmap =
            BitmapFactory.decodeFile(imagePath, decodeOptions)
                ?: return emptyList()

        val palette = Palette.from(bitmap).generate()

        val labeledSwatches = listOf(
            "생동감 있는 색" to palette.vibrantSwatch,
            "은은한 색" to palette.mutedSwatch,
            "밝은 색" to palette.lightVibrantSwatch,
            "어두운 색" to palette.darkVibrantSwatch,
            "밝고 은은한 색" to palette.lightMutedSwatch,
            "어둡고 은은한 색" to palette.darkMutedSwatch
        )

        bitmap.recycle()

        return labeledSwatches
            .mapNotNull { (label, swatch) ->
                swatch?.let {
                    ExtractedColor(
                        label = label,
                        colorArgb = it.rgb.toLong() and 0xFFFFFFFFL
                    )
                }
            }
            .distinctBy { it.colorArgb }
            .take(MAX_COLORS)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        targetSize: Int
    ): Int {
        var inSampleSize = 1
        val halfWidth = width / 2
        val halfHeight = height / 2

        while (
            (halfWidth / inSampleSize) >= targetSize &&
            (halfHeight / inSampleSize) >= targetSize
        ) {
            inSampleSize *= 2
        }

        return inSampleSize
    }
}
