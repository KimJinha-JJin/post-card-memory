package com.postcardmemory.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.postcardmemory.ui.components.StampOverlay
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.BrutalYellow

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val captureState by viewModel.captureState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(captureState) {
        if (captureState is CaptureState.Success) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(BrutalBlack)) {
        if (cameraPermission.status.isGranted) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    viewModel.setupCamera(lifecycleOwner, previewView.surfaceProvider)
                }
            )

            // Stamp frame overlay
            StampOverlay(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "카메라 권한이 필요합니다\n설정에서 권한을 허용해주세요",
                    color = BrutalWhite
                )
            }
        }

        // Back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(color = BrutalWhite, shape = CircleShape)
                .border(width = 2.dp, color = BrutalBlack, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "뒤로",
                tint = BrutalBlack
            )
        }

        // Shutter button
        if (captureState !is CaptureState.Capturing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            ) {
                // Shadow
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .offset(x = 4.dp, y = 4.dp)
                        .background(color = BrutalBlack, shape = CircleShape)
                )
                IconButton(
                    onClick = { viewModel.capturePhoto() },
                    modifier = Modifier
                        .size(72.dp)
                        .background(color = BrutalYellow, shape = CircleShape)
                        .border(width = 3.dp, color = BrutalBlack, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "촬영",
                        tint = BrutalBlack,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = BrutalYellow,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}
