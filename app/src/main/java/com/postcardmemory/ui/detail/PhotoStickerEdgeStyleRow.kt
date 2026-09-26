package com.postcardmemory.ui.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.postcardmemory.ui.components.EditorFlatPresetTile
import com.postcardmemory.ui.components.EditorQuietHint

/**
 * 선택한 사진 스티커의 오림 스타일 선택줄. 각 타일은 그 스티커 사진을 실제
 * 렌더러(photoStickerPaper)로 작게 그려 보여준다. 누끼 상태에서는 저장된
 * 스타일을 그대로 둔 채 선택만 막는다.
 */
@Composable
internal fun PhotoStickerEdgeStyleRow(
    sticker: PhotoStickerItem,
    enabled: Boolean,
    onSelectEdgeStyle: (PhotoStickerEdgeStyle) -> Unit
) {
    val styleEnabled =
        enabled && !sticker.isBackgroundRemoved

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .alpha(if (styleEnabled) 1f else 0.4f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        PhotoStickerEdgeStyle.entries.forEach { style ->
            EditorFlatPresetTile(
                onClick = { onSelectEdgeStyle(style) },
                enabled = styleEnabled,
                previewModifier = Modifier.size(52.dp),
                label = style.label,
                selected = sticker.edgeStyle == style
            ) {
                val paperSpec =
                    photoStickerPaperSpec(
                        style = style,
                        edgeSeed = sticker.edgeSeed
                    )

                AsyncImage(
                    model = sticker.originalUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        if (paperSpec != null) {
                            Modifier
                                .fillMaxSize()
                                .photoStickerPaper(
                                    spec = paperSpec,
                                    flipHorizontal = false,
                                    flipVertical = false,
                                    selectionColor = null
                                )
                        } else {
                            // 편집 미리보기와 같은 비율(16dp / 120dp)의 둥근 모서리.
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(5.dp))
                        }
                )
            }
        }
    }

    if (sticker.isBackgroundRemoved) {
        Spacer(modifier = Modifier.height(6.dp))

        EditorQuietHint(
            text = "배경제거 중에는 오림을 쉬어. 원본복원하면 다시 보여."
        )
    }
}
