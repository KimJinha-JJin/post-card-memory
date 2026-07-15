package com.postcardmemory.ui.detail

import android.net.Uri
import android.widget.Toast
import java.io.File
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.postcardmemory.ui.components.EditorSlider
import com.postcardmemory.ui.components.PhotoSourceMenu
import com.postcardmemory.ui.components.PostcardBackgroundColorPicker
import com.postcardmemory.ui.components.PostcardBackgroundPattern
import com.postcardmemory.ui.components.PostcardBackgroundPatternPicker
import com.postcardmemory.ui.components.PostcardCustomColorPicker
import com.postcardmemory.ui.components.PostcardDateFormat
import com.postcardmemory.ui.components.PostcardLayoutPicker
import com.postcardmemory.ui.components.PostcardLayoutStyle
import com.postcardmemory.ui.components.PostcardTextFont
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.ScreenBackgroundGray
import com.postcardmemory.ui.theme.SoftGray
import com.postcardmemory.ui.theme.SurfaceGray
import com.postcardmemory.utils.PostcardImageExporter
import com.postcardmemory.utils.PostcardRenderSpec

private enum class StickerEditMode {
    Move,
    Scale,
    Rotate
}

private enum class TextScaleTarget {
    Message,
    Date
}

private fun clampStickerOffset(
    offset: Offset,
    postcardSize: IntSize,
    stickerSize: IntSize
): Offset {
    val maxX =
        (postcardSize.width - stickerSize.width)
            .coerceAtLeast(0)
            .toFloat()
    val maxY =
        (postcardSize.height - stickerSize.height)
            .coerceAtLeast(0)
            .toFloat()

    return Offset(
        x = offset.x.coerceIn(
            minimumValue = 0f,
            maximumValue = maxX
        ),
        y = offset.y.coerceIn(
            minimumValue = 0f,
            maximumValue = maxY
        )
    )
}

private fun centeredStickerOffset(
    postcardSize: IntSize,
    stickerSize: IntSize
): Offset =
    Offset(
        x = ((postcardSize.width - stickerSize.width) / 2f)
            .coerceAtLeast(0f),
        y = ((postcardSize.height - stickerSize.height) / 2f)
            .coerceAtLeast(0f)
    )

private fun localStickerDeltaToParent(
    localDelta: Offset,
    rotationDegrees: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean
): Offset {
    val flippedX =
        if (flipHorizontal) -localDelta.x else localDelta.x
    val flippedY =
        if (flipVertical) -localDelta.y else localDelta.y

    if (rotationDegrees == 0f) {
        return Offset(flippedX, flippedY)
    }

    val radians =
        Math.toRadians(rotationDegrees.toDouble())
    val cos = kotlin.math.cos(radians).toFloat()
    val sin = kotlin.math.sin(radians).toFloat()

    return Offset(
        x = flippedX * cos - flippedY * sin,
        y = flippedX * sin + flippedY * cos
    )
}

private fun createStickerOverlayForExport(
    stickerUri: Uri?,
    originalStickerUri: Uri?,
    isBackgroundRemoved: Boolean,
    rotationDegrees: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    stickerOffset: Offset?,
    postcardSize: IntSize,
    stickerSize: IntSize
): PostcardImageExporter.StickerOverlay? {
    val selectedUri =
        stickerUri
            ?: return null

    if (
        postcardSize.width <= 0 ||
        postcardSize.height <= 0 ||
        stickerSize.width <= 0 ||
        stickerSize.height <= 0
    ) {
        return null
    }

    val resolvedOffset =
        clampStickerOffset(
            offset =
                stickerOffset
                    ?: centeredStickerOffset(
                        postcardSize = postcardSize,
                        stickerSize = stickerSize
                    ),
            postcardSize = postcardSize,
            stickerSize = stickerSize
        )
    return PostcardImageExporter.StickerOverlay(
        uri = selectedUri,
        originalUri = originalStickerUri,
        isBackgroundRemoved = isBackgroundRemoved,
        rotationDegrees = rotationDegrees,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical,
        normalizedX =
            (resolvedOffset.x /
                    postcardSize.width.toFloat())
                .coerceIn(0f, 1f),
        normalizedY =
            (resolvedOffset.y /
                    postcardSize.height.toFloat())
                .coerceIn(0f, 1f),
        sizeRatio =
            stickerSize.width.toFloat() /
                    postcardSize.width.toFloat()
    )
}

private fun createStickerOverlaysForExport(
    photoStickers: List<PhotoStickerItem>,
    postcardSize: IntSize,
    stickerSizes: Map<String, IntSize>
): List<PostcardImageExporter.StickerOverlay> {
    if (
        postcardSize.width <= 0 ||
        postcardSize.height <= 0
    ) {
        return emptyList()
    }

    return photoStickers.mapNotNull { sticker ->
        val stickerSize =
            stickerSizes[sticker.id]
                ?: return@mapNotNull null

        createStickerOverlayForExport(
            stickerUri = sticker.displayedUri,
            originalStickerUri = sticker.originalUri,
            isBackgroundRemoved = sticker.isBackgroundRemoved,
            rotationDegrees = sticker.rotationDegrees,
            flipHorizontal = sticker.flipHorizontal,
            flipVertical = sticker.flipVertical,
            stickerOffset = sticker.offset,
            postcardSize = postcardSize,
            stickerSize = stickerSize
        )
    }
}

private fun createSealOverlayForExport(
    type: SealType,
    colorArgb: Long,
    rotationDegrees: Float,
    sealOffset: Offset?,
    postcardSize: IntSize,
    sealSize: IntSize,
    capturedAtMillis: Long?
): PostcardImageExporter.SealOverlay? {
    if (
        postcardSize.width <= 0 ||
        postcardSize.height <= 0 ||
        sealSize.width <= 0 ||
        sealSize.height <= 0
    ) {
        return null
    }

    val resolvedOffset =
        clampStickerOffset(
            offset =
                sealOffset
                    ?: centeredStickerOffset(
                        postcardSize = postcardSize,
                        stickerSize = sealSize
                    ),
            postcardSize = postcardSize,
            stickerSize = sealSize
        )

    return PostcardImageExporter.SealOverlay(
        type = type.name,
        normalizedX =
            (resolvedOffset.x /
                    postcardSize.width.toFloat())
                .coerceIn(0f, 1f),
        normalizedY =
            (resolvedOffset.y /
                    postcardSize.height.toFloat())
                .coerceIn(0f, 1f),
        sizeRatio =
            sealSize.width.toFloat() /
                    postcardSize.width.toFloat(),
        rotationDegrees = rotationDegrees,
        colorArgb = colorArgb,
        capturedAtMillis = capturedAtMillis
    )
}

private fun createSealOverlaysForExport(
    photoSeals: List<PostcardSealItem>,
    postcardSize: IntSize,
    sealSizes: Map<String, IntSize>,
    capturedAtMillis: Long?
): List<PostcardImageExporter.SealOverlay> {
    if (
        postcardSize.width <= 0 ||
        postcardSize.height <= 0
    ) {
        return emptyList()
    }

    return photoSeals.mapNotNull { seal ->
        val sealSize =
            sealSizes[seal.id]
                ?: return@mapNotNull null

        createSealOverlayForExport(
            type = seal.type,
            colorArgb = seal.colorArgb,
            rotationDegrees = seal.rotationDegrees,
            sealOffset = seal.offset,
            postcardSize = postcardSize,
            sealSize = sealSize,
            capturedAtMillis = capturedAtMillis
        )
    }
}

/**
 * 사진·배경·텍스트 패널이 공유하는 퍼센트 조절 슬라이더.
 *
 * 라벨과 현재값을 한 줄에 두고 그 아래 얇은 EditorSlider만 둔다. 숫자
 * 직접 입력창과 최소·최대 범위 안내 문구는 없앴다. 드래그 중에는 로컬
 * 값을 보여 주고, 손을 떼면 외부 percent(저장 결과)를 그대로 따른다.
 * onPreviewPercentChanged·onPercentConfirmed 계약은 기존 TextSizeControl과
 * 동일해 미리보기·저장·Undo 스냅샷 로직이 그대로 유지된다.
 */
@Composable
private fun EditorPercentSlider(
    label: String,
    percent: Int,
    minPercent: Int,
    maxPercent: Int,
    enabled: Boolean,
    onPreviewPercentChanged: (Int) -> Unit,
    onPercentConfirmed: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var draggingPercent by remember { mutableIntStateOf(percent) }
    val shownPercent = if (isDragging) draggingPercent else percent

    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = BrutalBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$shownPercent%",
                color = GraphiteAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        EditorSlider(
            value = shownPercent.toFloat(),
            onValueChange = { newValue ->
                val snappedPercent =
                    ((newValue / 5f).roundToInt() * 5)
                        .coerceIn(minPercent, maxPercent)

                isDragging = true
                draggingPercent = snappedPercent
                onPreviewPercentChanged(snappedPercent)
            },
            onValueChangeFinished = {
                isDragging = false
                onPercentConfirmed(draggingPercent)
            },
            valueRange =
                minPercent.toFloat()..maxPercent.toFloat(),
            steps =
                ((maxPercent - minPercent) / 5) - 1,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 세 패널의 보조 행동(사진 바꾸기·색 가져오기·직접 고르기·문구 편집)에
 * 쓰는 가벼운 외곽선 버튼. 주요 선택지와 달리 검은 풀폭 버튼으로 강조하지
 * 않고, 아이콘+라벨만 얇은 테두리로 보여 준다.
 */
@Composable
private fun EditorSecondaryButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceGray),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BrutalBlack
        ),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )

        Spacer(
            modifier = Modifier.size(6.dp)
        )

        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StickerEditModeToolbar(
    sticker: PhotoStickerItem,
    editMode: StickerEditMode,
    onModeSelected: (StickerEditMode) -> Unit,
    isRemovingBackground: Boolean,
    onToggleBackgroundRemoval: () -> Unit,
    onToggleFlipHorizontal: () -> Unit,
    onToggleFlipVertical: () -> Unit,
    canMoveForward: Boolean,
    canMoveBackward: Boolean,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .background(
                color = NeutralLight,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StickerEditModeButton(
            label = "이동",
            selected = editMode == StickerEditMode.Move,
            enabled = enabled,
            onClick = {
                onModeSelected(StickerEditMode.Move)
            }
        )

        StickerEditModeButton(
            label = "크기",
            selected = editMode == StickerEditMode.Scale,
            enabled = enabled,
            onClick = {
                onModeSelected(
                    if (editMode == StickerEditMode.Scale) {
                        StickerEditMode.Move
                    } else {
                        StickerEditMode.Scale
                    }
                )
            }
        )

        StickerEditModeButton(
            label = "회전",
            selected = editMode == StickerEditMode.Rotate,
            enabled = enabled,
            onClick = {
                onModeSelected(
                    if (editMode == StickerEditMode.Rotate) {
                        StickerEditMode.Move
                    } else {
                        StickerEditMode.Rotate
                    }
                )
            }
        )

        StickerEditModeButton(
            label = "좌우대칭",
            selected = sticker.flipHorizontal,
            enabled = enabled,
            onClick = onToggleFlipHorizontal
        )

        StickerEditModeButton(
            label = "상하대칭",
            selected = sticker.flipVertical,
            enabled = enabled,
            onClick = onToggleFlipVertical
        )

        StickerEditModeButton(
            label =
                when {
                    isRemovingBackground -> "처리중..."
                    sticker.isBackgroundRemoved -> "원본복원"
                    else -> "배경제거"
                },
            selected = sticker.isBackgroundRemoved,
            enabled = enabled && !isRemovingBackground,
            onClick = onToggleBackgroundRemoval
        )

        StickerEditModeButton(
            label = "뒤로",
            selected = false,
            enabled = enabled && canMoveBackward,
            onClick = onMoveBackward
        )

        StickerEditModeButton(
            label = "앞으로",
            selected = false,
            enabled = enabled && canMoveForward,
            onClick = onMoveForward
        )
    }
}

