package com.postcardmemory.ui.detail

import android.net.Uri
import android.widget.Toast
import java.io.File
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.postcardmemory.ui.components.PostcardBackgroundPattern
import com.postcardmemory.ui.components.PostcardBackgroundPicker
import com.postcardmemory.ui.components.PostcardDateFormat
import com.postcardmemory.ui.components.PostcardLayoutPicker
import com.postcardmemory.ui.components.PostcardLayoutStyle
import com.postcardmemory.ui.components.PostcardTextFont
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalDeepViolet
import com.postcardmemory.ui.theme.BrutalLavender
import com.postcardmemory.ui.theme.BrutalViolet
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.LavenderSoft
import com.postcardmemory.utils.PostcardImageExporter
import com.postcardmemory.utils.PostcardRenderSpec

private enum class DetailDrawerSection {
    LAYOUT,
    PHOTO_SIZE,
    BACKGROUND,
    PATTERN_INTENSITY,
    TEXT_SIZE
}

private enum class CustomizationGroup(
    val label: String
) {
    PHOTO(label = "사진"),
    DESIGN(label = "디자인")
}

private enum class StickerEditMode {
    Move,
    Scale,
    Rotate
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

@Composable
private fun DetailDrawer(
    title: String,
    summary: String,
    expanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(
                        x = 5.dp,
                        y = 5.dp
                    )
                    .background(
                        color = BrutalBlack,
                        shape =
                            RoundedCornerShape(
                                16.dp
                            )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color =
                            if (expanded) {
                                BrutalLavender
                            } else {
                                BrutalWhite
                            },
                        shape =
                            RoundedCornerShape(
                                16.dp
                            )
                    )
                    .border(
                        width = 2.dp,
                        color = BrutalBlack,
                        shape =
                            RoundedCornerShape(
                                16.dp
                            )
                    )
                    .clickable(
                        enabled = enabled,
                        onClick = onClick
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 15.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        color = BrutalDeepViolet,
                        fontSize = 16.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )

                    Text(
                        text = "현재 선택: $summary",
                        color = BrutalDeepViolet,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                }

                Text(
                    text =
                        if (expanded) {
                            "▼"
                        } else {
                            "▶"
                        },
                    color = BrutalDeepViolet,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier =
                        Modifier.padding(start = 12.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter =
                expandVertically() +
                        fadeIn(),
            exit =
                shrinkVertically() +
                        fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun TextSizeControl(
    label: String,
    initialPercent: Int,
    minPercent: Int,
    maxPercent: Int,
    enabled: Boolean,
    onPreviewPercentChanged: (Int) -> Unit,
    onPercentConfirmed: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPercent by remember {
        mutableIntStateOf(initialPercent)
    }
    var textValue by remember {
        mutableStateOf(initialPercent.toString())
    }
    val focusManager = LocalFocusManager.current

    fun confirmTextValue() {
        val parsedPercent =
            textValue.toIntOrNull()
                ?: currentPercent
        val clampedPercent =
            parsedPercent.coerceIn(minPercent, maxPercent)

        currentPercent = clampedPercent
        textValue = clampedPercent.toString()
        onPercentConfirmed(clampedPercent)
    }

    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = BrutalDeepViolet,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    if (
                        newValue.isEmpty() ||
                        (
                            newValue.length <= 3 &&
                                    newValue.all { it.isDigit() }
                            )
                    ) {
                        textValue = newValue
                    }
                },
                enabled = enabled,
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            confirmTextValue()
                            focusManager.clearFocus()
                        }
                    ),
                modifier = Modifier
                    .width(76.dp)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            confirmTextValue()
                        }
                    }
            )

            Text(
                text = "%",
                color = BrutalDeepViolet,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.padding(start = 6.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Slider(
            value = currentPercent.toFloat(),
            onValueChange = { newValue ->
                val snappedPercent =
                    ((newValue / 5f).roundToInt() * 5)
                        .coerceIn(minPercent, maxPercent)

                currentPercent = snappedPercent
                textValue = snappedPercent.toString()
                onPreviewPercentChanged(snappedPercent)
            },
            onValueChangeFinished = {
                onPercentConfirmed(currentPercent)
            },
            valueRange =
                minPercent.toFloat()..maxPercent.toFloat(),
            steps =
                ((maxPercent - minPercent) / 5) - 1,
            enabled = enabled,
            colors =
                SliderDefaults.colors(
                    thumbColor = BrutalDeepViolet,
                    activeTrackColor = BrutalDeepViolet,
                    inactiveTrackColor = BrutalLavender
                ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "$minPercent% ~ $maxPercent%",
            color = BrutalDeepViolet,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
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
                color = BrutalLavender,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 2.dp,
                color = BrutalBlack,
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
                color = if (selected) BrutalViolet else BrutalWhite,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.5.dp,
                color = BrutalBlack,
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
            color = if (selected) BrutalWhite else BrutalDeepViolet,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
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
    polaroidPhotoScale: Float = 1f
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
                polaroidPhotoScale = polaroidPhotoScale
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
    val fontUpdateState by viewModel.fontUpdateState.collectAsState()
    val layoutUpdateState by viewModel.layoutUpdateState.collectAsState()
    val dateFormatUpdateState by viewModel.dateFormatUpdateState.collectAsState()
    val stickerBackgroundRemovalState by
        viewModel.stickerBackgroundRemovalState.collectAsState()

    var showDeleteDialog by remember {
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

    var openedDrawerName by rememberSaveable {
        mutableStateOf(
            DetailDrawerSection.LAYOUT.name
        )
    }

    var selectedCustomizationGroup by rememberSaveable {
        mutableStateOf(
            CustomizationGroup.PHOTO.name
        )
    }

    val customizationPagerState = rememberPagerState(
        pageCount = { 2 }
    )
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

    LaunchedEffect(postcardId) {
        viewModel.loadPostcard(postcardId)
        viewModel.loadPhotoStickersState(postcardId)
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

                viewModel.setPhotoStickers(photoStickers.map { sticker ->
                    if (sticker.id != targetId) {
                        return@map sticker
                    }

                    if (sticker.originalUri != removalState.sourceUri) {
                        viewModel.deleteStickerCacheUri(
                            removalState.resultUri
                        )
                        return@map sticker
                    }

                    sticker.removedBgUri?.let { oldUri ->
                        if (oldUri != removalState.resultUri) {
                            viewModel.deleteStickerCacheUri(oldUri)
                        }
                    }

                    sticker.copy(
                        removedBgUri = removalState.resultUri,
                        displayedUri = removalState.resultUri,
                        isBackgroundRemoved = true
                    )
                })

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
            .background(BrutalWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier.height(92.dp)
            )

            postcard?.let { pc ->
                Box(
                    modifier = Modifier.fillMaxWidth(0.88f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(
                                color = Color(
                                    pc.backgroundColorArgb
                                ),
                                shape = RectangleShape
                            )
                            .onSizeChanged { size ->
                                postcardPreviewSize = size
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
                                pc.polaroidPhotoScale
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
                                                color = BrutalViolet,
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
                                                color = BrutalViolet,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    else ->
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(
                                                width = 3.dp,
                                                color = BrutalBlack,
                                                shape = RoundedCornerShape(16.dp)
                                            )
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
                                            coroutineScope {
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
                                                    color = BrutalDeepViolet,
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
                                                color = BrutalCoral,
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
                                                toDelete?.removedBgUri?.let { uri ->
                                                    viewModel
                                                        .deleteStickerCacheUri(uri)
                                                }
                                                val remaining =
                                                    photoStickers.filter {
                                                        it.id != sticker.id
                                                    }
                                                viewModel.setPhotoStickers(remaining)
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
                                                    color = BrutalViolet,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

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
                    modifier = Modifier.height(28.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .background(
                            color = LavenderSoft,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = BrutalBlack,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(6.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    CustomizationGroup.entries.forEach { group ->
                        val groupSelected =
                            selectedCustomizationGroup ==
                                    group.name

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color =
                                        if (groupSelected) {
                                            BrutalViolet
                                        } else {
                                            BrutalWhite
                                        },
                                    shape =
                                        RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = BrutalBlack,
                                    shape =
                                        RoundedCornerShape(10.dp)
                                )
                                .clickable(
                                    enabled = controlsEnabled
                                ) {
                                    selectedCustomizationGroup =
                                        group.name
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Text(
                                text = group.label,
                                color =
                                    if (groupSelected) {
                                        BrutalWhite
                                    } else {
                                        BrutalDeepViolet
                                    },
                                fontSize = 14.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                if (
                    selectedCustomizationGroup ==
                    CustomizationGroup.PHOTO.name
                ) {
                DetailDrawer(
                    title = "레이아웃 꾸미기",
                    summary = selectedLayout.label,
                    expanded =
                        openedDrawerName ==
                                DetailDrawerSection
                                    .LAYOUT
                                    .name,
                    enabled = controlsEnabled,
                    onClick = {
                        openedDrawerName =
                            if (
                                openedDrawerName ==
                                DetailDrawerSection
                                    .LAYOUT
                                    .name
                            ) {
                                ""
                            } else {
                                DetailDrawerSection
                                    .LAYOUT
                                    .name
                            }
                    },
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
                        enabled = controlsEnabled,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                DetailDrawer(
                    title = "사진 크기",
                    summary =
                        if (
                            selectedLayout ==
                            PostcardLayoutStyle.POLAROID
                        ) {
                            "$polaroidPhotoScalePercent%"
                        } else {
                            "$stampPhotoScalePercent%"
                        },
                    expanded =
                        openedDrawerName ==
                                DetailDrawerSection
                                    .PHOTO_SIZE
                                    .name,
                    enabled = controlsEnabled,
                    onClick = {
                        openedDrawerName =
                            if (
                                openedDrawerName ==
                                DetailDrawerSection
                                    .PHOTO_SIZE
                                    .name
                            ) {
                                ""
                            } else {
                                DetailDrawerSection
                                    .PHOTO_SIZE
                                    .name
                            }
                    },
                    modifier =
                        Modifier.fillMaxWidth(0.92f)
                ) {
                    if (
                        selectedLayout ==
                        PostcardLayoutStyle.POLAROID
                    ) {
                        TextSizeControl(
                            label = "폴라로이드 사진 크기",
                            initialPercent =
                                polaroidPhotoScalePercent,
                            minPercent = 75,
                            maxPercent = 105,
                            enabled = controlsEnabled,
                            onPreviewPercentChanged = { percent ->
                                viewModel
                                    .setPolaroidPhotoScalePreview(
                                        percent / 100f
                                    )
                            },
                            onPercentConfirmed = { percent ->
                                viewModel
                                    .savePolaroidPhotoScale(
                                        percent / 100f
                                    )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextSizeControl(
                            label = "우표 사진 크기",
                            initialPercent =
                                stampPhotoScalePercent,
                            minPercent = 70,
                            maxPercent = 130,
                            enabled = controlsEnabled,
                            onPreviewPercentChanged = { percent ->
                                viewModel
                                    .setStampPhotoScalePreview(
                                        percent / 100f
                                    )
                            },
                            onPercentConfirmed = { percent ->
                                viewModel
                                    .saveStampPhotoScale(
                                        percent / 100f
                                    )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                }

                if (
                    selectedCustomizationGroup ==
                    CustomizationGroup.DESIGN.name
                ) {
                DetailDrawer(
                    title = "배경 꾸미기",
                    summary = selectedPattern.label,
                    expanded =
                        openedDrawerName ==
                                DetailDrawerSection
                                    .BACKGROUND
                                    .name,
                    enabled = controlsEnabled,
                    onClick = {
                        openedDrawerName =
                            if (
                                openedDrawerName ==
                                DetailDrawerSection
                                    .BACKGROUND
                                    .name
                            ) {
                                ""
                            } else {
                                DetailDrawerSection
                                    .BACKGROUND
                                    .name
                            }
                    },
                    modifier =
                        Modifier.fillMaxWidth(0.92f)
                ) {
                    PostcardBackgroundPicker(
                        selectedColorArgb =
                            pc.backgroundColorArgb,
                        hasBackgroundImage = false,
                        enabled = controlsEnabled,
                        onColorSelected = { colorArgb ->
                            viewModel.updateBackgroundColor(
                                colorArgb
                            )
                        },
                        onPickImage = {},
                        onRemoveImage = {},
                        selectedPattern = selectedPattern,
                        onPatternSelected = { pattern ->
                            viewModel
                                .updateBackgroundPattern(
                                    pattern.name
                                )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                DetailDrawer(
                    title = "무늬 세기",
                    summary = "$backgroundPatternDensityPercent%",
                    expanded =
                        openedDrawerName ==
                                DetailDrawerSection
                                    .PATTERN_INTENSITY
                                    .name,
                    enabled = controlsEnabled,
                    onClick = {
                        openedDrawerName =
                            if (
                                openedDrawerName ==
                                DetailDrawerSection
                                    .PATTERN_INTENSITY
                                    .name
                            ) {
                                ""
                            } else {
                                DetailDrawerSection
                                    .PATTERN_INTENSITY
                                    .name
                            }
                    },
                    modifier =
                        Modifier.fillMaxWidth(0.92f)
                ) {
                    TextSizeControl(
                        label = "무늬 세기",
                        initialPercent =
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

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                DetailDrawer(
                    title = "글자 크기",
                    summary =
                        "글귀 ${messageTextScalePercent}% · " +
                                "날짜 ${dateTextScalePercent}%",
                    expanded =
                        openedDrawerName ==
                                DetailDrawerSection
                                    .TEXT_SIZE
                                    .name,
                    enabled = controlsEnabled,
                    onClick = {
                        openedDrawerName =
                            if (
                                openedDrawerName ==
                                DetailDrawerSection
                                    .TEXT_SIZE
                                    .name
                            ) {
                                ""
                            } else {
                                DetailDrawerSection
                                    .TEXT_SIZE
                                    .name
                            }
                    },
                    modifier =
                        Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextSizeControl(
                            label = "글귀 크기",
                            initialPercent = messageTextScalePercent,
                            minPercent = 60,
                            maxPercent = 140,
                            enabled = controlsEnabled,
                            onPreviewPercentChanged = { percent ->
                                viewModel.setMessageTextScalePreview(
                                    percent / 100f
                                )
                            },
                            onPercentConfirmed = { percent ->
                                viewModel.saveMessageTextScale(
                                    percent / 100f
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        TextSizeControl(
                            label = "날짜 크기",
                            initialPercent = dateTextScalePercent,
                            minPercent = 60,
                            maxPercent = 180,
                            enabled = controlsEnabled,
                            onPreviewPercentChanged = { percent ->
                                viewModel.setDateTextScalePreview(
                                    percent / 100f
                                )
                            },
                            onPercentConfirmed = { percent ->
                                viewModel.saveDateTextScale(
                                    percent / 100f
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                }

                            }
                        }

                        else -> {
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
                                        val newSticker = PhotoStickerItem(
                                            originalUri = uri,
                                            displayedUri = uri
                                        )
                                        viewModel.setPhotoStickers(photoStickers + newSticker)
                                        viewModel.setSelectedStickerId(newSticker.id)
                                        backgroundRemovalError = null
                                        viewModel.resetStickerBackgroundRemovalState()
                                    },
                                    onDeleteSticker = { id ->
                                        val sticker = photoStickers.find { it.id == id }
                                        sticker?.removedBgUri?.let { uri ->
                                            viewModel.deleteStickerCacheUri(uri)
                                        }
                                        val remaining = photoStickers.filter { it.id != id }
                                        viewModel.setPhotoStickers(remaining)
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

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    repeat(2) { pageIndex ->
                        Box(
                            modifier = Modifier
                                .size(
                                    if (
                                        customizationPagerState.currentPage ==
                                        pageIndex
                                    ) {
                                        11.dp
                                    } else {
                                        8.dp
                                    }
                                )
                                .background(
                                    color =
                                        if (
                                            customizationPagerState.currentPage ==
                                            pageIndex
                                        ) {
                                            BrutalDeepViolet
                                        } else {
                                            BrutalLavender
                                        },
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = BrutalBlack,
                                    shape = CircleShape
                                )
                        )
                    }
                }
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
                            color = BrutalDeepViolet,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "날짜 형식 저장 중...",
                            color = BrutalDeepViolet,
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
                            color = BrutalDeepViolet,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "레이아웃 저장 중...",
                            color = BrutalDeepViolet,
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
                            color = BrutalDeepViolet,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "폰트 저장 중...",
                            color = BrutalDeepViolet,
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
                            color = BrutalDeepViolet,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = "배경 저장 중...",
                            color = BrutalDeepViolet,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Button(
                    onClick = {
                        viewModel.exportPostcardToGallery(
                            stickerOverlays =
                                createStickerOverlaysForExport(
                                    photoStickers =
                                        photoStickers,
                                    postcardSize =
                                        postcardPreviewSize,
                                    stickerSizes =
                                        stickerSizes
                                )
                        )
                    },
                    enabled = controlsEnabled,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                BrutalViolet,
                            contentColor =
                                BrutalWhite,
                            disabledContainerColor =
                                BrutalLavender,
                            disabledContentColor =
                                BrutalDeepViolet
                        ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = BrutalBlack
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .height(56.dp)
                ) {
                    if (
                        exportState
                                is ExportState.Exporting
                    ) {
                        CircularProgressIndicator(
                            color = BrutalWhite,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(
                            modifier = Modifier.size(10.dp)
                        )

                        Text(
                            text = "이미지 만드는 중...",
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                    } else {
                        Text(
                            text =
                                "1:1 이미지로 갤러리에 저장",
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(18.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .offset(
                                    x = 4.dp,
                                    y = 4.dp
                                )
                                .background(
                                    color = BrutalBlack,
                                    shape = CircleShape
                                )
                        )

                        IconButton(
                            onClick = {
                                messageDraft =
                                    pc.message

                                showMessageDialog =
                                    true
                            },
                            enabled = controlsEnabled,
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color =
                                        BrutalLavender,
                                    shape =
                                        CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color =
                                        BrutalBlack,
                                    shape =
                                        CircleShape
                                )
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.Edit,
                                contentDescription =
                                    "글귀 편집",
                                tint =
                                    BrutalDeepViolet
                            )
                        }
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .offset(
                                    x = 4.dp,
                                    y = 4.dp
                                )
                                .background(
                                    color = BrutalBlack,
                                    shape = CircleShape
                                )
                        )

                        IconButton(
                            onClick = {
                                showDeleteDialog = true
                            },
                            enabled = controlsEnabled,
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = BrutalCoral,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color =
                                        BrutalBlack,
                                    shape =
                                        CircleShape
                                )
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.Delete,
                                contentDescription =
                                    "삭제",
                                tint = BrutalBlack
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(40.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrutalDeepViolet)
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
                    tint = BrutalWhite
                )
            }

            Text(
                text = "우표 보기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrutalWhite,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )

            TextButton(
                onClick = {
                    viewModel.savePhotoStickersState(
                        postcardId
                    )
                    Toast.makeText(
                        context,
                        "스티커 배치를 저장했어!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                enabled = controlsEnabled
            ) {
                Text(
                    text = "저장",
                    color = BrutalWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }

    if (showMessageDialog) {
        AlertDialog(
            onDismissRequest = {
                showMessageDialog = false
            },
            title = {
                Text(
                    text = "글귀 남기기",
                    color = BrutalDeepViolet,
                    fontWeight =
                        FontWeight.ExtraBold
                )
            },
            text = {
                Column {
                    Text(
                        text =
                            "이 사진과 함께 기억하고 싶은 말을 적어봐.",
                        color = BrutalDeepViolet,
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
                        color = BrutalDeepViolet,
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
                        color = BrutalDeepViolet,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = LavenderSoft,
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
                        color = BrutalDeepViolet,
                        fontWeight =
                            FontWeight.ExtraBold
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
                        color = BrutalCoral,
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
                    color = BrutalDeepViolet,
                    fontWeight =
                        FontWeight.ExtraBold
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
                        color = BrutalDeepViolet,
                        fontWeight =
                            FontWeight.ExtraBold
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
                        FontWeight.ExtraBold
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
                        color = BrutalDeepViolet,
                        fontWeight =
                            FontWeight.ExtraBold
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
                            FontWeight.ExtraBold
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
                            color = BrutalDeepViolet,
                            fontWeight =
                                FontWeight.ExtraBold
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
                            FontWeight.ExtraBold
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
                            color = BrutalDeepViolet,
                            fontWeight =
                                FontWeight.ExtraBold
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
                            FontWeight.ExtraBold
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
                            color = BrutalDeepViolet,
                            fontWeight =
                                FontWeight.ExtraBold
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
                            FontWeight.ExtraBold
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
                            color = BrutalDeepViolet,
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                    }
                }
            )
        }

        SnackbarHost(
            hostState = textScaleSnackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
