package com.postcardmemory.ui.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.BrutalDeepViolet
import com.postcardmemory.ui.theme.BrutalWhite

@Composable
fun PhotoStickerDetailScreen(
    postcardId: Long,
    onNavigateBack: () -> Unit
) {
    var selectedStickerUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedStickerUri = uri
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        DetailScreen(
            postcardId = postcardId,
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 20.dp,
                    bottom = 28.dp
                ),
            horizontalAlignment = Alignment.End
        ) {
            selectedStickerUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .size(
                            width = 132.dp,
                            height = 154.dp
                        )
                        .background(
                            color = BrutalWhite,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(
                            width = 3.dp,
                            color = BrutalBlack,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(8.dp)
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "선택한 스티커 사진",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )

                    IconButton(
                        onClick = {
                            selectedStickerUri = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(34.dp)
                            .background(
                                color = BrutalCoral,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "선택한 사진 제거",
                            tint = BrutalWhite
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "사진 선택 완료",
                    color = BrutalDeepViolet,
                    fontSize = 12.sp,
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
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        )
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            Button(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts
                                .PickVisualMedia
                                .ImageOnly
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrutalDeepViolet,
                    contentColor = BrutalWhite
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.border(
                    width = 2.dp,
                    color = BrutalBlack,
                    shape = RoundedCornerShape(14.dp)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null
                )

                Text(
                    text = "  스티커 사진",
                    fontSize = 14.sp
                )
            }
        }
    }
}