@Composable
private fun StickerEditModeButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .background(
                color = if (selected) GraphiteAccent else BrutalWhite,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) BrutalWhite else BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 상세 편집 화면 하단에 고정되는 다섯 카테고리(사진·배경·텍스트·스티커·도장)
 * 전환용 도크. 스크롤 콘텐츠 밖 루트 Box에 얹혀 편집 내용을 위아래로
 * 움직여도 위치가 바뀌지 않는다. Pager 상태·선택 표현·탭 이동 콜백은
 * 기존 인라인 탭 바와 동일하게 유지한다.
 */
@Composable
private fun EditorBottomTabBar(
    selectedPage: Int,
    labels: List<String>,
    icons: List<ImageVector>,
    enabled: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(0.86f)
    ) {
        labels.forEachIndexed { pageIndex, pageLabel ->
            val pageSelected = selectedPage == pageIndex
            val tabColor =
                if (pageSelected) SunsetGold else GraphiteAccent

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = enabled) {
                        onTabSelected(pageIndex)
                    }
                    .semantics {
                        contentDescription = "$pageLabel 편집"
                    }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icons[pageIndex],
                    contentDescription = null,
                    tint = tabColor,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = pageLabel,
                    color = tabColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(3.dp))

                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth(0.5f)
                        .background(
                            color =
                                if (pageSelected) {
                                    SunsetGold
                                } else {
                                    Color.Transparent
                                },
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

private fun isPositionInsideTapedFilmPhotoBounds(
    position: Offset,
    previewSize: IntSize,
    stampPhotoScale: Float
): Boolean {
    if (previewSize.width <= 0) {
        return false
    }

    val scale =
        previewSize.width / PostcardRenderSpec.LOGICAL_SIZE
    val photoBounds =
        PostcardRenderSpec.layoutFor(
            layoutStyle = "TAPED_FILM",
            stampPhotoScale = stampPhotoScale
        ).photoBounds

    return position.x >= photoBounds.left * scale &&
            position.x <= photoBounds.right * scale &&
            position.y >= photoBounds.top * scale &&
            position.y <= photoBounds.bottom * scale
}

@Composable
private fun PostcardPreviewContent(
    imagePath: String,
    message: String,
    backgroundColorArgb: Long,
    backgroundPattern: String,
    messageFont: String,
    layoutStyle: String,
    capturedAt: Long,
    dateFormat: String,
    messageTextScale: Float = 1f,
    dateTextScale: Float = 1f,
    backgroundPatternDensity: Float = 1f,
    stampPhotoScale: Float = 1f,
    polaroidPhotoScale: Float = 1f,
    photoEdgeBlur: Float = 0f,
    stampPhotoOffsetX: Float = 0f,
    stampPhotoOffsetY: Float = 0f,
    polaroidPhotoOffsetX: Float = 0f,
    polaroidPhotoOffsetY: Float = 0f,
    tapedFilmPhotoOffsetX: Float = 0f,
    tapedFilmPhotoOffsetY: Float = 0f,
    stampPhotoZoom: Float = 1f,
    polaroidPhotoZoom: Float = 1f,
    tapedFilmPhotoZoom: Float = 1f
) {
    val sourceBitmap =
        remember(imagePath) {
            runCatching {
                PostcardRenderSpec.decodeSourceBitmap(
                    File(imagePath)
                )
            }.getOrNull()
        }

    DisposableEffect(sourceBitmap) {
        onDispose {
            if (
                sourceBitmap != null &&
                !sourceBitmap.isRecycled
            ) {
                sourceBitmap.recycle()
            }
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val bitmap =
            sourceBitmap ?: return@Canvas

        drawIntoCanvas { composeCanvas ->
            PostcardRenderSpec.drawBaseContent(
                canvas = composeCanvas.nativeCanvas,
                sourceBitmap = bitmap,
                backgroundColorArgb = backgroundColorArgb,
                backgroundPattern = backgroundPattern,
                message = message,
                messageFont = messageFont,
                layoutStyle = layoutStyle,
                capturedAt = capturedAt,
                dateFormat = dateFormat,
                targetSize = size.width,
                messageTextScale = messageTextScale,
                dateTextScale = dateTextScale,
                backgroundPatternDensity = backgroundPatternDensity,
                stampPhotoScale = stampPhotoScale,
                polaroidPhotoScale = polaroidPhotoScale,
                photoEdgeBlur = photoEdgeBlur,
                stampPhotoOffsetX = stampPhotoOffsetX,
                stampPhotoOffsetY = stampPhotoOffsetY,
                polaroidPhotoOffsetX = polaroidPhotoOffsetX,
                polaroidPhotoOffsetY = polaroidPhotoOffsetY,
                tapedFilmPhotoOffsetX = tapedFilmPhotoOffsetX,
                tapedFilmPhotoOffsetY = tapedFilmPhotoOffsetY,
                stampPhotoZoom = stampPhotoZoom,
                polaroidPhotoZoom = polaroidPhotoZoom,
                tapedFilmPhotoZoom = tapedFilmPhotoZoom
            )
        }
    }
}

@Composable
fun DetailScreen(
    postcardId: Long,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val postcard by viewModel.postcard.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val backgroundUpdateState by viewModel.backgroundUpdateState.collectAsState()
    val imageUpdateState by viewModel.imageUpdateState.collectAsState()
    val fontUpdateState by viewModel.fontUpdateState.collectAsState()
    val layoutUpdateState by viewModel.layoutUpdateState.collectAsState()
    val dateFormatUpdateState by viewModel.dateFormatUpdateState.collectAsState()
    val photoColorExtractionState by viewModel.photoColorExtractionState.collectAsState()
    val stickerBackgroundRemovalState by
        viewModel.stickerBackgroundRemovalState.collectAsState()

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var moreMenuExpanded by remember {
        mutableStateOf(false)
    }

    var showMessageDialog by remember {
        mutableStateOf(false)
    }

    var messageDraft by remember {
        mutableStateOf("")
    }

    val photoStickers by viewModel.photoStickers.collectAsState()
    val selectedStickerId by viewModel.selectedStickerId.collectAsState()
    val latestPhotoStickers by rememberUpdatedState(photoStickers)
    val canUndoSticker by viewModel.canUndoSticker.collectAsState()
    val canRedoSticker by viewModel.canRedoSticker.collectAsState()

    val photoSeals by viewModel.photoSeals.collectAsState()
    val selectedSealId by viewModel.selectedSealId.collectAsState()
    val latestPhotoSeals by rememberUpdatedState(photoSeals)
    val canUndoSeal by viewModel.canUndoSeal.collectAsState()
    val canRedoSeal by viewModel.canRedoSeal.collectAsState()
    var sealScaleDragSnapshotTaken by remember {
        mutableStateOf(false)
    }
    val canUndoPhotoTransform by viewModel.canUndoPhotoTransform.collectAsState()
    val canRedoPhotoTransform by viewModel.canRedoPhotoTransform.collectAsState()
    var stampPhotoScaleDragSnapshotTaken by remember {
        mutableStateOf(false)
    }
    var polaroidPhotoScaleDragSnapshotTaken by remember {
        mutableStateOf(false)
    }
    val latestPostcard by rememberUpdatedState(postcard)

    var stickerEditMode by remember {
        mutableStateOf(StickerEditMode.Move)
    }
    var stickerEditModeOwnerId by remember {
        mutableStateOf<String?>(null)
    }
    val resolvedStickerEditMode =
        if (
            selectedStickerId != null &&
            selectedStickerId == stickerEditModeOwnerId
        ) {
            stickerEditMode
        } else {
            StickerEditMode.Move
        }

    var stickerSizes by remember {
        mutableStateOf(mapOf<String, IntSize>())
    }

    var sealSizes by remember {
        mutableStateOf(mapOf<String, IntSize>())
    }

    var backgroundRemovalError by remember {
        mutableStateOf<String?>(null)
    }

    var postcardPreviewSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val baseStickerPx = with(LocalDensity.current) {
        120.dp.toPx()
    }
    val stickerScaleHandleTouchSize = 44.dp
    val stickerScaleHandleVisibleSize = 24.dp
    val stickerScaleHandleTouchPx = with(LocalDensity.current) {
        stickerScaleHandleTouchSize.toPx()
    }

    val context = LocalContext.current

    val postcardPhotoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                viewModel.updatePostcardImage(uri)
            }
        }

    val postcardFilePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                viewModel.updatePostcardImage(uri)
            }
        }

    var showPhotoSourceMenu by rememberSaveable {
        mutableStateOf(false)
    }

    /*
     * TakePicture 결과 콜백은 화면 재구성이나
     * 프로세스 재생성 뒤에도 올 수 있어서,
     * 임시 촬영 파일 경로를 rememberSaveable로 들고 있는다.
     */
    var pendingCameraCapturePath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    /*
     * 촬영 성공 후 viewModel.updatePostcardImage()가
     * 내부 저장을 끝낼 때까지는 임시 파일을 지우면 안 되므로,
     * imageUpdateState가 Success/Error로 정리된 뒤에만 삭제한다.
     */
    var pendingCameraCaptureCleanupPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val postcardCameraCapture =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            val capturePath =
                pendingCameraCapturePath
            pendingCameraCapturePath = null

            if (capturePath == null) {
                return@rememberLauncherForActivityResult
            }

            val captureFile = File(capturePath)

            if (success) {
                val captureUri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        captureFile
                    )

                pendingCameraCaptureCleanupPath =
                    capturePath

                viewModel.updatePostcardImage(
                    captureUri
                )
            } else {
                if (captureFile.exists()) {
                    captureFile.delete()
                }
            }
        }

    fun launchPostcardCameraCapture() {
        val captureDir =
            File(
                context.cacheDir,
                "camera_capture"
            )

        if (
            !captureDir.exists() &&
            !captureDir.mkdirs()
        ) {
            Toast.makeText(
                context,
                "임시 촬영 폴더를 만들지 못했어.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val captureFile =
            File(
                captureDir,
                "capture_${UUID.randomUUID()}.jpg"
            )

        pendingCameraCapturePath =
            captureFile.absolutePath

        val captureUri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                captureFile
            )

        runCatching {
            postcardCameraCapture.launch(captureUri)
        }.onFailure {
            pendingCameraCapturePath = null

            Toast.makeText(
                context,
                "카메라 앱을 찾지 못했어.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var customColorDrawerExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var textScaleTarget by rememberSaveable {
        mutableStateOf(TextScaleTarget.Message)
    }

    val customizationPagerState = rememberPagerState(
        pageCount = { 5 }
    )
    val customizationPagerScope = rememberCoroutineScope()
    val customizationPageLabels = remember {
        listOf("사진", "배경", "텍스트", "스티커", "도장")
    }
    val customizationPageIcons = remember {
        listOf(
            Icons.Default.Image,
            Icons.Default.Wallpaper,
            Icons.Default.TextFields,
            Icons.Default.EmojiEmotions,
            Icons.Default.Verified
        )
    }
    val selectedLayout =
        remember(postcard?.layoutStyle) {
            PostcardLayoutStyle.entries
                .firstOrNull { layout ->
                    layout.name ==
                            postcard?.layoutStyle
                }
                ?: PostcardLayoutStyle.STAMP
        }

    val selectedFont =
        remember(postcard?.messageFont) {
            PostcardTextFont.entries
                .firstOrNull { font ->
                    font.name ==
                            postcard?.messageFont
                }
                ?: PostcardTextFont.SERIF
        }

    val selectedPattern =
        remember(postcard?.backgroundPattern) {
            PostcardBackgroundPattern.entries
                .firstOrNull { pattern ->
                    pattern.name ==
                            postcard?.backgroundPattern
                }
                ?: PostcardBackgroundPattern.NONE
        }

    val selectedDateFormat =
        remember(postcard?.dateFormat) {
            PostcardDateFormat.entries
                .firstOrNull { dateFormat ->
                    dateFormat.name ==
                            postcard?.dateFormat
                }
                ?: PostcardDateFormat.DOT
        }

    val messageTextScalePercent =
        ((postcard?.messageTextScale ?: 1f) * 100f)
            .roundToInt()

    val dateTextScalePercent =
        ((postcard?.dateTextScale ?: 1f) * 100f)
            .roundToInt()

    val backgroundPatternDensityPercent =
        (
            (postcard?.backgroundPatternDensity ?: 1f) *
                    100f
            ).roundToInt()

    val stampPhotoScalePercent =
        ((postcard?.stampPhotoScale ?: 1f) * 100f)
            .roundToInt()

    val polaroidPhotoScalePercent =
        ((postcard?.polaroidPhotoScale ?: 1f) * 100f)
            .roundToInt()

    val photoEdgeBlurPercent =
        ((postcard?.photoEdgeBlur ?: 0f) * 100f)
            .roundToInt()

    LaunchedEffect(postcardId) {
        viewModel.loadPostcard(postcardId)
        viewModel.loadPhotoStickersState(postcardId)
        viewModel.loadPhotoSealsState(postcardId)
    }

    LaunchedEffect(deleted) {
        if (deleted) {
            onNavigateBack()
        }
    }

    LaunchedEffect(backgroundUpdateState) {
        if (backgroundUpdateState is BackgroundUpdateState.Success) {
            viewModel.resetBackgroundUpdateState()
        }
    }

    LaunchedEffect(imageUpdateState) {
        if (imageUpdateState is ImageUpdateState.Success) {
            viewModel.resetImageUpdateState()
        }

        /*
         * updatePostcardImage()가 Success/Error로 끝났다는 것은
         * 임시 촬영 파일의 바이트를 이미 다 읽었다는 뜻이라
         * 이 시점에 지워도 안전하다.
         */
        if (
            imageUpdateState is ImageUpdateState.Success ||
            imageUpdateState is ImageUpdateState.Error
        ) {
            pendingCameraCaptureCleanupPath?.let { cleanupPath ->
                pendingCameraCaptureCleanupPath = null

                File(cleanupPath).let { cleanupFile ->
                    if (cleanupFile.exists()) {
                        cleanupFile.delete()
                    }
                }
            }
        }
    }

    LaunchedEffect(fontUpdateState) {
        if (fontUpdateState is FontUpdateState.Success) {
            viewModel.resetFontUpdateState()
        }
    }

    LaunchedEffect(layoutUpdateState) {
        if (layoutUpdateState is LayoutUpdateState.Success) {
            viewModel.resetLayoutUpdateState()
        }
    }

    LaunchedEffect(dateFormatUpdateState) {
        if (dateFormatUpdateState is DateFormatUpdateState.Success) {
            viewModel.resetDateFormatUpdateState()
        }
    }

    LaunchedEffect(
        photoStickers,
        postcardPreviewSize,
        stickerSizes
    ) {
        if (postcardPreviewSize == IntSize.Zero) {
            return@LaunchedEffect
        }

        val updated = photoStickers.map { sticker ->
            val stickerSize =
                stickerSizes[sticker.id]

            if (
                stickerSize == null ||
                stickerSize == IntSize.Zero
            ) {
                return@map sticker
            }

            val newOffset =
                if (sticker.offset == null) {
                    centeredStickerOffset(
                        postcardSize = postcardPreviewSize,
                        stickerSize = stickerSize
                    )
                } else {
                    clampStickerOffset(
                        offset = sticker.offset,
                        postcardSize = postcardPreviewSize,
                        stickerSize = stickerSize
                    )
                }

            sticker.copy(offset = newOffset)
        }

        if (updated != photoStickers) {
            viewModel.setPhotoStickers(updated)
        }
    }

    LaunchedEffect(stickerBackgroundRemovalState) {
        when (
            val removalState =
                stickerBackgroundRemovalState
        ) {
            is StickerBackgroundRemovalState.Success -> {
                val targetId = removalState.stickerId

                val cacheUrisToDelete = mutableListOf<Uri>()

                val updatedStickers = photoStickers.map { sticker ->
                    if (sticker.id != targetId) {
                        return@map sticker
                    }

                    if (sticker.originalUri != removalState.sourceUri) {
                        cacheUrisToDelete += removalState.resultUri
                        return@map sticker
                    }

                    sticker.removedBgUri?.let { oldUri ->
                        if (oldUri != removalState.resultUri) {
                            cacheUrisToDelete += oldUri
                        }
                    }

                    sticker.copy(
                        removedBgUri = removalState.resultUri,
                        displayedUri = removalState.resultUri,
                        isBackgroundRemoved = true
                    )
                }

                viewModel.setPhotoStickers(updatedStickers)

                cacheUrisToDelete.distinct().forEach { uri ->
                    viewModel.deleteStickerCacheUri(uri)
                }

                backgroundRemovalError = null
                viewModel.resetStickerBackgroundRemovalState()
            }

            is StickerBackgroundRemovalState.Error -> {
                backgroundRemovalError =
                    removalState.message
                viewModel
                    .resetStickerBackgroundRemovalState()
            }

            else -> Unit
        }
    }

    val isRemovingBackground =
        stickerBackgroundRemovalState is StickerBackgroundRemovalState.Removing

    val controlsEnabled =
        exportState !is ExportState.Exporting &&
                backgroundUpdateState !is BackgroundUpdateState.Saving &&
                fontUpdateState !is FontUpdateState.Saving &&
                layoutUpdateState !is LayoutUpdateState.Saving &&
                dateFormatUpdateState !is DateFormatUpdateState.Saving &&
                imageUpdateState !is ImageUpdateState.Saving &&
                !isRemovingBackground
    val latestControlsEnabled by rememberUpdatedState(controlsEnabled)

    val textScaleSnackbarHostState =
        remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.textScaleSaveErrors.collect { message ->
            textScaleSnackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackgroundGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier.height(72.dp)
            )

            postcard?.let { pc ->
                Box(
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RectangleShape)
                            .background(
                                color = Color(
                                    pc.backgroundColorArgb
                                ),
                                shape = RectangleShape
                            )
                            .onSizeChanged { size ->
                                postcardPreviewSize = size
                            }
                            .pointerInput(
                                selectedLayout
                            ) {
                                val panSensitivity = 0.45f
                                var tapedFilmDragArmed = true
                                var photoTransformGestureSnapshotPending = true

                                coroutineScope {
                                    launch {
                                        awaitEachGesture {
                                            val down =
                                                awaitFirstDown(
                                                    requireUnconsumed = false
                                                )
                                            tapedFilmDragArmed =
                                                selectedLayout !=
                                                        PostcardLayoutStyle.TAPED_FILM ||
                                                        isPositionInsideTapedFilmPhotoBounds(
                                                            position = down.position,
                                                            previewSize = postcardPreviewSize,
                                                            stampPhotoScale =
                                                                latestPostcard
                                                                    ?.stampPhotoScale
                                                                    ?: 1f
                                                        )
                                            photoTransformGestureSnapshotPending = true
                                        }
                                    }

                                    launch {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        if (!latestControlsEnabled) {
                                            return@detectTransformGestures
                                        }

                                        if (
                                            selectedLayout ==
                                                    PostcardLayoutStyle.TAPED_FILM &&
                                                    !tapedFilmDragArmed
                                        ) {
                                            return@detectTransformGestures
                                        }

                                        val current =
                                            latestPostcard
                                                ?: return@detectTransformGestures

                                        if (photoTransformGestureSnapshotPending) {
                                            viewModel.recordPhotoTransformSnapshotForUndo()
                                            photoTransformGestureSnapshotPending = false
                                        }

                                        val oldOffsetX =
                                            when (selectedLayout) {
                                                PostcardLayoutStyle.POLAROID ->
                                                    current.polaroidPhotoOffsetX
                                                PostcardLayoutStyle.TAPED_FILM ->
                                                    current.tapedFilmPhotoOffsetX
                                                PostcardLayoutStyle.STAMP ->
                                                    current.stampPhotoOffsetX
                                            }
                                        val oldOffsetY =
                                            when (selectedLayout) {
                                                PostcardLayoutStyle.POLAROID ->
                                                    current.polaroidPhotoOffsetY
                                                PostcardLayoutStyle.TAPED_FILM ->
                                                    current.tapedFilmPhotoOffsetY
                                                PostcardLayoutStyle.STAMP ->
                                                    current.stampPhotoOffsetY
                                            }
                                        val oldZoom =
                                            when (selectedLayout) {
                                                PostcardLayoutStyle.POLAROID ->
                                                    current.polaroidPhotoZoom
                                                PostcardLayoutStyle.TAPED_FILM ->
                                                    current.tapedFilmPhotoZoom
                                                PostcardLayoutStyle.STAMP ->
                                                    current.stampPhotoZoom
                                            }

                                        val newZoom =
                                            (oldZoom * zoom).coerceIn(1f, 3f)

                                        val newOffsetX =
                                            if (postcardPreviewSize.width > 0) {
                                                (
                                                        oldOffsetX -
                                                                (pan.x / postcardPreviewSize.width) *
                                                                panSensitivity * 2f
                                                        ).coerceIn(-1f, 1f)
                                            } else {
                                                oldOffsetX
                                            }
                                        val newOffsetY =
                                            if (postcardPreviewSize.height > 0) {
                                                (
                                                        oldOffsetY -
                                                                (pan.y / postcardPreviewSize.height) *
                                                                panSensitivity * 2f
                                                        ).coerceIn(-1f, 1f)
                                            } else {
                                                oldOffsetY
                                            }

                                        when (selectedLayout) {
                                            PostcardLayoutStyle.POLAROID -> {
                                                viewModel.setPolaroidPhotoOffsetPreview(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.savePolaroidPhotoOffset(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.setPolaroidPhotoZoomPreview(newZoom)
                                                viewModel.savePolaroidPhotoZoom(newZoom)
                                            }

                                            PostcardLayoutStyle.TAPED_FILM -> {
                                                viewModel.setTapedFilmPhotoOffsetPreview(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.saveTapedFilmPhotoOffset(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.setTapedFilmPhotoZoomPreview(newZoom)
                                                viewModel.saveTapedFilmPhotoZoom(newZoom)
                                            }

                                            PostcardLayoutStyle.STAMP -> {
                                                viewModel.setStampPhotoOffsetPreview(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.saveStampPhotoOffset(
                                                    newOffsetX,
                                                    newOffsetY
                                                )
                                                viewModel.setStampPhotoZoomPreview(newZoom)
                                                viewModel.saveStampPhotoZoom(newZoom)
                                            }
                                        }
                                    }
                                    }
                                }
                            }
                    ) {
                        PostcardPreviewContent(
                            imagePath = pc.imagePath,
                            message = pc.message,
                            backgroundColorArgb =
                                pc.backgroundColorArgb,
                            backgroundPattern =
                                selectedPattern.name,
                            messageFont = selectedFont.name,
                            layoutStyle = selectedLayout.name,
                            capturedAt = pc.capturedAt,
                            dateFormat =
                                selectedDateFormat.name,
                            messageTextScale =
                                pc.messageTextScale,
                            dateTextScale =
                                pc.dateTextScale,
                            backgroundPatternDensity =
                                pc.backgroundPatternDensity,
                            stampPhotoScale =
                                pc.stampPhotoScale,
                            polaroidPhotoScale =
                                pc.polaroidPhotoScale,
                            photoEdgeBlur =
                                pc.photoEdgeBlur,
                            stampPhotoOffsetX =
                                pc.stampPhotoOffsetX,
                            stampPhotoOffsetY =
                                pc.stampPhotoOffsetY,
                            polaroidPhotoOffsetX =
                                pc.polaroidPhotoOffsetX,
                            polaroidPhotoOffsetY =
                                pc.polaroidPhotoOffsetY,
                            tapedFilmPhotoOffsetX =
                                pc.tapedFilmPhotoOffsetX,
                            tapedFilmPhotoOffsetY =
                                pc.tapedFilmPhotoOffsetY,
                            stampPhotoZoom =
                                pc.stampPhotoZoom,
                            polaroidPhotoZoom =
                                pc.polaroidPhotoZoom,
                            tapedFilmPhotoZoom =
                                pc.tapedFilmPhotoZoom
                        )

                        photoStickers.forEach { sticker ->
                            val isSelected =
                                sticker.id == selectedStickerId
                            val perStickerEditMode =
                                if (isSelected) {
                                    resolvedStickerEditMode
                                } else {
                                    StickerEditMode.Move
                                }
                            val currentOffset =
                                sticker.offset

                            val stickerPositionModifier =
                                if (currentOffset == null) {
                                    Modifier.align(
                                        Alignment.Center
                                    )
                                } else {
                                    Modifier
                                        .align(
                                            Alignment.TopStart
                                        )
                                        .offset {
                                            IntOffset(
                                                x = currentOffset.x
                                                    .roundToInt(),
                                                y = currentOffset.y
                                                    .roundToInt()
                                            )
                                        }
                                }

                            val imageModifier =
                                when {
                                    sticker.isBackgroundRemoved && isSelected ->
                                        Modifier
                                            .fillMaxSize()
                                            .border(
                                                width = 3.dp,
                                                color = GraphiteAccent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    sticker.isBackgroundRemoved ->
                                        Modifier.fillMaxSize()
                                    isSelected ->
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(
                                                width = 3.dp,
                                                color = GraphiteAccent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    else ->
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                }

                            Box(
                                modifier = stickerPositionModifier
                                    .size(120.dp * sticker.scale)
                                    .onSizeChanged { size ->
                                        stickerSizes =
                                            stickerSizes +
                                                    (sticker.id to size)
                                    }
                            ) {
                                AsyncImage(
                                    model = sticker.displayedUri,
                                    contentDescription =
                                        "포스트카드 스티커 사진",
                                    contentScale =
                                        if (sticker.isBackgroundRemoved) {
                                            ContentScale.Fit
                                        } else {
                                            ContentScale.Crop
                                        },
                                    modifier = imageModifier
                                        .graphicsLayer {
                                            rotationZ =
                                                sticker.rotationDegrees
                                            scaleX =
                                                if (sticker.flipHorizontal) -1f else 1f
                                            scaleY =
                                                if (sticker.flipVertical) -1f else 1f
                                        }
                                        .pointerInput(
                                            sticker.id,
                                            postcardPreviewSize,
                                            perStickerEditMode
                                        ) {
                                            var stickerGestureSnapshotPending = true

                                            coroutineScope {
                                            launch {
                                                awaitEachGesture {
                                                    awaitFirstDown(
                                                        requireUnconsumed = false
                                                    )
                                                    stickerGestureSnapshotPending = true
                                                }
                                            }
                                            launch {
                                                detectTapGestures(
                                                    onTap = {
                                                        if (selectedStickerId == sticker.id) {
                                                            stickerEditModeOwnerId = null
                                                            viewModel.setSelectedStickerId(null)
                                                        } else {
                                                            viewModel.setSelectedStickerId(
                                                                sticker.id
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                            launch {
                                            when (perStickerEditMode) {
                                                StickerEditMode.Move -> {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            viewModel.setSelectedStickerId(
                                                                sticker.id
                                                            )
                                                            viewModel.recordStickerSnapshotForUndo()
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()

                                                            if (postcardPreviewSize == IntSize.Zero) {
                                                                return@detectDragGestures
                                                            }

                                                            val currentSticker =
                                                                latestPhotoStickers.find {
                                                                    it.id == sticker.id
                                                                } ?: return@detectDragGestures

                                                            val currentStickerSize =
                                                                stickerSizes[sticker.id]
                                                                    ?: IntSize.Zero

                                                            val oldOffset =
                                                                currentSticker.offset
                                                                    ?: centeredStickerOffset(
                                                                        postcardSize = postcardPreviewSize,
                                                                        stickerSize = currentStickerSize
                                                                    )

                                                            val parentSpaceDrag =
                                                                localStickerDeltaToParent(
                                                                    localDelta = dragAmount,
                                                                    rotationDegrees =
                                                                        currentSticker.rotationDegrees,
                                                                    flipHorizontal =
                                                                        currentSticker.flipHorizontal,
                                                                    flipVertical =
                                                                        currentSticker.flipVertical
                                                                )

                                                            viewModel.setPhotoStickers(
                                                                latestPhotoStickers.map {
                                                                    if (it.id == sticker.id) {
                                                                        it.copy(
                                                                            offset = clampStickerOffset(
                                                                                offset = oldOffset + parentSpaceDrag,
                                                                                postcardSize = postcardPreviewSize,
                                                                                stickerSize = currentStickerSize
                                                                            )
                                                                        )
                                                                    } else {
                                                                        it
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    )
                                                }

                                                StickerEditMode.Scale -> {
                                                    detectTransformGestures {
                                                        centroid, _, zoom, _ ->

                                                        viewModel.setSelectedStickerId(sticker.id)

                                                        val currentSticker =
                                                            latestPhotoStickers.find {
                                                                it.id == sticker.id
                                                            } ?: return@detectTransformGestures

                                                        if (stickerGestureSnapshotPending) {
                                                            viewModel.recordStickerSnapshotForUndo()
                                                            stickerGestureSnapshotPending = false
                                                        }

                                                        val oldScale = currentSticker.scale
                                                        val newScale =
                                                            (oldScale * zoom).coerceIn(0.5f, 2.5f)
                                                        val actualZoom =
                                                            newScale / oldScale

                                                        if (postcardPreviewSize == IntSize.Zero) {
                                                            viewModel.setPhotoStickers(
                                                                latestPhotoStickers.map {
                                                                    if (it.id == sticker.id) {
                                                                        it.copy(scale = newScale)
                                                                    } else {
                                                                        it
                                                                    }
                                                                }
                                                            )
                                                            return@detectTransformGestures
                                                        }

                                                        val currentStickerSize =
                                                            stickerSizes[sticker.id]
                                                                ?: IntSize.Zero

                                                        val oldOffset =
                                                            currentSticker.offset
                                                                ?: centeredStickerOffset(
                                                                    postcardSize = postcardPreviewSize,
                                                                    stickerSize = currentStickerSize
                                                                )

                                                        val correctedOffset =
                                                            oldOffset +
                                                                    localStickerDeltaToParent(
                                                                        localDelta =
                                                                            centroid * (1f - actualZoom),
                                                                        rotationDegrees =
                                                                            currentSticker.rotationDegrees,
                                                                        flipHorizontal =
                                                                            currentSticker.flipHorizontal,
                                                                        flipVertical =
                                                                            currentSticker.flipVertical
                                                                    )

                                                        val newSizePx =
                                                            (baseStickerPx * newScale).roundToInt()
                                                        val newEffectiveSize =
                                                            IntSize(newSizePx, newSizePx)

                                                        viewModel.setPhotoStickers(
                                                            latestPhotoStickers.map {
                                                                if (it.id == sticker.id) {
                                                                    it.copy(
                                                                        scale = newScale,
                                                                        offset = clampStickerOffset(
                                                                            offset = correctedOffset,
                                                                            postcardSize = postcardPreviewSize,
                                                                            stickerSize = newEffectiveSize
                                                                        )
                                                                    )
                                                                } else {
                                                                    it
                                                                }
                                                            }
                                                        )
                                                    }
                                                }

                                                StickerEditMode.Rotate -> {
                                                    detectTransformGestures {
                                                        _, _, _, rotationChange ->

                                                        viewModel.setSelectedStickerId(sticker.id)

                                                        val currentSticker =
                                                            latestPhotoStickers.find {
                                                                it.id == sticker.id
                                                            } ?: return@detectTransformGestures

                                                        if (stickerGestureSnapshotPending) {
                                                            viewModel.recordStickerSnapshotForUndo()
                                                            stickerGestureSnapshotPending = false
                                                        }

                                                        val newRotation =
                                                            normalizeStickerRotation(
                                                                currentSticker.rotationDegrees +
                                                                        rotationChange
                                                            )

                                                        viewModel.setPhotoStickers(
                                                            latestPhotoStickers.map {
                                                                if (it.id == sticker.id) {
                                                                    it.copy(
                                                                        rotationDegrees = newRotation
                                                                    )
                                                                } else {
                                                                    it
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        }  // launch (mode gesture)
                                        }  // coroutineScope
                                )

                                if (isSelected && perStickerEditMode == StickerEditMode.Rotate) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = (-22).dp)
                                            .size(stickerScaleHandleTouchSize)
                                            .pointerInput(
                                                sticker.id,
                                                postcardPreviewSize,
                                                stickerScaleHandleTouchPx
                                            ) {
                                                var rotationGestureActive = false
                                                var gestureStartRotation = 0f
                                                var gestureStartCenter = Offset.Zero
                                                var gestureStartTouch = Offset.Zero
                                                var gestureStartAngle = 0f
                                                var accumulatedDrag = Offset.Zero

                                                detectDragGestures(
                                                    onDragStart = { startTouch ->
                                                        rotationGestureActive =
                                                            latestControlsEnabled
                                                        val currentSticker =
                                                            if (rotationGestureActive) {
                                                                latestPhotoStickers.find {
                                                                    it.id == sticker.id
                                                                }
                                                            } else {
                                                                null
                                                            }

                                                        if (
                                                            currentSticker == null ||
                                                            postcardPreviewSize == IntSize.Zero
                                                        ) {
                                                            rotationGestureActive = false
                                                        } else {
                                                            viewModel.setSelectedStickerId(
                                                                sticker.id
                                                            )
                                                            viewModel.recordStickerSnapshotForUndo()

                                                            gestureStartRotation =
                                                                currentSticker.rotationDegrees
                                                            val startSidePx =
                                                                baseStickerPx *
                                                                        currentSticker.scale
                                                            val startSize =
                                                                IntSize(
                                                                    startSidePx
                                                                        .roundToInt(),
                                                                    startSidePx
                                                                        .roundToInt()
                                                                )
                                                            val startOffset =
                                                                currentSticker.offset
                                                                    ?: centeredStickerOffset(
                                                                        postcardSize =
                                                                            postcardPreviewSize,
                                                                        stickerSize =
                                                                            startSize
                                                                    )
                                                            gestureStartCenter =
                                                                startOffset +
                                                                        Offset(
                                                                            x = startSidePx / 2f,
                                                                            y = startSidePx / 2f
                                                                        )

                                                            val handleTopLeft =
                                                                Offset(
                                                                    x = (
                                                                        startSidePx -
                                                                                stickerScaleHandleTouchPx
                                                                        ) / 2f,
                                                                    y = -stickerScaleHandleTouchPx / 2f
                                                                )
                                                            gestureStartTouch =
                                                                startOffset +
                                                                        handleTopLeft +
                                                                        startTouch
                                                            gestureStartAngle =
                                                                atan2(
                                                                    gestureStartTouch.y -
                                                                            gestureStartCenter.y,
                                                                    gestureStartTouch.x -
                                                                            gestureStartCenter.x
                                                                )
                                                            accumulatedDrag =
                                                                Offset.Zero
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        if (rotationGestureActive) {
                                                            change.consume()
                                                            accumulatedDrag += dragAmount

                                                            val currentTouch =
                                                                gestureStartTouch +
                                                                        accumulatedDrag
                                                            val currentAngle =
                                                                atan2(
                                                                    currentTouch.y -
                                                                            gestureStartCenter.y,
                                                                    currentTouch.x -
                                                                            gestureStartCenter.x
                                                                )
                                                            val angleDeltaDegrees =
                                                                (
                                                                    currentAngle -
                                                                            gestureStartAngle
                                                                    ) * 180f /
                                                                        kotlin.math.PI.toFloat()
                                                            val newRotation =
                                                                normalizeStickerRotation(
                                                                    gestureStartRotation +
                                                                            angleDeltaDegrees
                                                                )

                                                            viewModel.setPhotoStickers(
                                                                latestPhotoStickers.map {
                                                                    if (it.id == sticker.id) {
                                                                        it.copy(
                                                                            rotationDegrees =
                                                                                newRotation
                                                                        )
                                                                    } else {
                                                                        it
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        rotationGestureActive = false
                                                    },
                                                    onDragCancel = {
                                                        rotationGestureActive = false
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(
                                                    stickerScaleHandleVisibleSize
                                                )
                                                .background(
                                                    color = BrutalWhite,
                                                    shape = CircleShape
                                                )
                                                .border(
                                                    width = 2.dp,
                                                    color = BrutalBlack,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 8.dp, y = (-8).dp)
                                            .size(26.dp)
                                            .background(
                                                color = GalleryDangerRed,
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = BrutalBlack,
                                                shape = CircleShape
                                            )
                                            .clickable(
                                                enabled = controlsEnabled
                                            ) {
                                                val toDelete =
                                                    photoStickers.find {
                                                        it.id == sticker.id
                                                    }
                                                val removedBgUriToDelete =
                                                    toDelete?.removedBgUri

                                                viewModel.recordStickerSnapshotForUndo()

                                                val remaining =
                                                    photoStickers.filter {
                                                        it.id != sticker.id
                                                    }
                                                viewModel.setPhotoStickers(remaining)

                                                removedBgUriToDelete?.let { uri ->
                                                    viewModel
                                                        .deleteStickerCacheUri(uri)
                                                }
                                                stickerSizes =
                                                    stickerSizes - sticker.id
                                                if (selectedStickerId == sticker.id) {
                                                    viewModel.setSelectedStickerId(
                                                        remaining.lastOrNull()?.id
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "스티커 삭제",
                                            tint = BrutalWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                if (isSelected && perStickerEditMode == StickerEditMode.Scale) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(stickerScaleHandleTouchSize)
                                            .pointerInput(
                                                sticker.id,
                                                postcardPreviewSize,
                                                stickerScaleHandleTouchPx
                                            ) {
                                                var scaleGestureActive = false
                                                var gestureStartScale = 1f
                                                var gestureStartCenter = Offset.Zero
                                                var gestureStartTouch = Offset.Zero
                                                var gestureStartDistance = 1f
                                                var accumulatedDrag = Offset.Zero

                                                detectDragGestures(
                                                    onDragStart = { startTouch ->
                                                        scaleGestureActive =
                                                            latestControlsEnabled
                                                        val currentSticker =
                                                            if (scaleGestureActive) {
                                                                latestPhotoStickers.find {
                                                                    it.id == sticker.id
                                                                }
                                                            } else {
                                                                null
                                                            }

                                                        if (currentSticker == null) {
                                                            scaleGestureActive = false
                                                        } else {
                                                            viewModel.setSelectedStickerId(
                                                                sticker.id
                                                            )
                                                            viewModel.recordStickerSnapshotForUndo()

                                                            gestureStartScale =
                                                                currentSticker.scale
                                                            val startSidePx =
                                                                baseStickerPx *
                                                                        gestureStartScale
                                                            val startSize =
                                                                IntSize(
                                                                    startSidePx
                                                                        .roundToInt(),
                                                                    startSidePx
                                                                        .roundToInt()
                                                                )
                                                            val startOffset =
                                                                currentSticker.offset
                                                                    ?: centeredStickerOffset(
                                                                        postcardSize =
                                                                            postcardPreviewSize,
                                                                        stickerSize =
                                                                            startSize
                                                                    )
                                                            gestureStartCenter =
                                                                startOffset +
                                                                        Offset(
                                                                            x = startSidePx / 2f,
                                                                            y = startSidePx / 2f
                                                                        )

                                                            val handleTopLeft =
                                                                Offset(
                                                                    x = (
                                                                        startSidePx -
                                                                                stickerScaleHandleTouchPx
                                                                        ).coerceAtLeast(0f),
                                                                    y = (
                                                                        startSidePx -
                                                                                stickerScaleHandleTouchPx
                                                                        ).coerceAtLeast(0f)
                                                                )
                                                            gestureStartTouch =
                                                                handleTopLeft + startTouch
                                                            gestureStartDistance =
                                                                (
                                                                    gestureStartTouch -
                                                                            gestureStartCenter
                                                                    ).getDistance()
                                                                    .coerceAtLeast(1f)
                                                            accumulatedDrag =
                                                                Offset.Zero
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        if (scaleGestureActive) {
                                                            change.consume()
                                                            accumulatedDrag += dragAmount

                                                            val currentTouch =
                                                                gestureStartTouch +
                                                                        accumulatedDrag
                                                            val currentDistance =
                                                                (
                                                                    currentTouch -
                                                                            gestureStartCenter
                                                                    ).getDistance()
                                                                    .coerceAtLeast(1f)
                                                            val newScale =
                                                                (
                                                                    gestureStartScale *
                                                                            currentDistance /
                                                                            gestureStartDistance
                                                                    ).coerceIn(0.5f, 2.5f)
                                                            val newSidePx =
                                                                baseStickerPx * newScale
                                                            val newSize =
                                                                IntSize(
                                                                    newSidePx
                                                                        .roundToInt(),
                                                                    newSidePx
                                                                        .roundToInt()
                                                                )
                                                            val newOffset =
                                                                gestureStartCenter -
                                                                        Offset(
                                                                            x = newSidePx / 2f,
                                                                            y = newSidePx / 2f
                                                                        )

                                                            viewModel.setPhotoStickers(
                                                                latestPhotoStickers.map {
                                                                    if (it.id == sticker.id) {
                                                                        it.copy(
                                                                            scale = newScale,
                                                                            offset =
                                                                                clampStickerOffset(
                                                                                    offset =
                                                                                        newOffset,
                                                                                    postcardSize =
                                                                                        postcardPreviewSize,
                                                                                    stickerSize =
                                                                                        newSize
                                                                                )
                                                                        )
                                                                    } else {
                                                                        it
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        scaleGestureActive = false
                                                    },
                                                    onDragCancel = {
                                                        scaleGestureActive = false
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(
                                                    stickerScaleHandleVisibleSize
                                                )
                                                .background(
                                                    color = BrutalWhite,
                                                    shape = CircleShape
                                                )
                                                .border(
                                                    width = 2.dp,
                                                    color = GraphiteAccent,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        photoSeals.forEach { seal ->
                            val isSealSelected =
                                seal.id == selectedSealId
                            val currentSealOffset =
                                seal.offset

                            val sealPositionModifier =
                                if (currentSealOffset == null) {
                                    Modifier.align(
                                        Alignment.Center
                                    )
                                } else {
                                    Modifier
                                        .align(
                                            Alignment.TopStart
                                        )
                                        .offset {
                                            IntOffset(
                                                x = currentSealOffset.x
                                                    .roundToInt(),
                                                y = currentSealOffset.y
                                                    .roundToInt()
                                            )
                                        }
                                }

                            Box(
                                modifier = sealPositionModifier
                                    .size(90.dp * seal.scale)
                                    .onSizeChanged { size ->
                                        sealSizes =
                                            sealSizes +
                                                    (seal.id to size)
                                    }
                                    .graphicsLayer {
                                        rotationZ =
                                            seal.rotationDegrees
                                    }
                                    .then(
                                        if (isSealSelected) {
                                            val selectionShape =
                                                when (seal.type) {
                                                    SealType.CIRCLE_POSTMARK,
                                                    SealType.STAR -> CircleShape
                                                    SealType.WAVE_CANCEL,
                                                    SealType.AIR_MAIL ->
                                                        RoundedCornerShape(8.dp)
                                                }

                                            Modifier.border(
                                                width = 2.dp,
                                                color = GraphiteAccent,
                                                shape = selectionShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .pointerInput(
                                        seal.id,
                                        postcardPreviewSize
                                    ) {
                                        var sealGestureSnapshotPending = true

                                        coroutineScope {
                                            launch {
                                                awaitEachGesture {
                                                    awaitFirstDown(
                                                        requireUnconsumed = false
                                                    )
                                                    sealGestureSnapshotPending = true
                                                }
                                            }
                                            launch {
                                                detectTapGestures(
                                                    onTap = {
                                                        viewModel.setSelectedSealId(
                                                            if (selectedSealId == seal.id) {
                                                                null
                                                            } else {
                                                                seal.id
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                            launch {
                                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                                    if (postcardPreviewSize == IntSize.Zero) {
                                                        return@detectTransformGestures
                                                    }

                                                    val currentSeal =
                                                        latestPhotoSeals.find {
                                                            it.id == seal.id
                                                        } ?: return@detectTransformGestures

                                                    if (sealGestureSnapshotPending) {
                                                        viewModel.recordSealSnapshotForUndo()
                                                        sealGestureSnapshotPending = false
                                                    }

                                                    val currentSealSize =
                                                        sealSizes[seal.id]
                                                            ?: IntSize.Zero

                                                    val oldOffset =
                                                        currentSeal.offset
                                                            ?: centeredStickerOffset(
                                                                postcardSize = postcardPreviewSize,
                                                                stickerSize = currentSealSize
                                                            )

                                                    val parentDelta =
                                                        localStickerDeltaToParent(
                                                            localDelta = pan,
                                                            rotationDegrees = currentSeal.rotationDegrees,
                                                            flipHorizontal = false,
                                                            flipVertical = false
                                                        )

                                                    val newOffset =
                                                        clampStickerOffset(
                                                            offset = oldOffset + parentDelta,
                                                            postcardSize = postcardPreviewSize,
                                                            stickerSize = currentSealSize
                                                        )

                                                    val newScale =
                                                        (currentSeal.scale * zoom)
                                                            .coerceIn(0.5f, 3f)

                                                    val newRotation =
                                                        normalizeStickerRotation(
                                                            currentSeal.rotationDegrees +
                                                                    rotationChange
                                                        )

                                                    viewModel.setSelectedSealId(seal.id)
                                                    viewModel.setPhotoSeals(
                                                        latestPhotoSeals.map {
                                                            if (it.id == seal.id) {
                                                                it.copy(
                                                                    offset = newOffset,
                                                                    scale = newScale,
                                                                    rotationDegrees = newRotation
                                                                )
                                                            } else {
                                                                it
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                SealPreviewContent(
                                    type = seal.type,
                                    color = Color(seal.colorArgb),
                                    capturedAtMillis = pc.capturedAt,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isSealSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 8.dp, y = (-8).dp)
                                            .size(26.dp)
                                            .background(
                                                color = GalleryDangerRed,
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = BrutalBlack,
                                                shape = CircleShape
                                            )
                                            .clickable(
                                                enabled = controlsEnabled
                                            ) {
                                                viewModel.recordSealSnapshotForUndo()
                                                val remaining =
                                                    photoSeals.filter {
                                                        it.id != seal.id
                                                    }
                                                viewModel.setPhotoSeals(remaining)
                                                sealSizes =
                                                    sealSizes - seal.id
                                                if (selectedSealId == seal.id) {
                                                    viewModel.setSelectedSealId(
                                                        remaining.lastOrNull()?.id
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "도장 삭제",
                                            tint = BrutalWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            postcard?.let { pc ->

                val selectedSticker =
                    photoStickers.find {
                        it.id == selectedStickerId
                    }
                val selectedStickerIndex =
                    photoStickers.indexOfFirst {
                        it.id == selectedStickerId
                    }
                val canMoveSelectedStickerForward =
                    selectedStickerIndex != -1 &&
                            selectedStickerIndex < photoStickers.lastIndex
                val canMoveSelectedStickerBackward =
                    selectedStickerIndex > 0

                if (selectedSticker != null) {
                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    StickerEditModeToolbar(
                        sticker = selectedSticker,
                        editMode = resolvedStickerEditMode,
                        onModeSelected = { mode ->
                            stickerEditMode = mode
                            stickerEditModeOwnerId = selectedStickerId
                        },
                        isRemovingBackground = isRemovingBackground,
                        onToggleBackgroundRemoval = {
                            if (selectedSticker.isBackgroundRemoved) {
                                viewModel.recordStickerSnapshotForUndo()
                                viewModel.setPhotoStickers(
                                    photoStickers.map {
                                        if (it.id == selectedSticker.id) {
                                            it.copy(
                                                displayedUri = it.originalUri,
                                                isBackgroundRemoved = false
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                )
                                backgroundRemovalError = null
                            } else {
                                backgroundRemovalError = null
                                val removedBgUri =
                                    selectedSticker.removedBgUri
                                if (removedBgUri != null) {
                                    viewModel.recordStickerSnapshotForUndo()
                                    viewModel.setPhotoStickers(
                                        photoStickers.map {
                                            if (it.id == selectedSticker.id) {
                                                it.copy(
                                                    displayedUri = removedBgUri,
                                                    isBackgroundRemoved = true
                                                )
                                            } else {
                                                it
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.removeStickerBackground(
                                        stickerId = selectedSticker.id,
                                        sourceUri = selectedSticker.originalUri
                                    )
                                }
                            }
                        },
                        onToggleFlipHorizontal = {
                            viewModel.recordStickerSnapshotForUndo()
                            viewModel.setPhotoStickers(
                                photoStickers.map {
                                    if (it.id == selectedSticker.id) {
                                        it.copy(
                                            flipHorizontal = !it.flipHorizontal
                                        )
                                    } else {
                                        it
                                    }
                                }
                            )
                        },
                        onToggleFlipVertical = {
                            viewModel.recordStickerSnapshotForUndo()
                            viewModel.setPhotoStickers(
                                photoStickers.map {
                                    if (it.id == selectedSticker.id) {
                                        it.copy(
                                            flipVertical = !it.flipVertical
                                        )
                                    } else {
                                        it
                                    }
                                }
                            )
                        },
                        canMoveForward = canMoveSelectedStickerForward,
                        canMoveBackward = canMoveSelectedStickerBackward,
                        onMoveForward = {
                            viewModel.moveStickerForward(selectedSticker.id)
                        },
                        onMoveBackward = {
                            viewModel.moveStickerBackward(selectedSticker.id)
                        },
                        enabled = controlsEnabled,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                HorizontalPager(
                    state = customizationPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (page) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth(0.92f)
                            ) {
                    PostcardLayoutPicker(
                        selectedLayout =
                            selectedLayout,
                        onLayoutSelected = { layout ->
                            viewModel.updateLayoutStyle(
                                layout.name
                            )
                        },
                        onUndoPhotoTransform = {
                            viewModel.undoPhotoTransformChange()
                        },
                        onRedoPhotoTransform = {
                            viewModel.redoPhotoTransformChange()
                        },
                        canUndoPhotoTransform = canUndoPhotoTransform,
                        canRedoPhotoTransform = canRedoPhotoTransform,
                        enabled = controlsEnabled,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    if (
                        selectedLayout ==
                        PostcardLayoutStyle.POLAROID
                    ) {
                        EditorPercentSlider(
                            label = "사진 크기",
                            percent =
                                polaroidPhotoScalePercent,
                            minPercent = 75,
                            maxPercent = 105,
                            enabled = controlsEnabled,
                            onPreviewPercentChanged = { percent ->
                                if (!polaroidPhotoScaleDragSnapshotTaken) {
                                    viewModel.recordPhotoTransformSnapshotForUndo()
                                    polaroidPhotoScaleDragSnapshotTaken = true
                                }
                                viewModel
                                    .setPolaroidPhotoScalePreview(
                                        percent / 100f
                                    )
                            },
                            onPercentConfirmed = { percent ->
                                if (!polaroidPhotoScaleDragSnapshotTaken) {
                                    viewModel.recordPhotoTransformSnapshotForUndo()
                                }
                                polaroidPhotoScaleDragSnapshotTaken = false
                                viewModel
                                    .savePolaroidPhotoScale(
                                        percent / 100f
                                    )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val isTapedFilm =
                            selectedLayout ==
                                    PostcardLayoutStyle.TAPED_FILM

                        EditorPercentSlider(
                            label = "사진 크기",
                            percent =
                                stampPhotoScalePercent,
                            minPercent = if (isTapedFilm) 85 else 70,
                            maxPercent = if (isTapedFilm) 115 else 130,
                            enabled = controlsEnabled,
                            onPreviewPercentChanged = { percent ->
                                if (!stampPhotoScaleDragSnapshotTaken) {
                                    viewModel.recordPhotoTransformSnapshotForUndo()
                                    stampPhotoScaleDragSnapshotTaken = true
                                }
                                viewModel
                                    .setStampPhotoScalePreview(
                                        percent / 100f
                                    )
                            },
                            onPercentConfirmed = { percent ->
                                if (!stampPhotoScaleDragSnapshotTaken) {
                                    viewModel.recordPhotoTransformSnapshotForUndo()
                                }
                                stampPhotoScaleDragSnapshotTaken = false
                                viewModel
                                    .saveStampPhotoScale(
                                        percent / 100f
                                    )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    EditorPercentSlider(
                        label = "가장자리 흐림",
                        percent =
                            photoEdgeBlurPercent,
                        minPercent = 0,
                        maxPercent = 100,
                        enabled = controlsEnabled,
                        onPreviewPercentChanged = { percent ->
                            viewModel
                                .setPhotoEdgeBlurPreview(
                                    percent / 100f
                                )
                        },
                        onPercentConfirmed = { percent ->
                            viewModel
                                .savePhotoEdgeBlur(
                                    percent / 100f
                                )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    EditorSecondaryButton(
                        text = "사진 바꾸기",
                        icon = Icons.Default.Edit,
                        enabled = controlsEnabled,
                        onClick = {
                            showPhotoSourceMenu = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                            }
                            }
                        }

                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth(0.92f)
                            ) {
                    PostcardBackgroundColorPicker(
                        selectedColorArgb =
                            pc.backgroundColorArgb,
                        enabled = controlsEnabled,
                        onColorSelected = { colorArgb ->
                            viewModel.updateBackgroundColor(
                                colorArgb
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    TextButton(
                        onClick = {
                            customColorDrawerExpanded =
                                !customColorDrawerExpanded
                        },
                        enabled = controlsEnabled,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = BrutalBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.size(6.dp)
                        )

                        Text(
                            text =
                                if (customColorDrawerExpanded) {
                                    "직접 고르기 닫기"
                                } else {
                                    "직접 고르기"
                                },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    AnimatedVisibility(
                        visible = customColorDrawerExpanded,
                        enter =
                            expandVertically() + fadeIn(),
                        exit =
                            shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            PostcardCustomColorPicker(
                                selectedColorArgb =
                                    pc.backgroundColorArgb,
                                enabled = controlsEnabled,
                                onColorSelected = { colorArgb ->
                                    viewModel.updateBackgroundColor(
                                        colorArgb
                                    )
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    EditorSecondaryButton(
                        text =
                            if (
                                photoColorExtractionState is
                                        PhotoColorExtractionState.Extracting
                            ) {
                                "색 추출 중..."
                            } else {
                                "사진에서 색 가져오기"
                            },
                        icon = Icons.Default.Palette,
                        enabled =
                            controlsEnabled &&
                                    photoColorExtractionState !is
                                            PhotoColorExtractionState.Extracting,
                        onClick = {
                            viewModel
                                .extractBackgroundColorsFromPhoto()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    (
                            photoColorExtractionState as?
                                    PhotoColorExtractionState.Success
                            )?.let { successState ->
                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            successState.colors.forEach { extractedColor ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color =
                                                Color(
                                                    extractedColor
                                                        .colorArgb
                                                ),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = BrutalBlack,
                                            shape = CircleShape
                                        )
                                        .clickable(
                                            enabled = controlsEnabled
                                        ) {
                                            viewModel
                                                .updateBackgroundColor(
                                                    extractedColor
                                                        .colorArgb
                                                )
                                        }
                                )
                            }
                        }
                    }

                    (
                            photoColorExtractionState as?
                                    PhotoColorExtractionState.Error
                            )?.let { errorState ->
                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = errorState.message,
                            color = GalleryDangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    PostcardBackgroundPatternPicker(
                        selectedColorArgb =
                            pc.backgroundColorArgb,
                        selectedPattern = selectedPattern,
                        enabled = controlsEnabled,
                        onPatternSelected = { pattern ->
                            viewModel
                                .updateBackgroundPattern(
                                    pattern.name
                                )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    EditorPercentSlider(
                        label = "패턴 세기",
                        percent =
                            backgroundPatternDensityPercent,
                        minPercent = 70,
                        maxPercent = 150,
                        enabled = controlsEnabled,
                        onPreviewPercentChanged = { percent ->
                            viewModel
                                .setBackgroundPatternDensityPreview(
                                    percent / 100f
                                )
                        },
                        onPercentConfirmed = { percent ->
                            viewModel
                                .saveBackgroundPatternDensity(
                                    percent / 100f
                                )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                            }
                            }
                        }

                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth(0.92f)
                            ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text =
                                if (pc.message.isBlank()) {
                                    "글귀가 비어 있어."
                                } else {
                                    pc.message
                                },
                            color =
                                if (pc.message.isBlank()) {
                                    GraphiteAccent
                                } else {
                                    BrutalBlack
                                },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                messageDraft = pc.message
                                showMessageDialog = true
                            },
                            enabled = controlsEnabled,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = BrutalBlack
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(
                                modifier = Modifier.size(6.dp)
                            )

                            Text(
                                text = "편집",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Text(
                        text = "조절 대상",
                        color = BrutalBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            TextScaleTarget.Message to "글귀",
                            TextScaleTarget.Date to "날짜"
                        ).forEach { (target, targetLabel) ->
                            val targetSelected =
                                textScaleTarget == target

                            Column(
                                modifier = Modifier
                                    .clickable(
                                        enabled = controlsEnabled
                                    ) {
                                        textScaleTarget = target
                                    }
                                    .padding(
                                        horizontal = 6.dp,
                                        vertical = 4.dp
                                    ),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = targetLabel,
                                    color =
                                        if (targetSelected) {
                                            SunsetGold
                                        } else {
                                            GraphiteAccent
                                        },
                                    fontSize = 14.sp,
                                    fontWeight =
                                        if (targetSelected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Medium
                                        }
                                )

                                Spacer(
                                    modifier = Modifier.height(3.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .width(28.dp)
                                        .background(
                                            color =
                                                if (targetSelected) {
                                                    SunsetGold
                                                } else {
                                                    Color.Transparent
                                                },
                                            shape =
                                                RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    val messageTargetSelected =
                        textScaleTarget == TextScaleTarget.Message

                    EditorPercentSlider(
                        label = "크기",
                        percent =
                            if (messageTargetSelected) {
                                messageTextScalePercent
                            } else {
                                dateTextScalePercent
                            },
                        minPercent = 60,
                        maxPercent =
                            if (messageTargetSelected) 140 else 180,
                        enabled = controlsEnabled,
                        onPreviewPercentChanged = { percent ->
                            if (messageTargetSelected) {
                                viewModel.setMessageTextScalePreview(
                                    percent / 100f
                                )
                            } else {
                                viewModel.setDateTextScalePreview(
                                    percent / 100f
                                )
                            }
                        },
                        onPercentConfirmed = { percent ->
                            if (messageTargetSelected) {
                                viewModel.saveMessageTextScale(
                                    percent / 100f
                                )
                            } else {
                                viewModel.saveDateTextScale(
                                    percent / 100f
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                            }
                            }
                        }

                        3 -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                PhotoStickerPickerPanel(
                                    photoStickers = photoStickers,
                                    selectedStickerId = selectedStickerId,
                                    backgroundRemovalError = backgroundRemovalError,
                                    onSelectSticker = { id ->
                                        viewModel.setSelectedStickerId(id)
                                    },
                                    onAddFromGallery = { uri ->
                                        viewModel.recordStickerSnapshotForUndo()
                                        val newSticker = PhotoStickerItem(
                                            originalUri = uri,
                                            displayedUri = uri
                                        )
                                        viewModel.setPhotoStickers(photoStickers + newSticker)
                                        viewModel.setSelectedStickerId(newSticker.id)
                                        backgroundRemovalError = null
                                        viewModel.resetStickerBackgroundRemovalState()
                                    },
                                    onAddFromFile = { uri ->
                                        viewModel.recordStickerSnapshotForUndo()
                                        val newSticker = PhotoStickerItem(
                                            originalUri = uri,
                                            displayedUri = uri
                                        )
                                        viewModel.setPhotoStickers(photoStickers + newSticker)
                                        viewModel.setSelectedStickerId(newSticker.id)
                                        backgroundRemovalError = null
                                        viewModel.resetStickerBackgroundRemovalState()
                                    },
                                    onAddFromCamera = { captureFile ->
                                        backgroundRemovalError = null
                                        viewModel.resetStickerBackgroundRemovalState()
                                        viewModel.addCameraPhotoSticker(
                                            postcardId,
                                            captureFile
                                        )
                                    },
                                    onDeleteSticker = { id ->
                                        val sticker = photoStickers.find { it.id == id }
                                        val removedBgUriToDelete = sticker?.removedBgUri

                                        viewModel.recordStickerSnapshotForUndo()

                                        val remaining = photoStickers.filter { it.id != id }
                                        viewModel.setPhotoStickers(remaining)

                                        removedBgUriToDelete?.let { uri ->
                                            viewModel.deleteStickerCacheUri(uri)
                                        }
                                        sticker?.originalUri?.let { uri ->
                                            viewModel.deleteStickerOriginalIfUnreferenced(
                                                uri,
                                                remaining
                                            )
                                        }
                                        stickerSizes = stickerSizes - id
                                        if (selectedStickerId == id) {
                                            viewModel.setSelectedStickerId(
                                                remaining.lastOrNull()?.id
                                            )
                                        }
                                    },
                                    onDuplicateSticker = { id ->
                                        viewModel.duplicateSticker(id)
                                    },
                                    onUndoSticker = {
                                        viewModel.undoStickerChange()
                                    },
                                    onRedoSticker = {
                                        viewModel.redoStickerChange()
                                    },
                                    canUndoSticker = canUndoSticker,
                                    canRedoSticker = canRedoSticker,
                                    enabled = controlsEnabled,
                                    modifier = Modifier.fillMaxWidth(0.92f)
                                )
                            }
                        }

                        4 -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                SealPickerPanel(
                                    photoSeals = photoSeals,
                                    selectedSealId = selectedSealId,
                                    onSelectSeal = { id ->
                                        viewModel.setSelectedSealId(
                                            if (selectedSealId == id) {
                                                null
                                            } else {
                                                id
                                            }
                                        )
                                    },
                                    onAddSeal = { type ->
                                        viewModel.recordSealSnapshotForUndo()
                                        val newSeal =
                                            PostcardSealItem(type = type)
                                        viewModel.setPhotoSeals(
                                            photoSeals + newSeal
                                        )
                                        viewModel.setSelectedSealId(
                                            newSeal.id
                                        )
                                    },
                                    onDeleteSeal = { id ->
                                        viewModel.recordSealSnapshotForUndo()
                                        val remaining =
                                            photoSeals.filter {
                                                it.id != id
                                            }
                                        viewModel.setPhotoSeals(remaining)
                                        sealSizes = sealSizes - id
                                        if (selectedSealId == id) {
                                            viewModel.setSelectedSealId(
                                                remaining.lastOrNull()?.id
                                            )
                                        }
                                    },
                                    onScaleChanged = { id, newScale ->
                                        if (!sealScaleDragSnapshotTaken) {
                                            viewModel.recordSealSnapshotForUndo()
                                            sealScaleDragSnapshotTaken = true
                                        }
                                        viewModel.setPhotoSeals(
                                            photoSeals.map {
                                                if (it.id == id) {
                                                    it.copy(scale = newScale)
                                                } else {
                                                    it
                                                }
                                            }
                                        )
                                    },
                                    onScaleChangeFinished = {
                                        sealScaleDragSnapshotTaken = false
                                    },
                                    onRotateBy = { id, deltaDegrees ->
                                        viewModel.recordSealSnapshotForUndo()
                                        viewModel.setPhotoSeals(
                                            photoSeals.map {
                                                if (it.id == id) {
                                                    it.copy(
                                                        rotationDegrees =
                                                            normalizeStickerRotation(
                                                                it.rotationDegrees +
                                                                        deltaDegrees
                                                            )
                                                    )
                                                } else {
                                                    it
                                                }
                                            }
                                        )
                                    },
                                    onColorSelected = { id, colorArgb ->
                                        viewModel.recordSealSnapshotForUndo()
                                        viewModel.setPhotoSeals(
                                            photoSeals.map {
                                                if (it.id == id) {
                                                    it.copy(colorArgb = colorArgb)
                                                } else {
                                                    it
                                                }
                                            }
                                        )
                                    },
                                    onUndoSeal = {
                                        viewModel.undoSealChange()
                                    },
                                    onRedoSeal = {
                                        viewModel.redoSealChange()
                                    },
                                    canUndoSeal = canUndoSeal,
                                    canRedoSeal = canRedoSeal,
                                    enabled = controlsEnabled,
                                    modifier = Modifier.fillMaxWidth(0.92f)
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )
                if (
                    dateFormatUpdateState
                            is DateFormatUpdateState.Saving
                ) {
                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrutalBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "날짜 형식 저장 중...",
                            color = BrutalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (
                    layoutUpdateState
                            is LayoutUpdateState.Saving
                ) {
                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrutalBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "레이아웃 저장 중...",
                            color = BrutalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (
                    fontUpdateState
                            is FontUpdateState.Saving
                ) {
                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrutalBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "폰트 저장 중...",
                            color = BrutalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (
                    backgroundUpdateState
                            is BackgroundUpdateState.Saving
                ) {
                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrutalBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "배경 저장 중...",
                            color = BrutalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(96.dp)
                )
            }
            }
        }

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GalleryPaperWhite)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    enabled = controlsEnabled
                ) {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = BrutalBlack
                    )
                }

                Text(
                    text = "엽서 꾸미기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrutalBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )

                IconButton(
                    onClick = {
                        viewModel.savePhotoStickersState(
                            postcardId
                        )
                        viewModel.savePhotoSealsState(
                            postcardId
                        )
                        Toast.makeText(
                            context,
                            "현재 편집 상태를 저장했어!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    enabled = controlsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "편집 상태 저장",
                        tint = BrutalBlack
                    )
                }

                IconButton(
                    onClick = {
                        postcard?.let { pc ->
                            viewModel.exportPostcardToGallery(
                                stickerOverlays =
                                    createStickerOverlaysForExport(
                                        photoStickers =
                                            photoStickers,
                                        postcardSize =
                                            postcardPreviewSize,
                                        stickerSizes =
                                            stickerSizes
                                    ),
                                sealOverlays =
                                    createSealOverlaysForExport(
                                        photoSeals =
                                            photoSeals,
                                        postcardSize =
                                            postcardPreviewSize,
                                        sealSizes =
                                            sealSizes,
                                        capturedAtMillis =
                                            pc.capturedAt
                                    )
                            )
                        }
                    },
                    enabled = controlsEnabled
                ) {
                    if (
                        exportState
                                is ExportState.Exporting
                    ) {
                        CircularProgressIndicator(
                            color = BrutalCoral,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector =
                                Icons.Default.Download,
                            contentDescription =
                                "갤러리에 저장",
                            tint = BrutalCoral
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = {
                            moreMenuExpanded = true
                        },
                        enabled = controlsEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = BrutalBlack
                        )
                    }

                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = {
                            moreMenuExpanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "삭제",
                                    color = GalleryDangerRed
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = GalleryDangerRed
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
        }
    }

    if (showPhotoSourceMenu) {
        PhotoSourceMenu(
            onDismiss = {
                showPhotoSourceMenu = false
            },
            onCameraSelected = {
                showPhotoSourceMenu = false
                launchPostcardCameraCapture()
            },
            onGallerySelected = {
                showPhotoSourceMenu = false
                postcardPhotoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts
                            .PickVisualMedia
                            .ImageOnly
                    )
                )
            },
            onFileSelected = {
                showPhotoSourceMenu = false
                postcardFilePicker.launch(
                    arrayOf("image/*")
                )
            }
        )
    }

    if (showMessageDialog) {
        AlertDialog(
            onDismissRequest = {
                showMessageDialog = false
            },
            title = {
                Text(
                    text = "글귀 남기기",
                    color = BrutalBlack,
                    fontWeight =
                        FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        text =
                            "이 사진과 함께 기억하고 싶은 말을 적어봐.",
                        color = BrutalBlack,
                        fontSize = 14.sp,
                        modifier =
                            Modifier.padding(
                                bottom = 12.dp
                            )
                    )

                    OutlinedTextField(
                        value = messageDraft,
                        onValueChange = { newValue ->
                            if (newValue.length <= 120) {
                                messageDraft = newValue
                            }
                        },
                        label = {
                            Text("글귀")
                        },
                        placeholder = {
                            Text(
                                "오늘의 순간을 한 줄로 남겨봐"
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Text(
                        text =
                            "${messageDraft.length} / 120",
                        color = BrutalBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )

                    Text(
                        text =
                            "글귀를 모두 지운 뒤 저장하면 기존 글귀가 삭제돼.",
                        color = BrutalBlack,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = SoftGray,
                                shape =
                                    RoundedCornerShape(
                                        10.dp
                                    )
                            )
                            .padding(10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMessage(
                            messageDraft
                        )

                        showMessageDialog = false
                    }
                ) {
                    Text(
                        text = "저장",
                        color = BrutalBlack,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMessageDialog = false
                    }
                ) {
                    Text(
                        text = "취소",
                        color = BrutalBlack
                    )
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text(
                    text = "삭제하시겠어요?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text =
                        "이 우표를 삭제하면 복구할 수 없어요."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePostcard()
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        text = "삭제",
                        color = GalleryDangerRed,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }

    if (exportState is ExportState.Success) {
        AlertDialog(
            onDismissRequest = {
                viewModel.resetExportState()
            },
            title = {
                Text(
                    text = "저장 완료!",
                    color = BrutalBlack,
                    fontWeight =
                        FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text =
                        "1:1 포스트카드 이미지를 휴대폰 갤러리에 저장했어.\n\nPictures/PostcardMemory 앨범에서 확인할 수 있어."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetExportState()
                    }
                ) {
                    Text(
                        text = "확인",
                        color = BrutalBlack,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        )
    }

    (exportState as? ExportState.Error)?.let {
            exportError ->

        AlertDialog(
            onDismissRequest = {
                viewModel.resetExportState()
            },
            title = {
                Text(
                    text = "저장하지 못했어",
                    color = BrutalCoral,
                    fontWeight =
                        FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = exportError.message
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetExportState()
                    }
                ) {
                    Text(
                        text = "확인",
                        color = BrutalBlack,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        )
    }

    (
            backgroundUpdateState
                    as? BackgroundUpdateState.Error
            )?.let { backgroundError ->

            AlertDialog(
                onDismissRequest = {
                    viewModel.resetBackgroundUpdateState()
                },
                title = {
                    Text(
                        text = "배경을 저장하지 못했어",
                        color = BrutalCoral,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = backgroundError.message
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel
                                .resetBackgroundUpdateState()
                        }
                    ) {
                        Text(
                            text = "확인",
                            color = BrutalBlack,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            )
        }

    (
            imageUpdateState
                    as? ImageUpdateState.Error
            )?.let { imageError ->

            AlertDialog(
                onDismissRequest = {
                    viewModel.resetImageUpdateState()
                },
                title = {
                    Text(
                        text = "사진을 바꾸지 못했어",
                        color = BrutalCoral,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text =
                            "사진을 바꾸지 못했어. 기존 사진은 그대로 유지했어.\n" +
                                    imageError.message
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel
                                .resetImageUpdateState()
                        }
                    ) {
                        Text(
                            text = "확인",
                            color = BrutalBlack,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            )
        }

    (
            fontUpdateState
                    as? FontUpdateState.Error
            )?.let { fontError ->

            AlertDialog(
                onDismissRequest = {
                    viewModel.resetFontUpdateState()
                },
                title = {
                    Text(
                        text = "폰트를 저장하지 못했어",
                        color = BrutalCoral,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = fontError.message
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel
                                .resetFontUpdateState()
                        }
                    ) {
                        Text(
                            text = "확인",
                            color = BrutalBlack,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            )
        }

    (
            layoutUpdateState
                    as? LayoutUpdateState.Error
            )?.let { layoutError ->

            AlertDialog(
                onDismissRequest = {
                    viewModel.resetLayoutUpdateState()
                },
                title = {
                    Text(
                        text = "레이아웃을 저장하지 못했어",
                        color = BrutalCoral,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = layoutError.message
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel
                                .resetLayoutUpdateState()
                        }
                    ) {
                        Text(
                            text = "확인",
                            color = BrutalBlack,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            )
        }

    (
            dateFormatUpdateState
                    as? DateFormatUpdateState.Error
            )?.let { dateFormatError ->

            AlertDialog(
                onDismissRequest = {
                    viewModel.resetDateFormatUpdateState()
                },
                title = {
                    Text(
                        text = "날짜 형식을 저장하지 못했어",
                        color = BrutalCoral,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = dateFormatError.message
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel
                                .resetDateFormatUpdateState()
                        }
                    ) {
                        Text(
                            text = "확인",
                            color = BrutalBlack,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            )
        }

        if (postcard != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(ScreenBackgroundGray)
                    .navigationBarsPadding()
                    .padding(top = 4.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                EditorBottomTabBar(
                    selectedPage = customizationPagerState.currentPage,
                    labels = customizationPageLabels,
                    icons = customizationPageIcons,
                    enabled = controlsEnabled,
                    onTabSelected = { pageIndex ->
                        customizationPagerScope.launch {
                            customizationPagerState
                                .animateScrollToPage(pageIndex)
                        }
                    }
                )
            }
        }

        SnackbarHost(
            hostState = textScaleSnackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 64.dp)
        )
    }
}
