package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix

/**
 * 엽서 앞↔뒤 전환 전체 시간. 방문 달력 Page Curl(650ms)은 한 장을 걷는
 * 동작이고, 엽서는 같은 종이를 끝까지 넘기는 동작이라 조금 더 길게 둔다.
 * 이전 1100ms는 "집어드는" 첫 구간이 길어 굼뜨게 느껴졌다.
 */
internal const val POSTCARD_FLIP_DURATION_MS = 800

/** 접힘선(점 [creasePoint], 단위 법선 [normal])에 대한 거울 반사. */
internal fun reflectAcrossCrease(point: Offset, creasePoint: Offset, normal: Offset): Offset {
    val distance =
        (point.x - creasePoint.x) * normal.x + (point.y - creasePoint.y) * normal.y
    return Offset(
        point.x - 2f * distance * normal.x,
        point.y - 2f * distance * normal.y
    )
}

/**
 * [reflectAcrossCrease]와 같은 변환을 그리기용 Matrix에 쓴다. 넘어가는 모서리
 * 부분을 이 변환으로 다시 그리면 접혀 올라간 종이 날개가 된다(방문 달력
 * Page Curl과 같은 원리).
 */
internal fun Matrix.setCreaseReflection(creasePoint: Offset, normal: Offset) {
    reset()
    val offset = creasePoint.x * normal.x + creasePoint.y * normal.y
    this[0, 0] = 1f - 2f * normal.x * normal.x
    this[0, 1] = -2f * normal.x * normal.y
    this[1, 0] = -2f * normal.x * normal.y
    this[1, 1] = 1f - 2f * normal.y * normal.y
    this[3, 0] = 2f * offset * normal.x
    this[3, 1] = 2f * offset * normal.y
}
