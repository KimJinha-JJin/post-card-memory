package com.postcardmemory.ui.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    selectedStickerUri: Uri?,
    onSelectedStickerUriChange: (Uri?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                onSelectedStickerUriChange(uri)
            }
        }

    val filePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                onSelectedStickerUriChange(uri)
            }
        }

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
                text = "2 / 2",
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
                    .padding(
                        horizontal = 9.dp,
                        vertical = 5.dp
                    )
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "갤러리 사진을 골라서 포스트카드 위에 바로 올려봐.",
            color = BrutalDeepViolet,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    color = BrutalWhite,
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    width = 2.dp,
                    color = BrutalBlack,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val selectedUri = selectedStickerUri

            if (selectedUri == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = BrutalViolet
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "아직 고른 사진이 없어",
                        color = BrutalDeepViolet,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                AsyncImage(
                    model = selectedUri,
                    contentDescription = "선택한 스티커 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

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
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null
            )

            Text(
                text = "  갤러리에서 사진 선택",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedButton(
            onClick = {
                filePicker.launch(
                    arrayOf("image/*")
                )
            },
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null
            )

            Text(
                text = "\uD30C\uC77C\uC5D0\uC11C \uC120\uD0DD",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (selectedStickerUri != null) {
            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedButton(
                onClick = {
                    onSelectedStickerUriChange(null)
                },
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
                    text = "  선택한 사진 지우기",
                    color = BrutalCoral,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = "지금은 포스트카드 가운데에 한 장만 보여줘. 이동과 크기 조절은 다음 단계에서 붙일 거야.",
            color = Color(0xFF554B68),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BrutalWhite,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                )
        )
    }
}
