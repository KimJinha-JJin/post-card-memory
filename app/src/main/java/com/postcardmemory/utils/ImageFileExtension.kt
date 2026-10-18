package com.postcardmemory.utils

/**
 * 갤러리 등에서 고른 원본 사진을 앱 폴더에 복사할 때 붙이는 확장자. 파일 이름이 아니라
 * ContentResolver가 알려준 MIME 타입으로 정하며, 모르는 타입·null은 ".jpg"다.
 * 사진 스티커와 사진 테이프 저장소가 똑같이 복사해 두던 표를 107일차에 하나로 모았다.
 */
internal fun imageFileExtensionForMimeType(mimeType: String?): String =
    when (mimeType?.lowercase()) {
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        "image/heic",
        "image/heif" -> ".heic"
        else -> ".jpg"
    }
