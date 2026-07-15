package com.postcardmemory.ui.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.postcardmemory.ui.components.EditorEmptyHint
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.components.PhotoSourceMenu
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.SunsetCoral
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.SoftGray
import java.io.File
import java.util.UUID

@Composable
fun PhotoStickerPickerPanel(
    photoStickers: List<PhotoStickerItem>,
    selectedStickerId: String?,
    backgroundRemovalError: String?,
    onSelectSticker: (String) -> Unit,
    onAddFromGallery: (Uri) -> Unit,
    onAddFromFile: (Uri) -> Unit,
    onAddFromCamera: (File) -> Unit,
    onDeleteSticker: (String) -> Unit,
    onDuplicateSticker: (String) -> Unit,
    onUndoSticker: () -> Unit,
    onRedoSticker: () -> Unit,
    canUndoSticker: Boolean,
    canRedoSticker: Boolean,
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

    var showPhotoSourceMenu by remember {
        mutableStateOf(false)
    }

    /*
     * TakePicture 콜백은 재구성 뒤에도 올 수 있어서
     * 임시 촬영 경로를 rememberSaveable로 들고 있는다.
     */
    var pendingCameraCapturePath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val cameraCapture =
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
                onAddFromCamera(captureFile)
            } else {
                if (captureFile.exists()) {
                    captureFile.delete()
                }
            }
        }

    fun launchStickerCameraCapture() {
        val captureDir =
            File(
                context.cacheDir,
                "camera_capture"
            )

        if (
            !captureDir.exists() &&
            !captureDir.mkdirs()
        ) {
            return
        }

        val captureFile =
            File(
                captureDir,
                "sticker_capture_" +
                        UUID.randomUUID() +
                        ".jpg"
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
            cameraCapture.launch(captureUri)
        }.onFailure {
            pendingCameraCapturePath = null

            Toast.makeText(
                context,
                "카메라 앱을 찾지 못했어.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val selectedSticker =
        photoStickers.find { it.id == selectedStickerId }

    Column(
        modifier = modifier
            .background(
                color = NeutralLight,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "스티커 사진",
                color = BrutalBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-4).dp)
            ) {
                EditorUndoRedoButtons(
                    canUndo = canUndoSticker,
                    canRedo = canRedoSticker,
                    onUndo = onUndoSticker,
                    onRedo = onRedoSticker,
                    enabled = enabled,
                    undoContentDescription = "실행 취소",
                    redoContentDescription = "다시 실행"
                )

                Text(
                    text = "${photoStickers.size}장",
                    color = BrutalBlack,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            color = BrutalWhite,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "갤러리 사진을 골라서 포스트카드 위에 바로 올려봐.",
            color = BrutalBlack,
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
                        .size(56.dp)
                        .background(
                            color = BrutalWhite,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) SunsetCoral else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) {
                            onSelectSticker(sticker.id)
                        }
                        .padding(3.dp),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            // 추가 버튼
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = BrutalWhite,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = enabled) {
                        showPhotoSourceMenu = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = GraphiteAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "추가",
                        color = BrutalBlack,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                showPhotoSourceMenu = true
            },
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = GraphiteAccent,
                contentColor = BrutalWhite
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Text(
                text = "  사진 추가",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
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
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "이동·크기·회전은 위쪽 도구막대에서 조절할 수 있어.",
                color = BrutalBlack,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            backgroundRemovalError?.let { errorMessage ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = GalleryDangerRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onDuplicateSticker(selectedSticker.id) },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftGray,
                    contentColor = BrutalBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "선택한 스티커 복제",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onDeleteSticker(selectedSticker.id) },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GalleryDangerRed.copy(alpha = 0.16f),
                    contentColor = GalleryDangerRed
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = GalleryDangerRed
                )
                Text(
                    text = "  선택한 스티커 삭제",
                    color = GalleryDangerRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (photoStickers.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorEmptyHint(
                text = "아직 추가한 스티커가 없어."
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }

    if (showPhotoSourceMenu) {
        PhotoSourceMenu(
            onDismiss = {
                showPhotoSourceMenu = false
            },
            onCameraSelected = {
                showPhotoSourceMenu = false
                launchStickerCameraCapture()
            },
            onGallerySelected = {
                showPhotoSourceMenu = false
                photoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onFileSelected = {
                showPhotoSourceMenu = false
                filePicker.launch(arrayOf("image/*"))
            }
        )
    }
}
