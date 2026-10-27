package com.postcardmemory.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

/**
 * EXIF 방향값 하나가 뜻하는 변환 — 시계 방향으로 [rotationDegrees]만큼 돌린 뒤
 * [mirrorAfterRotation]이면 좌우를 뒤집는다.
 */
internal data class ExifOrientationTransform(
    val rotationDegrees: Float,
    val mirrorAfterRotation: Boolean
) {
    /** 90°/270° 회전이 들어가면 화면에 보이는 가로·세로가 저장된 픽셀과 뒤바뀐다. */
    val swapsWidthAndHeight: Boolean
        get() = rotationDegrees == 90f || rotationDegrees == 270f
}

/**
 * 방향값 → 변환. NORMAL·UNDEFINED와 1~8 밖의 잘못된 값은 손대지 않는다(null).
 * 촬영 사진 자르기([ImageUtils]), 스티커 내보내기([PostcardImageExporter]),
 * API 28 미만 사진 렌더링([PostcardRenderSpec])이 각자 복사해 두던 표를 107일차에 하나로 모았다.
 */
internal fun exifOrientationTransform(orientation: Int): ExifOrientationTransform? =
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> ExifOrientationTransform(0f, mirrorAfterRotation = true)
        ExifInterface.ORIENTATION_ROTATE_180 -> ExifOrientationTransform(180f, mirrorAfterRotation = false)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> ExifOrientationTransform(180f, mirrorAfterRotation = true)
        ExifInterface.ORIENTATION_TRANSPOSE -> ExifOrientationTransform(90f, mirrorAfterRotation = true)
        ExifInterface.ORIENTATION_ROTATE_90 -> ExifOrientationTransform(90f, mirrorAfterRotation = false)
        ExifInterface.ORIENTATION_TRANSVERSE -> ExifOrientationTransform(270f, mirrorAfterRotation = true)
        ExifInterface.ORIENTATION_ROTATE_270 -> ExifOrientationTransform(270f, mirrorAfterRotation = false)
        else -> null
    }

/**
 * [bitmap]을 EXIF 방향대로 세운 새 비트맵을 돌려준다. 변환이 없으면 [bitmap]을 그대로 돌려준다.
 * 원본 recycle 여부는 호출부마다 달라서(촬영 사진 자르기는 하지 않음) 여기서 정하지 않는다.
 * 행렬은 예전 세 벌과 같다 — 회전은 setRotate, 반전은 그 뒤 postScale(-1, 1)이고,
 * 회전 없는 좌우 반전은 단위 행렬에 postScale을 해 예전 setScale(-1, 1)과 같은 값이 된다.
 */
internal fun rotateBitmapForExifOrientation(
    bitmap: Bitmap,
    orientation: Int
): Bitmap {
    val transform = exifOrientationTransform(orientation) ?: return bitmap
    val matrix = Matrix()
    if (transform.rotationDegrees != 0f) {
        matrix.setRotate(transform.rotationDegrees)
    }
    if (transform.mirrorAfterRotation) {
        matrix.postScale(-1f, 1f)
    }
    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}
