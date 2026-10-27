package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.ScreenBackgroundGray
import kotlinx.coroutines.launch

/** 카메라·갤러리·파일 중 사진 출처를 고르는 공용 바텀시트 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceMenu(
    onDismiss: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit,
    onFileSelected: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var isClosing by remember { mutableStateOf(false) }

    fun hideThenRun(action: () -> Unit) {
        if (isClosing) return
        isClosing = true

        scope.launch {
            sheetState.hide()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ScreenBackgroundGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "사진 가져오기",
                color = BrutalBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            PhotoSourceMenuItem(
                icon = Icons.Default.CameraAlt,
                label = "카메라로 촬영",
                onClick = { hideThenRun(onCameraSelected) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            PhotoSourceMenuItem(
                icon = Icons.Default.PhotoLibrary,
                label = "갤러리에서 선택",
                onClick = { hideThenRun(onGallerySelected) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            PhotoSourceMenuItem(
                icon = Icons.Default.FolderOpen,
                label = "파일에서 선택",
                onClick = { hideThenRun(onFileSelected) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            PhotoSourceMenuItem(
                icon = Icons.Default.Close,
                label = "취소",
                onClick = { hideThenRun(onDismiss) },
                emphasized = false
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PhotoSourceMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    emphasized: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (emphasized) NeutralLight else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GraphiteAccent
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            color = BrutalBlack,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
