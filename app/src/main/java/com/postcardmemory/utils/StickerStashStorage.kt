package com.postcardmemory.utils

import android.graphics.BitmapFactory
import com.postcardmemory.ui.detail.PhotoStickerEdgeStyle
import com.postcardmemory.ui.detail.parsePhotoStickerEdgeSeed
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * 110일차: 스티커 통 — 엽서와 독립된 사진 스티커 보관함.
 *
 * ```
 * filesDir/sticker_stash/index.txt            보관 목록(한 줄 = 한 조각)
 * filesDir/sticker_stash/items/<id>/original.* 원본 사진의 독립 복사본
 * filesDir/sticker_stash/items/<id>/cutout.png 배경제거 그림의 독립 복사본(있을 때만)
 * ```
 *
 * 엽서 파일(sticker_originals/<postcardId>/, sticker_bgs/<postcardId>/)을 참조하지 않고 늘
 * 복사해 둔다. 그래서 엽서를 지워도 통의 조각은 남고(엽서 삭제·고아 진단은
 * [DecorationStateFile]·[PostcardAssetDirectory] 목록만 돌며 이 폴더를 모른다),
 * 통에서 빼도 엽서의 스티커 파일은 그대로다. 다시 쓸 때도 엽서 몫으로 새로 복사한다.
 *
 * 쓰기 순서: 조각 파일을 먼저 다 쓰고 검증한 뒤 목록을 원자적으로 교체한다. 목록 교체가
 * 실패하면 방금 쓴 파일을 지워 목록에 없는 파일만 남긴다(목록에 있는데 파일이 없는 상태는
 * 만들지 않는다). 빼기는 반대로 목록을 먼저 바꾸고 성공한 뒤에만 파일을 지운다.
 *
 * 순수 JVM 코드로 두어 JUnit에서 임시 폴더로 검증한다. 그림 판독([isReadableImage])과 원본
 * 읽기([PhotoStickerStashSource])만 호출자가 넘긴다.
 */
internal object StickerStashStorage {

    const val DIRECTORY_NAME = "sticker_stash"
    const val MAX_STASHED_STICKERS = 60

    private const val INDEX_FILE_NAME = "index.txt"
    private const val ITEMS_DIRECTORY_NAME = "items"
    private const val ORIGINAL_FILE_PREFIX = "original"
    private const val CUTOUT_FILE_NAME = "cutout.png"
    private const val NONE = "~"
    private val ID_PATTERN = Regex("^[0-9a-fA-F-]{36}$")
    private val EXTENSION_PATTERN = Regex("^\\.[a-z0-9]{1,5}$")

    // 갤러리 사이드바와 편집 화면이 같은 목록을 고칠 수 있어 읽기-고치기-쓰기를 한 줄로 세운다.
    private val lock = Any()

    fun root(filesDir: File): File = File(filesDir, DIRECTORY_NAME)

    /** 최근에 담은 순서. 목록에는 있지만 파일이 사라진 조각은 보여 주지 않는다(목록은 그대로 둔다). */
    fun list(filesDir: File): List<StashedPhotoSticker> =
        synchronized(lock) {
            val root = root(filesDir)
            readIndexLines(root)
                .orEmpty()
                .mapNotNull { parseLine(root, it) }
                .filter { sticker ->
                    sticker.originalFile.isFile &&
                        (sticker.cutoutFile == null || sticker.cutoutFile.isFile)
                }
                .sortedByDescending { it.createdAtMillis }
        }

