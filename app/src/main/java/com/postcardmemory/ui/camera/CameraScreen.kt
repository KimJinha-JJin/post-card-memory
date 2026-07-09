package com.postcardmemory.ui.camera

import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.postcardmemory.R
import com.postcardmemory.ui.components.PinkingPhotoGuide
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.ScreenBackgroundGray
import com.postcardmemory.ui.theme.SoftGray
import com.postcardmemory.ui.theme.SurfaceGray
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    val captureState by viewModel.captureState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var flashVisible by remember {
        mutableStateOf(false)
    }

    val cropState =
        captureState as? CaptureState.CropReady

    /*
     * 크롭 화면에서 뒤로 가기를 누르면
     * 사진을 버리고 촬영 화면으로 돌아간다.
     */
    BackHandler(
        enabled = cropState != null
    ) {
        viewModel.discardCapturedPhoto()
    }

    /*
     * 저장 및 이동 화면에서는
     * 뒤로 가기를 막는다.
     */
    BackHandler(
        enabled =
            captureState is CaptureState.Saving ||
                    captureState is CaptureState.Success
    ) {
        Unit
    }

    /*
     * 촬영 순간 흰색 플래시를 잠깐 표시한다.
     */
    LaunchedEffect(flashVisible) {
        if (flashVisible) {
            delay(120)
            flashVisible = false
        }
    }

    /*
     * 저장 완료 후 연한 라벤더 이동 화면을 보여준 뒤
     * 갤러리로 돌아간다.
     */
    LaunchedEffect(captureState) {
        when (captureState) {
            is CaptureState.Success -> {
                delay(1100)

                viewModel.resetState()
                onNavigateBack()
            }

            else -> Unit
        }
    }

    /*
     * 카메라 권한 요청
     */
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackgroundGray)
    ) {
        when {
            cropState != null -> {
                CropEditorScreen(
                    cropState = cropState,
                    onRetake = {
                        viewModel.discardCapturedPhoto()
                    },
                    onSave = { zoom, offset, viewportSize ->
                        viewModel.saveCroppedPhoto(
                            zoom = zoom,
                            offsetX = offset.x,
                            offsetY = offset.y,
                            viewportSize = viewportSize
                        )
                    }
                )
            }

            captureState is CaptureState.Saving -> {
                TeleportWaitingScreen(
                    title = "우표를 정리하는 중...",
                    subtitle = "수정구가 추억의 좌표를 맞추고 있어요"
                )
            }

            captureState is CaptureState.Success -> {
                TeleportWaitingScreen(
                    title = "갤러리로 이동하는 중...",
                    subtitle = "뿅! 추억 한 장을 안전하게 옮기고 있어요"
                )
            }

            else -> {
                CameraPreviewScreen(
                    permissionGranted =
                        cameraPermission.status.isGranted,
                    captureState = captureState,
                    onSetupCamera = { previewView ->
                        viewModel.setupCamera(
                            lifecycleOwner = lifecycleOwner,
                            surfaceProvider =
                                previewView.surfaceProvider
                        )
                    },
                    onNavigateBack = onNavigateBack,
                    onCapture = {
                        flashVisible = true
                        viewModel.capturePhoto()
                    }
                )
            }
        }

        /*
         * 촬영 순간 화면 전체에 흰색 플래시를 표시한다.
         */
        if (flashVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.White.copy(alpha = 0.94f)
                    )
            )
        }
    }
}

