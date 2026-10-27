package com.postcardmemory

import android.app.Application
import android.util.Log
import com.postcardmemory.utils.PostcardTempCleanup
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class PostCardMemoryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            val cleanupResult =
                PostcardTempCleanup.cleanup(filesDir)

            cleanupResult.failedFiles.forEach { path ->
                Log.w(
                    "PostcardTempCleanup",
                    "오래된 카메라 임시 파일을 삭제하지 못함: $path"
                )
            }
        }
    }
}
