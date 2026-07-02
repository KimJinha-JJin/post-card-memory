package com.postcardmemory.ui.detail

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlin.math.roundToInt
import com.postcardmemory.ui.components.PostcardBackgroundPattern
import com.postcardmemory.ui.components.PostcardBackgroundPicker
import com.postcardmemory.ui.components.PostcardDateFormat
import com.postcardmemory.ui.components.PostcardDateFormatPicker
import com.postcardmemory.ui.components.PostcardFontPicker
import com.postcardmemory.ui.components.PostcardLayoutPicker
import com.postcardmemory.ui.components.PostcardLayoutStyle
import com.postcardmemory.ui.components.PostcardPatternPreview
import com.postcardmemory.ui.components.PostcardTextFont
import com.postcardmemory.ui.components.StampPhoto
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalDeepViolet
import com.postcardmemory.ui.theme.BrutalLavender
import com.postcardmemory.ui.theme.BrutalViolet
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.LavenderSoft
import com.postcardmemory.utils.PostcardImageExporter

private enum class DetailDrawerSection {
    LAYOUT,
    BACKGROUND,
    TEXT,
    DATE
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

private fun createStickerOverlayForExport(
    stickerUri: Uri?,
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
    val availableX =
        (postcardSize.width - stickerSize.width)
            .coerceAtLeast(0)
            .toFloat()
    val availableY =
        (postcardSize.height - stickerSize.height)
            .coerceAtLeast(0)
            .toFloat()

    return PostcardImageExporter.StickerOverlay(
        uri = selectedUri,
        normalizedX =
            if (availableX == 0f) {
                0.5f
            } else {
                (resolvedOffset.x / availableX)
                    .coerceIn(0f, 1f)
            },
        normalizedY =
            if (availableY == 0f) {
                0.5f
            } else {
                (resolvedOffset.y / availableY)
                    .coerceIn(0f, 1f)
            },
        sizeRatio =
            stickerSize.width.toFloat() /
                    postcardSize.width.toFloat()
    )
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
private fun PostcardPreviewContent(
    imagePath: String,
    contentDescription: String?,
    message: String,
    dateText: String,
    selectedFont: PostcardTextFont,
    selectedLayout: PostcardLayoutStyle
) {
    when (selectedLayout) {
        PostcardLayoutStyle.STANDARD -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 24.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                StampPhoto(
                    imagePath = imagePath,
                    contentDescription =
                        contentDescription,
                    modifier =
                        Modifier.fillMaxWidth(0.78f),
                    outlineColor = Color.White,
                    outlineWidth = 3f
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                PostcardMessageCard(
                    message = message,
                    selectedFont = selectedFont,
                    widthFraction = 1f
                )

                if (message.isNotBlank()) {
                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }

                PostcardDateLabel(
                    dateText = dateText
                )
            }
        }

        PostcardLayoutStyle.PHOTO_FOCUS -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 18.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                StampPhoto(
                    imagePath = imagePath,
                    contentDescription =
                        contentDescription,
                    modifier =
                        Modifier.fillMaxWidth(0.94f),
                    outlineColor = Color.White,
                    outlineWidth = 3f
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                PostcardMessageCard(
                    message = message,
                    selectedFont = selectedFont,
                    widthFraction = 0.94f,
                    compact = true
                )

                if (message.isNotBlank()) {
                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )
                }

                PostcardDateLabel(
                    dateText = dateText
                )
            }
        }

        PostcardLayoutStyle.AIRY -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 28.dp,
                        vertical = 34.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                StampPhoto(
                    imagePath = imagePath,
                    contentDescription =
                        contentDescription,
                    modifier =
                        Modifier.fillMaxWidth(0.62f),
                    outlineColor = Color.White,
                    outlineWidth = 3f
                )

                Spacer(
                    modifier = Modifier.height(30.dp)
                )

                PostcardMessageCard(
                    message = message,
                    selectedFont = selectedFont,
                    widthFraction = 0.84f
                )

