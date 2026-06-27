package com.postcardmemory.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.postcardmemory.ui.components.StampBorderCanvas
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.BrutalYellow
import com.postcardmemory.ui.theme.pastelColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DetailScreen(
    postcardId: Long,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val postcard by viewModel.postcard.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(postcardId) {
        viewModel.loadPostcard(postcardId)
    }

    LaunchedEffect(deleted) {
        if (deleted) onNavigateBack()
    }

    val cardColor = remember(postcardId) {
        pastelColors[abs(postcardId.toInt()) % pastelColors.size]
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREAN) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrutalWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            postcard?.let { pc ->
                // Large stamp card
                Box(modifier = Modifier.fillMaxWidth(0.8f)) {
                    // Shadow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = 6.dp, y = 6.dp)
                            .background(color = BrutalBlack, shape = RoundedCornerShape(12.dp))
                            .aspectRatio(3f / 4.5f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = cardColor, shape = RoundedCornerShape(12.dp))
                            .border(width = 3.dp, color = BrutalBlack, shape = RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                        ) {
                            AsyncImage(
                                model = File(pc.imagePath),
                                contentDescription = pc.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            StampBorderCanvas(
                                modifier = Modifier.fillMaxSize(),
                                backgroundColor = Color.Transparent,
                                perfSize = 10.dp,
                                strokeWidth = 3.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = dateFormatter.format(Date(pc.capturedAt)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrutalBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Delete button
                    Box {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .offset(x = 3.dp, y = 3.dp)
                                .background(color = BrutalBlack, shape = CircleShape)
                        )
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(56.dp)
                                .background(color = BrutalCoral, shape = CircleShape)
                                .border(width = 2.dp, color = BrutalBlack, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "삭제",
                                tint = BrutalBlack
                            )
                        }
                    }
                }
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrutalBlack)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로",
                    tint = BrutalYellow
                )
            }
            Text(
                text = "우표 보기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrutalYellow,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("삭제하시겠어요?", fontWeight = FontWeight.Bold) },
            text = { Text("이 우표를 삭제하면 복구할 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePostcard()
                    showDeleteDialog = false
                }) {
                    Text("삭제", color = BrutalCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}
