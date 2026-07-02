package com.postcardmemory.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postcardmemory.ui.components.StampCard
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalDeepViolet
import com.postcardmemory.ui.theme.BrutalViolet
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.LavenderBackground
import com.postcardmemory.ui.theme.LavenderSurface

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

    val selectionMode = selectedIds.isNotEmpty()

    fun toggleSelection(id: Long) {
        selectedIds =
            if (id in selectedIds) {
                selectedIds - id
            } else {
                selectedIds + id
            }
    }

    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }

    Scaffold(
        containerColor = LavenderBackground,

        topBar = {
            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrutalViolet)
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
                            tint = BrutalWhite
                        )
                    }

                    Text(
                        text = "${selectedIds.size}개 선택",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrutalWhite,
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
                            tint = BrutalCoral
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrutalDeepViolet)
                        .padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        )
                ) {
                    Text(
                        text = "📮 포스트카드 메모리",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrutalWhite
                    )
                }
            }
        },

        floatingActionButton = {
            if (!selectionMode) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .offset(
                                x = 4.dp,
                                y = 4.dp
                            )
                            .background(
                                color = BrutalBlack,
                                shape = CircleShape
                            )
                    )

                    FloatingActionButton(
                        onClick = onNavigateToCamera,
                        containerColor = BrutalViolet,
                        contentColor = BrutalWhite,
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "카메라",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        if (postcards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LavenderBackground)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            color = LavenderSurface,
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
                        color = BrutalDeepViolet,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
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
                    .background(LavenderBackground)
            ) {
                items(
                    items = postcards,
                    key = { postcard ->
                        postcard.id
                    }
                ) { postcard ->
                    StampCard(
                        postcard = postcard,
                        isSelected = postcard.id in selectedIds,
                        onClick = {
                            if (selectionMode) {
                                toggleSelection(postcard.id)
                            } else {
                                onNavigateToDetail(postcard.id)
                            }
                        },
                        onLongClick = {
                            toggleSelection(postcard.id)
                        }
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
            containerColor = LavenderSurface,
            titleContentColor = BrutalDeepViolet,
            textContentColor = BrutalBlack,
            title = {
                Text(
                    text = "${selectedIds.size}개를 삭제할까요?",
                    fontWeight = FontWeight.ExtraBold
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
                        color = BrutalCoral,
                        fontWeight = FontWeight.ExtraBold
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
                        color = BrutalViolet,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}