                if (message.isNotBlank()) {
                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )
                }

                PostcardDateLabel(
                    dateText = dateText
                )
            }
        }

        PostcardLayoutStyle.MAGAZINE -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 20.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth(0.92f)
                ) {
                    StampPhoto(
                        imagePath = imagePath,
                        contentDescription =
                            contentDescription,
                        modifier =
                            Modifier.fillMaxWidth(),
                        outlineColor = Color.White,
                        outlineWidth = 3f
                    )

                    if (message.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(
                                    Alignment.BottomCenter
                                )
                                .fillMaxWidth()
                                .padding(12.dp)
                                .background(
                                    color =
                                        BrutalBlack.copy(
                                            alpha = 0.72f
                                        ),
                                    shape =
                                        RoundedCornerShape(
                                            10.dp
                                        )
                                )
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 10.dp
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Text(
                                text = message,
                                color = BrutalWhite,
                                fontSize = 16.sp,
                                fontFamily =
                                    selectedFont
                                        .fontFamily,
                                fontWeight =
                                    FontWeight.Medium,
                                textAlign =
                                    TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                PostcardDateLabel(
                    dateText = dateText
                )
            }
        }
    }
}

@Composable
private fun PostcardMessageCard(
    message: String,
    selectedFont: PostcardTextFont,
    widthFraction: Float,
    compact: Boolean = false
) {
    if (message.isBlank()) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .background(
                color =
                    BrutalWhite.copy(
                        alpha = 0.88f
                    ),
                shape =
                    RoundedCornerShape(
                        12.dp
                    )
            )
            .padding(
                horizontal =
                    if (compact) {
                        14.dp
                    } else {
                        16.dp
                    },
                vertical =
                    if (compact) {
                        10.dp
                    } else {
                        14.dp
                    }
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = message,
            color = BrutalBlack,
            fontSize =
                if (compact) {
                    15.sp
                } else {
                    17.sp
                },
            fontFamily =
                selectedFont.fontFamily,
            fontWeight =
                FontWeight.Normal,
            textAlign =
                TextAlign.Center,
            lineHeight =
                if (compact) {
                    21.sp
                } else {
                    25.sp
                }
        )
    }
}

