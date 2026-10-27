package com.postcardmemory.ui.gallery

enum class GalleryPlayMode {
    NONE,
    POND,
    // 동글이 쉼터. 예전 "양떼목장" 시절 이름을 일부러 유지한다 — GalleryScreen의 PlayModeSaver가
    // 이 이름 문자열을 저장 상태(rememberSaveable)에 넣고 valueOf로 되읽으므로, 이름을 바꾸면
    // 이전 빌드가 남긴 저장 상태를 복원할 때 예외가 난다.
    SHEEP_RANCH,
    RACE
}
