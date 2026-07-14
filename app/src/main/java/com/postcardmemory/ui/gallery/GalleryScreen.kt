package com.postcardmemory.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postcardmemory.R
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCard
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.PaperTray
import com.postcardmemory.ui.theme.SurfaceGray

private val ViewModeSaver = Saver<GalleryViewMode, String>(
    save = { it.name },
    restore = { GalleryViewMode.valueOf(it) }
)

private val SortOrderSaver = Saver<GallerySortOrder, String>(
    save = { it.name },
    restore = { GallerySortOrder.valueOf(it) }
)

@Composable
fun GalleryScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val postcards by viewModel.postcards.collectAsState()

    var selectedIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var viewMode by rememberSaveable(stateSaver = ViewModeSaver) {
        mutableStateOf(GalleryViewMode.COMPACT_GRID)
    }

    var sortOrder by rememberSaveable(stateSaver = SortOrderSaver) {
        mutableStateOf(GallerySortOrder.NEWEST)
    }

    var viewMenuExpanded by remember {
        mutableStateOf(false)
    }

    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }

    val selectionMode = selectedIds.isNotEmpty()

    fun toggleSelection(id: Long) {
        selectedIds =
            if (id in selectedIds) {
                selectedIds - id
            } else {
                selectedIds + id
            }
    }

    fun handleItemClick(id: Long) {
        if (selectedIds.isNotEmpty()) {
            toggleSelection(id)
        } else {
            onNavigateToDetail(id)
        }
    }

    fun handleItemLongClick(id: Long) {
        toggleSelection(id)
    }

    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }

    Scaffold(
        containerColor = GalleryPaperWhite,

        topBar = {
            if (selectionMode) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GalleryPaperWhite)
                            .padding(
                                horizontal = 8.dp,
                                vertical = 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                selectedIds = emptySet()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "선택 취소",
                                tint = BrutalBlack
                            )
                        }

                        Text(
                            text = "${selectedIds.size}개 선택",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrutalCoral,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                showDeleteDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "선택 항목 삭제",
                                tint = GalleryDangerRed
                            )
                        }
                    }

                    HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GalleryPaperWhite)
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "포스트카드 메모리",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrutalBlack,
                            modifier = Modifier.weight(1f)
                        )

                        Box {
                            IconButton(
                                onClick = {
                                    viewMenuExpanded = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "보기 방식 변경",
                                    tint = BrutalBlack
                                )
                            }

                            DropdownMenu(
                                expanded = viewMenuExpanded,
                                onDismissRequest = {
                                    viewMenuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("3열 그리드 보기")
                                    },
                                    onClick = {
                                        viewMode = GalleryViewMode.COMPACT_GRID
                                        viewMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("세부 기록 보기")
                                    },
                                    onClick = {
                                        viewMode = GalleryViewMode.DETAIL_LIST
                                        viewMenuExpanded = false
                                    }
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    sortMenuExpanded = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "정렬 방식 변경",
                                    tint = BrutalBlack
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = {
                                    sortMenuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("날짜 최신순")
                                    },
                                    onClick = {
                                        sortOrder = GallerySortOrder.NEWEST
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("날짜 오래된 순")
                                    },
                                    onClick = {
                                        sortOrder = GallerySortOrder.OLDEST
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
                }
            }
        },

        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = onNavigateToCamera,
                    containerColor = BrutalWhite,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_camera_button),
                        contentDescription = "카메라",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    ) { paddingValues ->

        if (postcards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GalleryPaperWhite)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            color = PaperTray,
                            shape = CircleShape
                        )
                        .padding(
                            horizontal = 38.dp,
                            vertical = 32.dp
                        )
                ) {
                    Text(
                        text = "📮",
                        fontSize = 64.sp
                    )

                    Text(
                        text = "아직 추억이 없어요\n첫 번째 사진을 찍어봐요!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrutalBlack,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else {
            val displayedPostcards = remember(postcards, sortOrder) {
                when (sortOrder) {
                    GallerySortOrder.NEWEST ->
                        postcards.sortedByDescending { it.capturedAt }

                    GallerySortOrder.OLDEST ->
                        postcards.sortedBy { it.capturedAt }
                }
            }

            when (viewMode) {
                GalleryViewMode.COMPACT_GRID -> {
                    GalleryGrid(
                        postcards = displayedPostcards,
                        selectedIds = selectedIds,
                        paddingValues = paddingValues,
                        onItemClick = ::handleItemClick,
                        onItemLongClick = ::handleItemLongClick
                    )
                }

                GalleryViewMode.DETAIL_LIST -> {
                    GalleryDetailList(
                        postcards = displayedPostcards,
                        selectedIds = selectedIds,
                        paddingValues = paddingValues,
                        onItemClick = ::handleItemClick,
                        onItemLongClick = ::handleItemLongClick
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            containerColor = PaperSurface,
            titleContentColor = BrutalBlack,
            textContentColor = BrutalBlack,
            title = {
                Text(
                    text = "${selectedIds.size}개를 삭제할까요?",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "선택한 사진은 삭제 후 복구할 수 없어요."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idsToDelete = selectedIds

                        showDeleteDialog = false
                        selectedIds = emptySet()

                        viewModel.deletePostcards(idsToDelete)
                    }
                ) {
                    Text(
                        text = "삭제",
                        color = GalleryDangerRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        text = "취소",
                        color = GraphiteAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun GalleryGrid(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    paddingValues: PaddingValues,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = paddingValues.calculateTopPadding() + 14.dp,
            bottom = paddingValues.calculateBottomPadding() + 88.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
    ) {
        lazyGridItems(
            items = postcards,
            key = { postcard ->
                postcard.id
            }
        ) { postcard ->
            StampCard(
                postcard = postcard,
                isSelected = postcard.id in selectedIds,
                onClick = {
                    onItemClick(postcard.id)
                },
                onLongClick = {
                    onItemLongClick(postcard.id)
                }
            )
        }
    }
}

@Composable
private fun GalleryDetailList(
    postcards: List<Postcard>,
    selectedIds: Set<Long>,
    paddingValues: PaddingValues,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 88.dp
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
            ) {
                Text(
                    text = "날짜",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GraphiteAccent,
                    modifier = Modifier.width(96.dp)
                )

                Text(
                    text = "내용",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GraphiteAccent
                )
            }

            HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
        }

        lazyColumnItems(
            items = postcards,
            key = { postcard ->
                postcard.id
            }
        ) { postcard ->
            PostcardDetailRow(
                postcard = postcard,
                isSelected = postcard.id in selectedIds,
                onClick = {
                    onItemClick(postcard.id)
                },
                onLongClick = {
                    onItemLongClick(postcard.id)
                }
            )

            HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
        }
    }
}
