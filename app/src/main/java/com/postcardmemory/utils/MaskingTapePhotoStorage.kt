package com.postcardmemory.utils

import android.content.Context
import android.net.Uri
import com.postcardmemory.ui.detail.MaskingTapeItem
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * PhotoStickerImageStorage.copyToStickerOriginalStorage(Uri)와 동일한 이유로
 * 존재한다 — Photo Picker가 돌려주는 content://media/picker/... URI는
 * persistable grant를 지원하지 않아(takePersistableUriPermission이 항상
 * 실패) 원본 URI를 그대로 오래 보관할 수 없다. 마스킹테이프 사진은
 * Photo Picker로만 추가하므로(OpenDocument/Camera 경로 없음) 선택 즉시
 * 이 저장소로 복사해 앱 소유 파일로 만든다.
 */
object MaskingTapePhotoStorage {

    private const val PHOTOS_DIRECTORY_NAME = "masking_tape_photos"

    fun copyToMaskingTapePhotoStorage(
        context: Context,
        postcardId: Long,
        sourceUri: Uri
    ): Uri {
        val photosDirectory =
            File(
                File(
                    context.filesDir,
                    PHOTOS_DIRECTORY_NAME
                ),
                postcardId.toString()
            )

        if (
            !photosDirectory.exists() &&
            !photosDirectory.mkdirs()
        ) {
            throw IOException(
                "마스킹테이프 사진 저장 폴더를 만들지 못했습니다."
            )
        }

        val outputFile =
            File(
                photosDirectory,
                "masking_tape_" +
                        UUID.randomUUID() +
                        getFileExtension(context, sourceUri)
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
                        "선택한 사진을 열 수 없습니다."
                    )

            if (!decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }

            return Uri.fromFile(outputFile)
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

    /**
     * 삭제된 마스킹테이프의 photoUri가
     * masking_tape_photos/ 소유 파일이면서
     * 남은 테이프 중 같은 파일을 참조하는 것이
     * 하나도 없을 때만 실제 파일을 지운다.
     */
    fun deleteIfUnreferenced(
        context: Context,
        deletedUri: Uri?,
        remainingTapes: List<MaskingTapeItem>
    ) {
        if (deletedUri == null || deletedUri.scheme != "file") {
            return
        }

        val path =
            deletedUri.path
                ?: return

        val targetFile =
            File(path).canonicalFile

        val photosRoot =
            File(
                context.filesDir,
                PHOTOS_DIRECTORY_NAME
            ).canonicalFile

        if (
            !targetFile.path.startsWith(
                photosRoot.path
            )
        ) {
            return
        }

        val stillReferenced =
            remainingTapes.any { tape ->
                tape.photoUri == deletedUri
            }

        if (!stillReferenced && targetFile.exists()) {
            targetFile.delete()
        }
    }
}