@Composable
private fun CameraPreviewScreen(
    permissionGranted: Boolean,
    captureState: CaptureState,
    onSetupCamera: (PreviewView) -> Unit,
    onNavigateBack: () -> Unit,
    onCapture: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackgroundGray)
    ) {
        if (permissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 70.dp,
                        bottom = 118.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                /*
                 * 실제 카메라 미리보기 자체를
                 * 정사각형 크기로 제한한다.
                 */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(BrutalBlack)
                        .clipToBounds()
                ) {
                    AndroidView(
                        factory = { context ->
                            PreviewView(context).apply {
                                scaleType =
                                    PreviewView.ScaleType.FILL_CENTER

                                implementationMode =
                                    PreviewView.ImplementationMode.COMPATIBLE

                                onSetupCamera(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    /*
                     * 정사각형 미리보기의 가장자리에
                     * 핑킹가위 프레임을 표시한다.
                     */
                    PinkingPhotoGuide(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        outlineColor = BrutalBlack,
                        outlineWidth = 4f,
                        haloColor = NeutralLight,
                        haloWidth = 8f
                    )
                }

                Text(
                    text =
                        "프레임을 기준으로 촬영하고\n다음 화면에서 위치를 조정할 수 있어요",
                    color = BrutalBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .background(
                            color = SurfaceGray,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text =
                        "카메라 권한이 필요합니다\n설정에서 권한을 허용해주세요",
                    color = BrutalBlack,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        /*
         * 뒤로 가기 버튼
         */
        IconButton(
            onClick = onNavigateBack,
            enabled =
                captureState !is CaptureState.Capturing,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    color = SoftGray,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "뒤로",
                tint = BrutalBlack
            )
        }

        /*
         * 촬영 버튼
         */
        if (
            permissionGranted &&
            captureState !is CaptureState.Capturing
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 34.dp)
            ) {
                IconButton(
                    onClick = onCapture,
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = NeutralLight,
                            shape = CircleShape
                        )
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_camera_button),
                        contentDescription = "촬영",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                }
            }
        } else if (
            permissionGranted &&
            captureState is CaptureState.Capturing
        ) {
            /*
             * 촬영 처리 중 표시
             */
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 34.dp)
                    .size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = GraphiteAccent,
                    strokeWidth = 4.dp
                )
            }
        }

        /*
         * 오류 메시지
         */
        val errorState =
            captureState as? CaptureState.Error

        if (errorState != null) {
            Text(
                text = errorState.message,
                color = BrutalBlack,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 124.dp
                    )
                    .background(
                        color = BrutalCoral,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun CropEditorScreen(
    cropState: CaptureState.CropReady,
    onRetake: () -> Unit,
    onSave: (
        zoom: Float,
        offset: Offset,
        viewportSize: Float
    ) -> Unit
) {
    var zoom by remember(cropState.sourcePath) {
        mutableStateOf(1f)
    }

    var offset by remember(cropState.sourcePath) {
        mutableStateOf(Offset.Zero)
    }

    var viewportSize by remember(cropState.sourcePath) {
        mutableStateOf(0f)
    }

    val density = LocalDensity.current

    /*
     * 사진을 움직였을 때
     * 사진 바깥의 빈 공간이 보이지 않도록 이동 범위를 제한한다.
     */
    fun clampOffset(
        proposedOffset: Offset,
        proposedZoom: Float
    ): Offset {
        if (viewportSize <= 0f) {
            return Offset.Zero
        }

        val baseScale = max(
            viewportSize /
                    cropState.imageWidth.toFloat(),
            viewportSize /
                    cropState.imageHeight.toFloat()
        )

        val displayedWidth =
            cropState.imageWidth *
                    baseScale *
                    proposedZoom

        val displayedHeight =
            cropState.imageHeight *
                    baseScale *
                    proposedZoom

        val maximumOffsetX = max(
            0f,
            (displayedWidth - viewportSize) / 2f
        )

        val maximumOffsetY = max(
            0f,
            (displayedHeight - viewportSize) / 2f
        )

        return Offset(
            x = proposedOffset.x.coerceIn(
                -maximumOffsetX,
                maximumOffsetX
            ),
            y = proposedOffset.y.coerceIn(
                -maximumOffsetY,
                maximumOffsetY
            )
        )
    }

    /*
     * 한 손가락 이동과
     * 두 손가락 확대 및 축소를 처리한다.
     */
    val transformableState =
        rememberTransformableState {
                zoomChange,
                panChange,
                _ ->

            val oldZoom = zoom

            val newZoom =
                (oldZoom * zoomChange)
                    .coerceIn(
                        minimumValue = 1f,
                        maximumValue = 4f
                    )

            val zoomRatio =
                if (oldZoom > 0f) {
                    newZoom / oldZoom
                } else {
                    1f
                }

            val proposedOffset =
                Offset(
                    x =
                        offset.x *
                                zoomRatio +
                                panChange.x,
                    y =
                        offset.y *
                                zoomRatio +
                                panChange.y
                )

            zoom = newZoom

            offset = clampOffset(
                proposedOffset = proposedOffset,
                proposedZoom = newZoom
            )
        }

    /*
     * 사진이 크롭 영역을 빈틈없이 채우도록
     * 기본 표시 비율을 계산한다.
     */
    val baseScale =
        if (viewportSize > 0f) {
            max(
                viewportSize /
                        cropState.imageWidth.toFloat(),
                viewportSize /
                        cropState.imageHeight.toFloat()
            )
        } else {
            1f
        }

    val baseDisplayedWidth =
        cropState.imageWidth * baseScale

    val baseDisplayedHeight =
        cropState.imageHeight * baseScale

    val baseDisplayedWidthDp =
        with(density) {
            baseDisplayedWidth.toDp()
        }

    val baseDisplayedHeightDp =
        with(density) {
            baseDisplayedHeight.toDp()
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackgroundGray),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        /*
         * 크롭 화면 상단바
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrutalBlack)
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRetake
            ) {
                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,
                    contentDescription = "다시 촬영",
                    tint = BrutalWhite
                )
            }

            Text(
                text = "우표 영역 고르기",
                color = BrutalWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }

        Text(
            text =
                "사진을 움직이고 두 손가락으로 확대하거나 축소해봐",
            color = BrutalBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                top = 20.dp,
                bottom = 16.dp
            )
        )

        /*
         * 정사각형 크롭 편집 영역
         */
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .aspectRatio(1f)
                .background(BrutalBlack)
                .clipToBounds()
                .onSizeChanged { size ->
                    viewportSize =
                        size.width.toFloat()

                    offset = clampOffset(
                        proposedOffset = offset,
                        proposedZoom = zoom
                    )
                }
                .transformable(
                    state = transformableState
                ),
            contentAlignment = Alignment.Center
        ) {
            if (viewportSize > 0f) {
                AsyncImage(
                    model = File(
                        cropState.sourcePath
                    ),
                    contentDescription = "자를 사진",
                    contentScale =
                        ContentScale.FillBounds,
                    modifier = Modifier
                        .size(
                            width =
                                baseDisplayedWidthDp,
                            height =
                                baseDisplayedHeightDp
                        )
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom

                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }

            /*
             * 크롭 영역의 핑킹가위 테두리
             */
            PinkingPhotoGuide(
                modifier = Modifier.fillMaxSize(),
                outlineColor = BrutalBlack,
                outlineWidth = 4f,
                haloColor = NeutralLight,
                haloWidth = 9f
            )
        }

        Text(
            text =
                "현재 ${String.format("%.1f", zoom)}배 · 최대 4배 확대",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        /*
         * 다시 찍기와 저장 버튼
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 22.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRetake,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceGray,
                    contentColor = BrutalBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )

                Text(
                    text = "다시 찍기",
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Button(
                onClick = {
                    onSave(
                        zoom,
                        offset,
                        viewportSize
                    )
                },
                enabled = viewportSize > 0f,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GraphiteAccent,
                    contentColor = BrutalWhite,
                    disabledContainerColor =
                        NeutralLight,
                    disabledContentColor =
                        BrutalBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )

                Text(
                    text = "이대로 저장",
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TeleportWaitingScreen(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackgroundGray),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            /*
             * 갤러리 이동을 표현하는 수정구
             */
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = SurfaceGray,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔮",
                    fontSize = 54.sp
                )
            }

            Text(
                text = title,
                color = BrutalBlack,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = subtitle,
                color = BrutalBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    top = 10.dp,
                    start = 24.dp,
                    end = 24.dp
                )
            )

            CircularProgressIndicator(
                color = GraphiteAccent,
                strokeWidth = 4.dp,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
