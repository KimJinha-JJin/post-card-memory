package com.postcardmemory.utils

import java.io.File

/**
 * 꾸미기 요소별 확정 상태 파일(`filesDir/<directoryName>/<postcardId>.txt`)의 단일 기준.
 *
 * 저장·복원(DetailViewModel의 persist*EditState / readConfirmed*State), 삭제
 * (cleanupPostcardOwnedAssets), 고아 파일 진단(OrphanFileDiagnostics)이 모두 이 목록을
 * 쓴다. 삭제와 진단은 [entries]를 순회하므로, 새 꾸미기 종류를 여기 추가하면 두 경로에
 * 자동으로 포함된다.
 *
 * [directoryName]은 기존 사용자 기기에 이미 저장된 파일 위치다 — 철자·대소문자·단수/복수를
 * 한 글자라도 바꾸면 기존 엽서의 꾸미기가 사라진 것처럼 보이므로 절대 바꾸지 않는다.
 * [assetName]은 삭제 결과(PostcardDeletionResult)와 진단 결과의 type 이름이다.
 */
internal enum class DecorationStateFile(
    val directoryName: String,
    val assetName: String,
    val displayName: String
) {
    PHOTO_STICKER("sticker_states", "stickerState", "스티커"),
    SEAL("seal_states", "sealState", "도장"),
    DOODLE("doodle_states", "doodleState", "낙서"),
    TEXT_STICKER("text_sticker_states", "textStickerState", "텍스트 스티커"),
    MASKING_TAPE("masking_tape_states", "maskingTapeState", "마스킹테이프"),
    LABEL_STICKER("label_sticker_states", "labelStickerState", "라벨 스티커");

    fun file(filesDir: File, postcardId: Long): File =
        File(filesDir, "$directoryName/$postcardId.txt")
}

/**
 * 엽서 한 장이 소유하는 꾸미기 자산 디렉터리(`filesDir/<directoryName>/<postcardId>/`)의
 * 단일 기준. 초안 전용 누끼 디렉터리(draft_sticker_bgs)는 초안 수명과 함께 움직이므로
 * PostcardDraftStorage가 따로 관리한다.
 *
 * [DecorationStateFile]과 같은 이유로 [directoryName]은 절대 바꾸지 않는다.
 * [deletionAssetName]/[orphanType]은 기존 삭제·진단 결과 이름을 그대로 보존한 것이다.
 */
internal enum class PostcardAssetDirectory(
    val directoryName: String,
    val deletionAssetName: String,
    val orphanType: String
) {
    CONFIRMED_STICKER_BACKGROUNDS(
        "sticker_bgs",
        "confirmedStickerBackgrounds",
        "confirmedStickerBackground"
    ),
    STICKER_ORIGINALS(
        "sticker_originals",
        "cameraStickerOriginals",
        "cameraStickerOriginal"
    ),
    MASKING_TAPE_PHOTOS(
        "masking_tape_photos",
        "maskingTapePhotoOriginals",
        "maskingTapePhotoOriginal"
    );

    fun root(filesDir: File): File =
        File(filesDir, directoryName)

    fun directory(filesDir: File, postcardId: Long): File =
        File(filesDir, "$directoryName/$postcardId")
}
