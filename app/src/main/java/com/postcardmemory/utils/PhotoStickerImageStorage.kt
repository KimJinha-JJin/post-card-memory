package com.postcardmemory.utils

import android.content.Context
import android.net.Uri
import com.postcardmemory.ui.detail.PhotoStickerItem
import java.io.File
import java.io.IOException
import java.util.UUID

object PhotoStickerImageStorage {

    private const val ORIGINALS_DIRECTORY_NAME =
        "sticker_originals"

    /**
     * 카메라로 촬영한 스티커 원본을
     * postcard별 영구 폴더로 복사하고 검증한다.
     */
    fun copyToStickerOriginalStorage(
        context: Context,
        postcardId: Long,
        sourceFile: File
    ): Uri {
        val originalsDirectory =
            File(
                File(
                    context.filesDir,
                    ORIGINALS_DIRECTORY_NAME
                ),
                postcardId.toString()
            )

        if (
            !originalsDirectory.exists() &&
            !originalsDirectory.mkdirs()
        ) {
            throw IOException(
                "스티커 사진 저장 폴더를 만들지 못했습니다."
            )
        }

        val outputFile =
            File(
                originalsDirectory,
                "sticker_" +
                        UUID.randomUUID() +
                        ".jpg"
            )

        try {
            sourceFile.copyTo(
                outputFile,
                overwrite = true
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
                        "촬영한 사진을 열 수 없습니다."
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

    /**
     * 삭제된 스티커의 originalUri가
     * sticker_originals/ 소유 파일이면서
     * 남은 스티커 중 같은 원본을 참조하는 것이
     * 하나도 없을 때만 실제 파일을 지운다.
     */
    fun deleteOriginalIfUnreferenced(
        context: Context,
        deletedUri: Uri?,
        remainingStickers: List<PhotoStickerItem>
    ) {
        if (deletedUri == null || deletedUri.scheme != "file") {
            return
        }

        val path =
            deletedUri.path
                ?: return

        val targetFile =
            File(path).canonicalFile

        val originalsRoot =
            File(
                context.filesDir,
                ORIGINALS_DIRECTORY_NAME
            ).canonicalFile

        if (
            !targetFile.path.startsWith(
                originalsRoot.path
            )
        ) {
            return
        }

        val stillReferenced =
            remainingStickers.any { sticker ->
                sticker.originalUri == deletedUri
            }

        if (!stillReferenced && targetFile.exists()) {
            targetFile.delete()
        }
    }
}
