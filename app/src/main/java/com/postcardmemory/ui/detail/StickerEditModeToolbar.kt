package com.postcardmemory.ui.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.postcardmemory.ui.components.EditorActionDivider
import com.postcardmemory.ui.components.EditorTextAction

/**
 * 53일차 제8차 Selected-object Action Box 철거 파일럿: 이 툴바를 감싸던
 * 둥근 회색 배경과, 각 항목을 감싸던 개별 filled 버튼 컨테이너를 없애고
 * 공용 EditorTextAction/EditorActionDivider로 평면화했다. 상태 판단(배경제거
 * 가능 여부에 따른 문구 교체, 레이어 이동 disabled 조건)과 제스처 로직은
 * 전혀 바뀌지 않았다 — 시각적 container만 없앴다.
 *
 * 이미지처리(배경제거·원본복원)와 쌓임순서(뒤로·앞으로)가 성격이 다른
 * action이라는 걸 보여주기 위해 그 사이에만 구분선을 둔다(뒤로·앞으로
 * 사이는 한 그룹이라 구분선 없음). 이동/크기/회전/좌우·상하대칭은
 * pinch·twist 제스처가 이미 처리하므로 툴바에서 뺐다(기존 결정 유지).
 */
@Composable
internal fun StickerEditModeToolbar(
    sticker: PhotoStickerItem,
    isRemovingBackground: Boolean,
    onToggleBackgroundRemoval: () -> Unit,
    canMoveForward: Boolean,
    canMoveBackward: Boolean,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorTextAction(
            text =
                when {
                    isRemovingBackground -> "처리중..."
                    sticker.isBackgroundRemoved -> "원본복원"
                    else -> "배경제거"
                },
            enabled = enabled && !isRemovingBackground,
            onClick = onToggleBackgroundRemoval
        )

        EditorActionDivider()

        EditorTextAction(
            text = "뒤로",
            enabled = enabled && canMoveBackward,
            onClick = onMoveBackward
        )

        EditorTextAction(
            text = "앞으로",
            enabled = enabled && canMoveForward,
            onClick = onMoveForward
        )
    }
}
