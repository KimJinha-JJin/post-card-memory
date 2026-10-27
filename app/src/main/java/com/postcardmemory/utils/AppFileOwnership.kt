package com.postcardmemory.utils

import java.io.File

/**
 * [file]이 [root] 디렉터리 **안에** 있는지 판정한다. 파일 삭제 가드 전용이다.
 *
 * `file.path.startsWith(root.path)`처럼 문자열 앞부분만 비교하면 이름이 비슷한
 * 형제 디렉터리가 통과한다 — root가 `.../sticker_originals`일 때
 * `.../sticker_originals_backup/a.jpg`가 "안에 있다"로 잘못 판정되고, 그 판정이
 * 곧바로 `delete()`로 이어진다. 그래서 경로 조각 경계(`File.separatorChar`)까지
 * 확인한다.
 *
 * - `..`이 섞인 경로는 canonical 변환으로 먼저 정규화한 뒤 판정한다.
 * - [root] 그 자체는 false다 — 디렉터리는 삭제 대상 자산 파일이 아니다.
 * - canonical 변환이 실패하면(IO/보안 오류) 안전한 쪽인 false로 본다.
 *
 * 파일이 실제로 존재하는지는 보지 않는다. 삭제 가드는 존재 확인보다 먼저
 * "이건 우리 것인가"를 물어야 하기 때문이다.
 */
internal fun isInsideDirectory(root: File, file: File): Boolean =
    runCatching {
        val rootPath = root.canonicalPath
        val filePath = file.canonicalPath

        filePath.length > rootPath.length &&
                filePath.startsWith(rootPath) &&
                filePath[rootPath.length] == File.separatorChar
    }.getOrDefault(false)
