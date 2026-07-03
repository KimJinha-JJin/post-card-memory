package com.postcardmemory.ui.detail

import android.net.Uri
import android.widget.Toast
import java.util.UUID
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    originalStickerUri: Uri?,
    isBackgroundRemoved: Boolean,
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
private fun PostcardPreviewContent(
    imagePath: String,
    contentDescription: String?,
    message: String,
    dateText: String,
    selectedFont: PostcardTextFont,
    selectedLayout: PostcardLayoutStyle
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val side = maxWidth
        val messageFontSize =
            ((side.value * 62f / 2048f)
                .coerceIn(10.5f, 14f)).sp
        val messageLineHeight =
            ((side.value * 74f / 2048f)
                .coerceIn(14f, 19f)).sp
        val compactMessageFontSize =
            ((side.value * 50f / 2048f)
                .coerceIn(10f, 13f)).sp
        val compactMessageLineHeight =
            ((side.value * 60f / 2048f)
                .coerceIn(13.5f, 17f)).sp
        val messageHorizontalPadding =
            (side * (65f / 2048f))
                .coerceIn(8.dp, 12.dp)
        val messageVerticalPadding =
            (side * (38f / 2048f))
                .coerceIn(6.dp, 10.dp)
        val dateFontSize =
            ((side.value * 34f / 2048f)
                .coerceIn(9.5f, 12f)).sp
        val compactDateFontSize =
            ((side.value * 30f / 2048f)
                .coerceIn(9f, 11f)).sp
        fun Modifier.exportBounds(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
        ): Modifier =
            this
                .align(Alignment.TopStart)
                .offset(
                    x = side * (left / 2048f),
                    y = side * (top / 2048f)
                )
                .fillMaxWidth(
                    (right - left) / 2048f
                )
                .height(
                    side * ((bottom - top) / 2048f)
                )

        when (selectedLayout) {
            PostcardLayoutStyle.STANDARD -> {
                StampPhoto(
                    imagePath = imagePath,
                    contentDescription =
                        contentDescription,
                    modifier =
                        Modifier.exportBounds(
                            left = 394f,
                            top = 180f,
                            right = 1654f,
                            bottom = 1440f
                        ),
                    outlineColor = Color.White,
                    outlineWidth = 3f
                )

                PostcardMessageCard(
                    message = message,
                    selectedFont = selectedFont,
                    widthFraction = 1f,
                    modifier = Modifier.exportBounds(
                        left = 220f,
                        top = 1505f,
                        right = 1828f,
                        bottom = 1748f
                    ),
                    fontSize = messageFontSize,
                    lineHeight = messageLineHeight,
                    horizontalPadding = messageHorizontalPadding,
                    verticalPadding = messageVerticalPadding,
                    maxLines = 4
                )

                PostcardDateLabel(
                    dateText = dateText,
                    modifier = Modifier.exportBounds(
                        left = 544f,
                        top = 1846f,
                        right = 1504f,
                        bottom = 1932f
                    ),
                    fontSize = dateFontSize
                )
            }

            PostcardLayoutStyle.PHOTO_FOCUS -> {
                StampPhoto(
                    imagePath = imagePath,
                    contentDescription =
                        contentDescription,
                    modifier =
                        Modifier.exportBounds(
                            left = 264f,
                            top = 110f,
                            right = 1784f,
                            bottom = 1630f
                        ),
                    outlineColor = Color.White,
                    outlineWidth = 3f
                )

                PostcardMessageCard(
                    message = message,
                    selectedFont = selectedFont,
                    widthFraction = 1f,
                    compact = true,
                    modifier = Modifier.exportBounds(
                        left = 250f,
                        top = 1670f,
                        right = 1798f,
                        bottom = 1838f
                    ),
                    fontSize = compactMessageFontSize,
                    lineHeight = compactMessageLineHeight,
                    horizontalPadding = messageHorizontalPadding,
                    verticalPadding = messageVerticalPadding,
                    maxLines = 3
                )

                PostcardDateLabel(
                    dateText = dateText,
                    modifier = Modifier.exportBounds(
                        left = 574f,
                        top = 1900f,
                        right = 1474f,
                        bottom = 1972f
                    ),
                    fontSize = compactDateFontSize
                )
            }

            PostcardLayoutStyle.AIRY -> {
                StampPhoto(
                    imagePath = imagePath,
                    contentDescription =
                        contentDescription,
                    modifier =
                        Modifier.exportBounds(
                            left = 534f,
                            top = 250f,
                            right = 1514f,
                            bottom = 1230f
                        ),
                    outlineColor = Color.White,
                    outlineWidth = 3f
                )

                PostcardMessageCard(
                    message = message,
                    selectedFont = selectedFont,
                    widthFraction = 1f,
                    modifier = Modifier.exportBounds(
                        left = 320f,
                        top = 1390f,
                        right = 1728f,
                        bottom = 1666f
                    ),
                    fontSize = messageFontSize,
                    lineHeight = messageLineHeight,
                    horizontalPadding = messageHorizontalPadding,
                    verticalPadding = messageVerticalPadding,
                    maxLines = 4
                )

                PostcardDateLabel(
                    dateText = dateText,
                    modifier = Modifier.exportBounds(
                        left = 544f,
                        top = 1810f,
                        right = 1504f,
                        bottom = 1896f
                    ),
                    fontSize = dateFontSize
                )
            }

            PostcardLayoutStyle.MAGAZINE -> {
                Box(
                    modifier =
                        Modifier.exportBounds(
                            left = 194f,
                            top = 120f,
                            right = 1854f,
                            bottom = 1780f
                        )
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

                PostcardDateLabel(
                    dateText = dateText,
                    modifier = Modifier.exportBounds(
                        left = 544f,
                        top = 1846f,
                        right = 1504f,
                        bottom = 1932f
                    ),
                    fontSize = dateFontSize
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
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit =
        if (compact) {
            15.sp
        } else {
            17.sp
        },
    lineHeight: androidx.compose.ui.unit.TextUnit =
        if (compact) {
            21.sp
        } else {
            25.sp
        },
    horizontalPadding: Dp =
        if (compact) {
            14.dp
        } else {
            16.dp
        },
    verticalPadding: Dp =
        if (compact) {
            10.dp
        } else {
            14.dp
        },
    maxLines: Int =
        if (compact) {
            3
        } else {
            4
        }
) {
    if (message.isBlank()) {
        return
    }

    Box(
        modifier = modifier
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
                horizontal = horizontalPadding,
                vertical = verticalPadding
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = message,
            color = BrutalBlack,
            fontSize = fontSize,
            fontFamily =
                selectedFont.fontFamily,
            fontWeight =
                FontWeight.Normal,
            textAlign =
                TextAlign.Center,
            lineHeight = lineHeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PostcardDateLabel(
    dateText: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dateText,
            fontSize = fontSize,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            color = BrutalBlack,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier.fillMaxWidth()
        )
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

    val context = LocalContext.current

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
                            .aspectRatio(1f)
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

                        photoStickers.forEach { sticker ->
                            val isSelected =
                                sticker.id == selectedStickerId
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
                                        .pointerInput(
                                            sticker.id,
                                            postcardPreviewSize
                                        ) {
                                            coroutineScope {
                                            launch {
                                                detectTapGestures(
                                                    onTap = {
                                                        viewModel.setSelectedStickerId(
                                                            if (selectedStickerId == sticker.id) {
                                                                null
                                                            } else {
                                                                sticker.id
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                            launch {
                                            detectTransformGestures {
                                                centroid, pan, zoom, _ ->

                                                viewModel.setSelectedStickerId(sticker.id)

                                                val currentSticker =
                                                    photoStickers.find {
                                                        it.id == sticker.id
                                                    } ?: return@detectTransformGestures

                                                val oldScale = currentSticker.scale
                                                val newScale =
                                                    (oldScale * zoom).coerceIn(0.5f, 2.5f)
                                                val actualZoom =
                                                    newScale / oldScale

                                                if (postcardPreviewSize == IntSize.Zero) {
                                                    viewModel.setPhotoStickers(
                                                        photoStickers.map {
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
                                                            centroid * (1f - actualZoom)

                                                val newOffset =
                                                    correctedOffset + pan

                                                val newSizePx =
                                                    (baseStickerPx * newScale).roundToInt()
                                                val newEffectiveSize =
                                                    IntSize(newSizePx, newSizePx)

                                                viewModel.setPhotoStickers(
                                                    photoStickers.map {
                                                        if (it.id == sticker.id) {
                                                            it.copy(
                                                                scale = newScale,
                                                                offset = clampStickerOffset(
                                                                    offset = newOffset,
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
                                        }  // launch (transform)
                                        }  // coroutineScope
                                )

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
                            }
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
                                    photoStickers = photoStickers,
                                    selectedStickerId = selectedStickerId,
                                    isRemovingBackground = isRemovingBackground,
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
                                    onRemoveBackground = { id ->
                                        val sticker = photoStickers.find { it.id == id }
                                        if (sticker != null) {
                                            backgroundRemovalError = null
                                            if (sticker.removedBgUri != null) {
                                                viewModel.setPhotoStickers(
                                                    photoStickers.map {
                                                        if (it.id == id) {
                                                            it.copy(
                                                                displayedUri = sticker.removedBgUri,
                                                                isBackgroundRemoved = true
                                                            )
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                )
                                            } else {
                                                viewModel.removeStickerBackground(
                                                    stickerId = id,
                                                    sourceUri = sticker.originalUri
                                                )
                                            }
                                        }
                                    },
                                    onRestoreOriginal = { id ->
                                        viewModel.setPhotoStickers(
                                            photoStickers.map {
                                                if (it.id == id) {
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
}
