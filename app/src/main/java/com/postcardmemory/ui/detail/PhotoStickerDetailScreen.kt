package com.postcardmemory.ui.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.postcardmemory.ui.components.EditorActionDivider
import com.postcardmemory.ui.components.EditorFlatPresetTile
import com.postcardmemory.ui.components.EditorQuietHint
import com.postcardmemory.ui.components.EditorTextAction
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.components.PhotoSourceMenu
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.GraphiteAccent
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
    isRemovingBackground: Boolean,
    onToggleBackgroundRemoval: () -> Unit,
    canMoveForward: Boolean,
    canMoveBackward: Boolean,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit,
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
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((-4).dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
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
                color = GraphiteAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 스티커 목록 (가로 스크롤)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            photoStickers.forEach { sticker ->
                val isSelected = sticker.id == selectedStickerId

                EditorFlatPresetTile(
                    onClick = { onSelectSticker(sticker.id) },
                    enabled = enabled,
                    previewModifier = Modifier.size(56.dp),
                    selected = isSelected
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
            EditorFlatPresetTile(
                onClick = { showPhotoSourceMenu = true },
                enabled = enabled,
                previewModifier = Modifier.size(56.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = GraphiteAccent,
                        modifier = Modifier.size(18.dp)
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

        // 선택된 스티커 조작 영역
        if (selectedSticker != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "선택한 스티커",
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            StickerEditModeToolbar(
                sticker = selectedSticker,
                isRemovingBackground = isRemovingBackground,
                onToggleBackgroundRemoval = onToggleBackgroundRemoval,
                canMoveForward = canMoveForward,
                canMoveBackward = canMoveBackward,
                onMoveForward = onMoveForward,
                onMoveBackward = onMoveBackward,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            backgroundRemovalError?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = GalleryDangerRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorTextAction(
                    text = "복제",
                    onClick = { onDuplicateSticker(selectedSticker.id) },
                    enabled = enabled
                )

                EditorActionDivider()

                EditorTextAction(
                    text = "삭제",
                    onClick = { onDeleteSticker(selectedSticker.id) },
                    enabled = enabled,
                    contentColor = GalleryDangerRed
                )
            }
        } else if (photoStickers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorQuietHint(
                text = "편집할 스티커를 선택해."
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
