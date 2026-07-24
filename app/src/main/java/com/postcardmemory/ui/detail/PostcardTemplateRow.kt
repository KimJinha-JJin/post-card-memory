package com.postcardmemory.ui.detail

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.utils.PostcardRenderSpec
import java.io.File

/**
 * 사진 탭의 "엽서 레이아웃" 바로 아래에 놓이는 템플릿 한 줄. 추천/내 템플릿을
 * 각각 별도 인스턴스로 호출한다(섞어서 하나의 목록으로 만들지 않음).
 * 미리보기는 현재 엽서의 실제 사진 위에 템플릿 스타일을 적용해 보여준다 —
 * 새 렌더 경로를 만들지 않고 PostcardRenderSpec.drawBaseContent를 그대로 재사용한다.
 */
@Composable
fun PostcardTemplateSection(
    title: String,
    templates: List<PostcardTemplate>,
    sourceBitmap: Bitmap?,
    selectedTemplateId: String?,
    onSelect: (PostcardTemplate) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null,
    onRequestRename: ((PostcardTemplate) -> Unit)? = null,
    onRequestOverwrite: ((PostcardTemplate) -> Unit)? = null,
    onRequestDelete: ((PostcardTemplate) -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null
) {
    if (templates.isEmpty() && leadingContent == null) {
        return
    }

    Column(
        modifier = modifier.alpha(if (enabled) 1f else 0.55f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (onUndo != null && onRedo != null) {
                EditorUndoRedoButtons(
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    enabled = enabled,
                    undoContentDescription = "템플릿 적용 실행 취소",
                    redoContentDescription = "템플릿 적용 다시 실행"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            leadingContent?.invoke()

            templates.forEach { template ->
                val canManage =
                    template.source == TemplateSource.USER &&
                            onRequestRename != null &&
                            onRequestOverwrite != null &&
                            onRequestDelete != null

                PostcardTemplateCard(
                    template = template,
                    sourceBitmap = sourceBitmap,
                    selected = template.id == selectedTemplateId,
                    enabled = enabled,
                    onClick = { onSelect(template) },
                    manageMenuContent =
                        if (canManage) {
                            {
                                TemplateManageMenuButton(
                                    enabled = enabled,
                                    onRename = { onRequestRename(template) },
                                    onOverwrite = { onRequestOverwrite(template) },
                                    onDelete = { onRequestDelete(template) }
                                )
                            }
                        } else {
                            null
                        }
                )
            }
        }
    }
}

@Composable
fun PostcardTemplateCard(
    template: PostcardTemplate,
    sourceBitmap: Bitmap?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    manageMenuContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(PaperField)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) SunsetGold else PaperDivider,
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            TemplateStylePreview(
                sourceBitmap = sourceBitmap,
                style = template.style
            )

            if (manageMenuContent != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                ) {
                    manageMenuContent()
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = template.name,
            color = BrutalBlack,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 80.dp)
        )
    }
}

@Composable
private fun TemplateManageMenuButton(
    enabled: Boolean,
    onRename: () -> Unit,
    onOverwrite: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = Color.White.copy(alpha = 0.88f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "템플릿 관리",
                tint = BrutalBlack,
                modifier = Modifier.size(14.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("이름 변경") },
                onClick = {
                    expanded = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text("현재 꾸밈으로 덮어쓰기") },
                onClick = {
                    expanded = false
                    onOverwrite()
                }
            )
            DropdownMenuItem(
                text = { Text("삭제") },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun TemplateStylePreview(
    sourceBitmap: Bitmap?,
    style: PostcardTemplateStyle,
    modifier: Modifier = Modifier
) {
    if (sourceBitmap == null || sourceBitmap.isRecycled) {
        return
    }

    Canvas(modifier = modifier.fillMaxWidth()) {
        drawIntoCanvas { canvas ->
            PostcardRenderSpec.drawBaseContent(
                canvas = canvas.nativeCanvas,
                sourceBitmap = sourceBitmap,
                backgroundColorArgb = style.backgroundColorArgb,
                backgroundPattern = style.backgroundPattern,
                message = "",
                messageFont = style.messageFont,
                layoutStyle = style.layoutStyle,
                capturedAt = System.currentTimeMillis(),
                dateFormat = style.dateFormat,
                targetSize = size.width,
                messageTextScale = style.messageTextScale,
                dateTextScale = style.dateTextScale,
                backgroundPatternDensity = style.backgroundPatternDensity,
                stampPhotoScale = style.stampPhotoScale,
                polaroidPhotoScale = style.polaroidPhotoScale,
                photoEdgeBlur = style.photoEdgeBlur,
                stampPhotoOffsetX = style.stampPhotoOffsetX,
                stampPhotoOffsetY = style.stampPhotoOffsetY,
                polaroidPhotoOffsetX = style.polaroidPhotoOffsetX,
                polaroidPhotoOffsetY = style.polaroidPhotoOffsetY,
                tapedFilmPhotoOffsetX = style.tapedFilmPhotoOffsetX,
                tapedFilmPhotoOffsetY = style.tapedFilmPhotoOffsetY,
                stampPhotoZoom = style.stampPhotoZoom,
                polaroidPhotoZoom = style.polaroidPhotoZoom,
                tapedFilmPhotoZoom = style.tapedFilmPhotoZoom
            )
        }
    }
}

/**
 * 템플릿 카드 전용으로 원본 사진을 한 번만 디코드해 모든 카드가 같은
 * Bitmap을 공유한다(카드마다 다시 디코드하면 템플릿 개수만큼 중복 디코드가
 * 발생한다). 이 Composable을 떠나면 recycle한다.
 */
@Composable
fun rememberTemplatePreviewBitmap(imagePath: String): Bitmap? {
    val bitmap =
        remember(imagePath) {
            runCatching {
                PostcardRenderSpec.decodeSourceBitmap(File(imagePath))
            }.getOrNull()
        }

    DisposableEffect(bitmap) {
        onDispose {
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    return bitmap
}