@Composable
private fun PostcardDateLabel(
    dateText: String
) {
    Text(
        text = dateText,
        fontSize = 12.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        color = BrutalBlack,
        modifier = Modifier
            .background(
                color =
                    BrutalWhite.copy(
                        alpha = 0.84f
                    ),
                shape =
                    RoundedCornerShape(
                        8.dp
                    )
            )
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    )
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

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var showMessageDialog by remember {
        mutableStateOf(false)
    }

    var messageDraft by remember {
        mutableStateOf("")
    }

    var selectedStickerUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var postcardPreviewSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    var stickerSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    var stickerOffset by remember {
        mutableStateOf<Offset?>(null)
    }

    var openedDrawerName by rememberSaveable {
        mutableStateOf(
            DetailDrawerSection.LAYOUT.name
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
                ?: PostcardLayoutStyle.STANDARD
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

    LaunchedEffect(postcardId) {
        viewModel.loadPostcard(postcardId)
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
        selectedStickerUri,
        postcardPreviewSize,
        stickerSize
    ) {
        val selectedUri = selectedStickerUri

        if (selectedUri == null) {
            stickerOffset = null
            return@LaunchedEffect
        }

        if (
            postcardPreviewSize == IntSize.Zero ||
            stickerSize == IntSize.Zero
        ) {
            return@LaunchedEffect
        }

        val currentOffset = stickerOffset

        stickerOffset =
            if (currentOffset == null) {
                centeredStickerOffset(
                    postcardSize = postcardPreviewSize,
                    stickerSize = stickerSize
                )
            } else {
                clampStickerOffset(
                    offset = currentOffset,
                    postcardSize = postcardPreviewSize,
                    stickerSize = stickerSize
                )
            }
    }

    val controlsEnabled =
        exportState !is ExportState.Exporting &&
                backgroundUpdateState !is BackgroundUpdateState.Saving &&
                fontUpdateState !is FontUpdateState.Saving &&
                layoutUpdateState !is LayoutUpdateState.Saving &&
                dateFormatUpdateState !is DateFormatUpdateState.Saving

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
                            .matchParentSize()
                            .offset(
                                x = 8.dp,
                                y = 10.dp
                            )
                            .background(
                                color = BrutalBlack,
                                shape = RectangleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(
                                    pc.backgroundColorArgb
                                ),
                                shape = RectangleShape
                            )
                            .border(
                                width = 3.dp,
                                color = BrutalBlack,
                                shape = RectangleShape
                            )
                            .onSizeChanged { size ->
                                postcardPreviewSize = size
                            }
                    ) {
                        PostcardPatternPreview(
                            pattern = selectedPattern,
                            backgroundColorArgb = pc.backgroundColorArgb,
                            modifier = Modifier.matchParentSize()
                        )

                        PostcardPreviewContent(
                            imagePath = pc.imagePath,
                            contentDescription = pc.title,
                            message = pc.message,
                            dateText =
                                selectedDateFormat.format(
                                    pc.capturedAt
                                ),
                            selectedFont = selectedFont,
                            selectedLayout = selectedLayout
                        )

                        selectedStickerUri?.let { stickerUri ->
                            val currentStickerOffset =
                                stickerOffset
                            val stickerPositionModifier =
                                if (currentStickerOffset == null) {
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
                                                x =
                                                    currentStickerOffset.x
                                                        .roundToInt(),
                                                y =
                                                    currentStickerOffset.y
                                                        .roundToInt()
                                            )
                                        }
                                }

                            AsyncImage(
                                model = stickerUri,
                                contentDescription =
                                    "포스트카드 스티커 사진",
                                contentScale = ContentScale.Crop,
                                modifier = stickerPositionModifier
                                    .size(120.dp)
                                    .onSizeChanged { size ->
                                        stickerSize = size
                                    }
                                    .pointerInput(
                                        stickerUri,
                                        postcardPreviewSize,
                                        stickerSize
                                    ) {
                                        detectDragGestures(
                                            onDrag = {
                                                    change,
                                                    dragAmount ->

                                                change.consume()

                                                if (
                                                    postcardPreviewSize ==
                                                    IntSize.Zero ||
                                                    stickerSize ==
                                                    IntSize.Zero
                                                ) {
                                                    return@detectDragGestures
                                                }

                                                val startOffset =
                                                    stickerOffset
                                                        ?: centeredStickerOffset(
                                                            postcardSize =
                                                                postcardPreviewSize,
                                                            stickerSize =
                                                                stickerSize
                                                        )

                                                stickerOffset =
                                                    clampStickerOffset(
                                                        offset =
                                                            startOffset +
                                                                    dragAmount,
                                                        postcardSize =
                                                            postcardPreviewSize,
                                                        stickerSize =
                                                            stickerSize
                                                    )
                                            }
                                        )
                                    }
                                    .clip(
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 3.dp,
                                        color = BrutalBlack,
                                        shape =
                                            RoundedCornerShape(
                                                16.dp
                                            )
                                    )
                            )
                        }
                    }
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
                    title = "글귀 꾸미기",
                    summary = selectedFont.label,
                    expanded =
                        openedDrawerName ==
                                DetailDrawerSection
                                    .TEXT
                                    .name,
                    enabled = controlsEnabled,
                    onClick = {
                        openedDrawerName =
                            if (
                                openedDrawerName ==
                                DetailDrawerSection
                                    .TEXT
                                    .name
                            ) {
                                ""
                            } else {
                                DetailDrawerSection
                                    .TEXT
                                    .name
                            }
                    },
                    modifier =
                        Modifier.fillMaxWidth(0.92f)
                ) {
                    PostcardFontPicker(
                        selectedFont = selectedFont,
                        onFontSelected = { font ->
                            viewModel.updateMessageFont(
                                font.name
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
                    title = "날짜 꾸미기",
                    summary = selectedDateFormat.label,
                    expanded =
                        openedDrawerName ==
                                DetailDrawerSection
                                    .DATE
                                    .name,
                    enabled = controlsEnabled,
                    onClick = {
                        openedDrawerName =
                            if (
                                openedDrawerName ==
                                DetailDrawerSection
                                    .DATE
                                    .name
                            ) {
                                ""
                            } else {
                                DetailDrawerSection
                                    .DATE
                                    .name
                            }
                    },
                    modifier =
                        Modifier.fillMaxWidth(0.92f)
                ) {
                    PostcardDateFormatPicker(
                        selectedFormat =
                            selectedDateFormat,
                        onFormatSelected = { dateFormat ->
                            viewModel.updateDateFormat(
                                dateFormat.name
                            )
                        },
                        enabled = controlsEnabled,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                PhotoStickerPickerPanel(
                                    selectedStickerUri =
                                        selectedStickerUri,
                                    onSelectedStickerUriChange = { uri ->
                                        stickerOffset = null
                                        selectedStickerUri = uri
                                    },
                                    enabled = controlsEnabled,
                                    modifier =
                                        Modifier.fillMaxWidth(0.92f)
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
                            stickerOverlay =
                                createStickerOverlayForExport(
                                    stickerUri =
                                        selectedStickerUri,
                                    stickerOffset =
                                        stickerOffset,
                                    postcardSize =
                                        postcardPreviewSize,
                                    stickerSize =
                                        stickerSize
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
                modifier =
                    Modifier.padding(start = 8.dp)
            )
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
}