    fun stash(
        filesDir: File,
        source: PhotoStickerStashSource,
        isReadableImage: (File) -> Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        writeIndex: (File, String) -> Boolean = ConfirmedEditStateStorage::writeTextAtomically
    ): StickerStashResult {
        return synchronized(lock) {
            val root = root(filesDir)
            // 목록을 읽지 못했는데 새 목록을 쓰면 기존 조각이 목록에서 사라진다.
            val lines = readIndexLines(root) ?: return StickerStashResult.Failed
            val existing = lines.mapNotNull { parseLine(root, it) }
            if (existing.size >= MAX_STASHED_STICKERS) return StickerStashResult.Full

            val id = UUID.randomUUID().toString()
            val itemDirectory = File(File(root, ITEMS_DIRECTORY_NAME), id)
            val extension =
                source.originalExtension.lowercase().takeIf { EXTENSION_PATTERN.matches(it) } ?: ".jpg"
            try {
                if (!itemDirectory.mkdirs()) return StickerStashResult.Failed

                val originalFile = File(itemDirectory, ORIGINAL_FILE_PREFIX + extension)
                val originalDigest =
                    copyWithDigest(source.openOriginal, originalFile)
                        ?: return failAndClean(itemDirectory)
                if (!isReadableImage(originalFile)) return failAndClean(itemDirectory)

                val openCutout = source.openCutout
                val cutoutDigest =
                    if (openCutout == null) {
                        NONE
                    } else {
                        val cutoutFile = File(itemDirectory, CUTOUT_FILE_NAME)
                        val digest =
                            copyWithDigest(openCutout, cutoutFile) ?: return failAndClean(itemDirectory)
                        if (!isReadableImage(cutoutFile)) return failAndClean(itemDirectory)
                        digest
                    }

                val sticker =
                    StashedPhotoSticker(
                        id = id,
                        createdAtMillis = nowMillis,
                        originalFile = originalFile,
                        cutoutFile = if (openCutout == null) null else File(itemDirectory, CUTOUT_FILE_NAME),
                        isBackgroundRemoved = source.isBackgroundRemoved && openCutout != null,
                        edgeStyle = source.edgeStyle,
                        edgeSeed = source.edgeSeed,
                        flipHorizontal = source.flipHorizontal,
                        flipVertical = source.flipVertical,
                        contentKey = ""
                    ).let { it.copy(contentKey = contentKeyOf(it, originalDigest, cutoutDigest)) }

                // 같은 그림·같은 모양을 두 번 담지 않는다(다른 엽서에 다시 붙인 복사본도 같은 조각).
                if (existing.any { it.contentKey == sticker.contentKey }) {
                    itemDirectory.deleteRecursively()
                    return StickerStashResult.AlreadyStashed
                }

                val content = (lines + serialize(sticker)).joinToString(separator = "\n", postfix = "\n")
                if (!writeIndex(File(root, INDEX_FILE_NAME), content)) return failAndClean(itemDirectory)

                StickerStashResult.Stashed(sticker)
            } catch (_: Exception) {
                failAndClean(itemDirectory)
            }
        }
    }

    /**
     * 통에서 뺀다. 목록을 먼저 바꾸고, 그게 성공했을 때만 조각 폴더를 지운다. 엽서 쪽 파일은
     * 애초에 이 폴더 밖이라 건드릴 수 없다. 목록에 없던 id는 true(이미 빠진 상태).
     */
    fun remove(
        filesDir: File,
        stickerId: String,
        writeIndex: (File, String) -> Boolean = ConfirmedEditStateStorage::writeTextAtomically
    ): Boolean {
        return synchronized(lock) {
            if (!ID_PATTERN.matches(stickerId)) return false
            val root = root(filesDir)
            val lines = readIndexLines(root) ?: return false
            val remaining = lines.filterNot { it.substringBefore('\t') == stickerId }
            if (remaining.size != lines.size) {
                val content =
                    if (remaining.isEmpty()) "" else remaining.joinToString(separator = "\n", postfix = "\n")
                if (!writeIndex(File(root, INDEX_FILE_NAME), content)) return false
            }
            val itemsRoot = File(root, ITEMS_DIRECTORY_NAME)
            val itemDirectory = File(itemsRoot, stickerId)
            if (itemDirectory.exists() && isInsideDirectory(itemsRoot, itemDirectory)) {
                itemDirectory.deleteRecursively()
            }
            true
        }
    }

    /** 목록 줄(빈 줄 제외). 파일이 없으면 빈 목록, 읽기 실패면 null. 해석 못 한 줄도 그대로 보존한다. */
    private fun readIndexLines(root: File): List<String>? {
        val indexFile = File(root, INDEX_FILE_NAME)
        if (!indexFile.exists()) return emptyList()
        return try {
            indexFile.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
        } catch (_: IOException) {
            null
        }
    }

    private fun failAndClean(itemDirectory: File): StickerStashResult {
        itemDirectory.deleteRecursively()
        return StickerStashResult.Failed
    }

