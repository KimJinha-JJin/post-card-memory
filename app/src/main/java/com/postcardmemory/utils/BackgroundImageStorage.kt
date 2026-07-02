package com.postcardmemory.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

object BackgroundImageStorage {

    fun copyToAppStorage(
        context: Context,
        sourceUri: Uri
    ): String {
        val backgroundDirectory =
            File(
                context.filesDir,
                "postcard_backgrounds"
            )

        if (
            !backgroundDirectory.exists() &&
            !backgroundDirectory.mkdirs()
        ) {
            throw IOException(
                "배경사진 저장 폴더를 만들지 못했습니다."
            )
        }

        val extension =
            getFileExtension(
                context = context,
                uri = sourceUri
            )

        val outputFile =
            File(
                backgroundDirectory,
                "background_" +
                        System.currentTimeMillis() +
                        extension
            )

        try {
            context.contentResolver
                .openInputStream(sourceUri)
                ?.use { inputStream ->
                    outputFile
                        .outputStream()
                        .buffered()
                        .use { outputStream ->
                            inputStream.copyTo(
                                outputStream
                            )
                        }
                }
                ?: throw IOException(
                    "선택한 배경사진을 불러오지 못했습니다."
                )

            return outputFile.absolutePath
        } catch (exception: Exception) {
            if (outputFile.exists()) {
                outputFile.delete()
            }

            throw exception
        }
    }

    fun deleteBackgroundImage(
        imagePath: String?
    ) {
        if (imagePath.isNullOrBlank()) {
            return
        }

        val imageFile =
            File(imagePath)

        if (imageFile.exists()) {
            imageFile.delete()
        }
    }

    private fun getFileExtension(
        context: Context,
        uri: Uri
    ): String {
        return when (
            context.contentResolver
                .getType(uri)
                ?.lowercase()
        ) {
            "image/png" ->
                ".png"

            "image/webp" ->
                ".webp"

            "image/heic",
            "image/heif" ->
                ".heic"

            else ->
                ".jpg"
        }
    }
}