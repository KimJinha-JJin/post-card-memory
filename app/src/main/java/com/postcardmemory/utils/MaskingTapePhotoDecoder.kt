package com.postcardmemory.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * 마스킹테이프 "사진" 디자인은 사진을 반복 타일링하는 질감으로 쓰므로
 * 원본 해상도가 필요 없다 — 메모리 절약을 위해 긴 변을 maxDimension으로
 * 제한해 디코드한다. Compose 미리보기(ui/components/MaskingTapeShapes.kt)와
 * Canvas export(PostcardImageExporter.kt) 양쪽이 이 함수 하나를 공유해서
 * 같은 비트맵을 얻으므로 화면과 저장 이미지의 타일 패턴이 어긋나지 않는다.
 */
object MaskingTapePhotoDecoder {

    private const val DEFAULT_MAX_DIMENSION = 512

    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Bitmap? = runCatching {
        val boundsOptions =
            BitmapFactory.Options().apply { inJustDecodeBounds = true }

        // inJustDecodeBounds=true인 decodeStream은 성공해도 항상 null을 돌려주고
        // outWidth/outHeight만 채운다. 그래서 이 호출 결과로 실패를 판정하면
        // 안 된다 — 스트림이 정상이어도 매번 null로 빠져 사진이 영영 그려지지
        // 않았다. 실제 실패는 바로 아래 outWidth/outHeight 검사로 걸러진다.
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        }

        val sourceWidth = boundsOptions.outWidth
        val sourceHeight = boundsOptions.outHeight

        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return@runCatching null
        }

        var sampleSize = 1
        while (
            sourceWidth / sampleSize > maxDimension ||
            sourceHeight / sampleSize > maxDimension
        ) {
            sampleSize *= 2
        }

        val decodeOptions =
            BitmapFactory.Options().apply { inSampleSize = sampleSize }

        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }.getOrNull()
}