    /** 끝까지 복사하고 빈 파일이 아님을 확인한 뒤 내용의 SHA-256을 돌려준다. 실패하면 null. */
    private fun copyWithDigest(open: () -> InputStream?, target: File): String? {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = open() ?: return null
        input.use { stream ->
            target.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
            }
        }
        if (!target.isFile || target.length() <= 0L) return null
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun contentKeyOf(sticker: StashedPhotoSticker, originalDigest: String, cutoutDigest: String): String =
        listOf(
            originalDigest,
            cutoutDigest,
            sticker.isBackgroundRemoved,
            sticker.edgeStyle.storedName,
            sticker.edgeSeed,
            sticker.flipHorizontal,
            sticker.flipVertical
        ).joinToString("|")

    private fun serialize(sticker: StashedPhotoSticker): String =
        listOf(
            sticker.id,
            sticker.createdAtMillis.toString(),
            sticker.originalFile.name,
            sticker.cutoutFile?.name ?: NONE,
            sticker.isBackgroundRemoved.toString(),
            sticker.edgeStyle.storedName,
            sticker.edgeSeed.toString(),
            sticker.flipHorizontal.toString(),
            sticker.flipVertical.toString(),
            sticker.contentKey
        ).joinToString("\t")

    private fun parseLine(root: File, line: String): StashedPhotoSticker? {
        val p = line.split("\t")
        if (p.size < 10) return null
        val id = p[0].takeIf { ID_PATTERN.matches(it) } ?: return null
        val originalName = p[2].takeIf { it.startsWith(ORIGINAL_FILE_PREFIX) && '/' !in it && '\\' !in it }
            ?: return null
        val itemDirectory = File(File(root, ITEMS_DIRECTORY_NAME), id)
        return StashedPhotoSticker(
            id = id,
            createdAtMillis = p[1].toLongOrNull() ?: return null,
            originalFile = File(itemDirectory, originalName),
            cutoutFile = if (p[3] == CUTOUT_FILE_NAME) File(itemDirectory, CUTOUT_FILE_NAME) else null,
            isBackgroundRemoved = p[4].toBoolean() && p[3] == CUTOUT_FILE_NAME,
            edgeStyle = PhotoStickerEdgeStyle.fromStored(p[5]),
            edgeSeed = parsePhotoStickerEdgeSeed(p[6]),
            flipHorizontal = p[7].toBoolean(),
            flipVertical = p[8].toBoolean(),
            contentKey = p[9]
        )
    }
}

/** 통에 담긴 사진 스티커 한 조각. 위치·크기·회전은 담지 않는다 — 붙일 때 새로 정한다. */
internal data class StashedPhotoSticker(
    val id: String,
    val createdAtMillis: Long,
    val originalFile: File,
    val cutoutFile: File?,
    val isBackgroundRemoved: Boolean,
    val edgeStyle: PhotoStickerEdgeStyle,
    val edgeSeed: Long,
    val flipHorizontal: Boolean,
    val flipVertical: Boolean,
    val contentKey: String
) {
    /** 화면에 보일 그림(배경제거가 켜져 있으면 누끼). */
    val displayedFile: File get() = if (isBackgroundRemoved && cutoutFile != null) cutoutFile else originalFile
}

/** 담을 스티커의 원본 읽기와 모양 정보. [openCutout]이 null이면 누끼 없이 담는다. */
internal class PhotoStickerStashSource(
    val openOriginal: () -> InputStream?,
    val originalExtension: String,
    val openCutout: (() -> InputStream?)?,
    val isBackgroundRemoved: Boolean,
    val edgeStyle: PhotoStickerEdgeStyle,
    val edgeSeed: Long,
    val flipHorizontal: Boolean,
    val flipVertical: Boolean
)

internal sealed interface StickerStashResult {
    data class Stashed(val sticker: StashedPhotoSticker) : StickerStashResult
    data object AlreadyStashed : StickerStashResult
    data object Full : StickerStashResult
    data object Failed : StickerStashResult
}

/** 앱에서 쓰는 그림 판독: 크기만 읽어 정상 그림인지 본다(전체 decode 없음). */
internal fun isDecodableStashImage(file: File): Boolean {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    return options.outWidth > 0 && options.outHeight > 0
}
