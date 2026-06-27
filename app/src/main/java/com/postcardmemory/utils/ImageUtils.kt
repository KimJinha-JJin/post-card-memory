package com.postcardmemory.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Crops a photo file to 3:4 aspect ratio (stamp format) and saves it to app private storage.
     * Returns the new cropped file.
     */
    fun cropToStampRatio(context: Context, sourceFile: File): File {
        // Read EXIF for rotation
        val exif = ExifInterface(sourceFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            ?: throw IllegalStateException("Could not decode bitmap from ${sourceFile.absolutePath}")

        // Apply rotation based on EXIF
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        // Crop to 3:4 ratio (portrait stamp)
        val targetRatio = 3f / 4f
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        val (cropWidth, cropHeight) = if (currentRatio > targetRatio) {
            // Image is wider than 3:4 - crop width
            val newWidth = (bitmap.height * targetRatio).toInt()
            Pair(newWidth, bitmap.height)
        } else {
            // Image is taller than 3:4 - crop height
            val newHeight = (bitmap.width / targetRatio).toInt()
            Pair(bitmap.width, newHeight)
        }

        val x = (bitmap.width - cropWidth) / 2
        val y = (bitmap.height - cropHeight) / 2

        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)

        // Scale down to reasonable size (max 1080px width)
        val maxWidth = 1080
        val finalBitmap = if (croppedBitmap.width > maxWidth) {
            val scale = maxWidth.toFloat() / croppedBitmap.width
            val newH = (croppedBitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(croppedBitmap, maxWidth, newH, true)
        } else {
            croppedBitmap
        }

        // Save to private storage
        val dir = File(context.filesDir, "postcards")
        dir.mkdirs()
        val outputFile = File(dir, "postcard_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        return outputFile
    }
}
