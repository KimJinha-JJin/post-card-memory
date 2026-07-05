package com.postcardmemory.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID

object PostcardImageStorage {

    fun copyToAppStorage(
        context: Context,
        sourceUri: Uri
    ): String {
        val postcardsDirectory =
            File(
                context.filesDir,
                "postcards"
            )

        if (
            !postcardsDirectory.exists() &&
            !postcardsDirectory.mkdirs()
        ) {
            throw IOException(
                "사진 저장 폴더를 만들지 못했습니다."
            )
        }

        val extension =
            getFileExtension(
                context = context,
                uri = sourceUri
            )

        val outputFile =
            File(
                postcardsDirectory,
                "postcard_" +
                        UUID.randomUUID() +
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
                    "선택한 사진을 불러오지 못했습니다."
                )

            if (
                !outputFile.exists() ||
                outputFile.length() <= 0L
            ) {
                throw IOException(
                    "사진 파일이 올바르게 저장되지 않았습니다."
                )
            }

            val decodedBitmap =
                runCatching {
                    PostcardRenderSpec
                        .decodeSourceBitmap(
                            outputFile
                        )
                }.getOrNull()
                    ?: throw IOException(
                        "선택한 파일을 사진으로 열 수 없습니다."
                    )

            if (!decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }

            return outputFile.absolutePath
        } catch (exception: Exception) {
            if (outputFile.exists()) {
                outputFile.delete()
            }

            throw exception
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
