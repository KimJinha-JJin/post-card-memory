package com.postcardmemory.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalDeepViolet
import com.postcardmemory.ui.theme.BrutalLavender
import com.postcardmemory.ui.theme.BrutalViolet
import com.postcardmemory.ui.theme.BrutalWhite

@Composable
fun PhotoStickerPickerPanel(
    photoStickers: List<PhotoStickerItem>,
    selectedStickerId: String?,
    isRemovingBackground: Boolean,
    backgroundRemovalError: String?,
    onSelectSticker: (String) -> Unit,
    onAddFromGallery: (Uri) -> Unit,
    onAddFromFile: (Uri) -> Unit,
    onRemoveBackground: (String) -> Unit,
    onRestoreOriginal: (String) -> Unit,
    onDeleteSticker: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                }
                onAddFromGallery(uri)
            }
        }

    val filePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                }
                onAddFromFile(uri)
            }
        }

    val selectedSticker =
        photoStickers.find { it.id == selectedStickerId }

    Column(
        modifier = modifier
            .background(
                color = BrutalLavender,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 2.dp,
                color = BrutalBlack,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "스티커 사진",
                color = BrutalDeepViolet,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "${photoStickers.size}장",
                color = BrutalDeepViolet,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(
                        color = BrutalWhite,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = BrutalBlack,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "갤러리 사진을 골라서 포스트카드 위에 바로 올려뵐아.",
            color = BrutalDeepViolet,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 스티커 목록 (가로 스크롤)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            photoStickers.forEach { sticker ->
                val isSelected = sticker.id == selectedStickerId

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = BrutalWhite,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = if (isSelected) 3.dp else 2.dp,
                            color = if (isSelected) BrutalViolet else BrutalBlack,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) {
                            onSelectSticker(sticker.id)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = sticker.displayedUri,
                        contentDescription = null,
                        contentScale =
                            if (sticker.isBackgroundRemoved) {
                                ContentScale.Fit
                            } else {
                                ContentScale.Crop
                            },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 추가 버튼
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = BrutalWhite,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = BrutalBlack,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = enabled) {
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = BrutalViolet,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "추가",
                        color = BrutalDeepViolet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 갤러리/파일 버튼
        Button(
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrutalViolet,
                contentColor = BrutalWhite
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = BrutalBlack,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
            Text(
                text = "  갤러리에서 사진 추가",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                filePicker.launch(arrayOf("image/*"))
            },
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
            Text(
                text = "  파일에서 추가",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // 선택된 스티커 조작 영역
        if (selectedSticker != null) {
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BrutalBlack.copy(alpha = 0.15f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "선택된 스티커",
                color = BrutalDeepViolet,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 배경 제거 / 원본 복원
            OutlinedButton(
                onClick = {
                    if (selectedSticker.isBackgroundRemoved) {
                        onRestoreOriginal(selectedSticker.id)
                    } else {
                        onRemoveBackground(selectedSticker.id)
                    }
                },
                enabled = enabled && !isRemovingBackground,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRemovingBackground) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "배경 제거 중...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    Text(
                        text = if (selectedSticker.isBackgroundRemoved) {
                            "원본으로 되돌리기"
                        } else {
                            "배경 제거"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            backgroundRemovalError?.let { errorMessage ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = BrutalCoral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onDeleteSticker(selectedSticker.id) },
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = BrutalCoral
                )
                Text(
                    text = "  선택한 스티커 삭제",
                    color = BrutalCoral,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (photoStickers.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "갤러리 사진을 추가하면 포스트카드 위에서 바로 이동하고 크기를 조절할 수 있어.",
                color = Color(0xFF554B68),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = BrutalWhite,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
