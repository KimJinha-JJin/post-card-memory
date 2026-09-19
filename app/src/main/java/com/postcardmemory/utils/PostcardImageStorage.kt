package com.postcardmemory.utils

import android.content.Context
import java.io.File

object PostcardImageStorage {

    /**
     * 중심 사진 교체 성공 후 이전 파일을 정리할 때 사용한다. imagePath는 항상
     * 앱이 filesDir 하위에 만든 경로이지만(외부 content:// URI가 그대로
     * 저장될 일은 없음), 방어적으로 실제 filesDir 내부 경로인지 확인한 뒤에만
     * 삭제한다 — 사용자의 갤러리 원본이나 외부 파일을 잘못 지우지 않기 위함.
     */
    fun deleteIfOwnedByApp(
        context: Context,
        path: String?
    ) {
        deleteIfOwnedByApp(context.filesDir, path)
    }

    /** internal: Context 없이 java.io.File만으로 순수 JUnit에서 검증하기 위한 오버로드. */
    internal fun deleteIfOwnedByApp(
        filesDir: File,
        path: String?
    ) {
        if (path.isNullOrBlank()) {
            return
        }

        val file = File(path)

        val isWithinFilesDir =
            runCatching {
                val root =
                    filesDir.canonicalPath +
                            File.separator
                file.canonicalPath.startsWith(root)
            }.getOrDefault(false)

        if (isWithinFilesDir && file.exists()) {
            file.delete()
        }
    }
}
