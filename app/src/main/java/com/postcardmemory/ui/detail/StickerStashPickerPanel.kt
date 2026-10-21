package com.postcardmemory.ui.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.postcardmemory.ui.components.EditorFlatPresetTile
import com.postcardmemory.ui.components.EditorQuietHint
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.utils.StashedPhotoSticker

/** 스티커 탭 하위 줄 `사진 | 텍스트 | 라벨 | 스티커 통`에서 스티커 통의 자리. */
internal const val STICKER_STASH_SUB_TAB_INDEX = 3

/**
 * 110일차: 스티커 통에서 꺼낸 조각의 모양(오림 가장자리·뒤집기). 조준 미리보기는 기존처럼
 * 사진만 보여 주고, 핀셋이 닿아 [aimStickerId] 스티커가 만들어지는 순간 [applyTo]로 입힌다.
 * 다른 조준에서 만든 스티커에는 아무것도 바꾸지 않는다.
 */
internal data class StashedStickerShape(
    val aimStickerId: String,
    val edgeStyle: PhotoStickerEdgeStyle,
    val edgeSeed: Long,
    val flipHorizontal: Boolean,
    val flipVertical: Boolean
) {
    fun applyTo(item: PhotoStickerItem): PhotoStickerItem =
        if (item.id != aimStickerId) {
            item
        } else {
            item.copy(
                edgeStyle = edgeStyle,
                edgeSeed = edgeSeed,
                flipHorizontal = flipHorizontal,
                flipVertical = flipVertical
            )
        }
}

internal fun StashedPhotoSticker.shapeFor(aimStickerId: String): StashedStickerShape =
    StashedStickerShape(
        aimStickerId = aimStickerId,
        edgeStyle = edgeStyle,
        edgeSeed = edgeSeed,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical
    )

/**
 * 스티커 탭의 `스티커 통` 칸. 사진 칸의 스티커 목록과 같은 가로 줄 타일 문법을 쓰고,
 * 조각을 누르면 사진 칸으로 넘어가 기존 핀셋 조준을 시작한다. 통의 조각은 그대로 남는다.
 */
@Composable
internal fun StickerStashPickerPanel(
    stashedStickers: List<StashedPhotoSticker>,
    enabled: Boolean,
    onPick: (StashedPhotoSticker) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stashedStickers.size}조각",
                color = GraphiteAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (stashedStickers.isEmpty()) {
            EditorQuietHint(
                text = "아직 스티커 통이 비어 있어. 사진 칸에서 스티커를 고르고 '통에 넣기'를 누르면 여기 모여."
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                stashedStickers.forEach { sticker ->
                    EditorFlatPresetTile(
                        onClick = { onPick(sticker) },
                        enabled = enabled,
                        previewModifier = Modifier.size(56.dp)
                    ) {
                        AsyncImage(
                            model = sticker.displayedFile,
                            contentDescription = "스티커 통 조각",
                            contentScale =
                                if (sticker.isBackgroundRemoved) {
                                    ContentScale.Fit
                                } else {
                                    ContentScale.Crop
                                },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            EditorQuietHint(
                text = "조각을 누르면 엽서 위로 집어 와. 통에 있는 조각은 그대로 남아."
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
