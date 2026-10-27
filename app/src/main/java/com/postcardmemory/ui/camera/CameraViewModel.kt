package com.postcardmemory.ui.camera

import android.content.Context
import android.util.Rational
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.utils.ImageUtils
import com.postcardmemory.utils.withProvisionalFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: PostcardRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _captureState =
        MutableStateFlow<CaptureState>(
            CaptureState.Idle
        )

    val captureState: StateFlow<CaptureState> =
        _captureState

    private var imageCapture: ImageCapture? = null

    private var pendingSourcePath: String? = null

    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider =
                        cameraProviderFuture.get()

                    val preview =
                        Preview.Builder()
                            .build()
                            .also { cameraPreview ->
                                cameraPreview.setSurfaceProvider(
                                    surfaceProvider
                                )
                            }

                    val newImageCapture =
                        ImageCapture.Builder()
                            .setCaptureMode(
                                ImageCapture
                                    .CAPTURE_MODE_MINIMIZE_LATENCY
                            )
                            .setJpegQuality(95)
                            .build()

                    /*
                     * 촬영 파일도 1:1 정사각형 영역을
                     * 기준으로 잘리도록 요청한다.
                     */
                    newImageCapture.setCropAspectRatio(
                        Rational(1, 1)
                    )

                    /*
                     * Preview와 ImageCapture가
                     * 완전히 동일한 중앙 1:1 영역을 사용한다.
                     */
                    val squareViewPort =
                        ViewPort.Builder(
                            Rational(1, 1),
                            Surface.ROTATION_0
                        )
                            .setScaleType(
                                ViewPort.FILL_CENTER
                            )
                            .build()

                    val squareUseCaseGroup =
                        UseCaseGroup.Builder()
                            .setViewPort(squareViewPort)
                            .addUseCase(preview)
                            .addUseCase(newImageCapture)
                            .build()

                    imageCapture = newImageCapture

                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        squareUseCaseGroup
                    )
                } catch (exception: Exception) {
                    _captureState.value =
                        CaptureState.Error(
                            exception.message
                                ?: "카메라를 실행하지 못했습니다."
                        )
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun capturePhoto() {
        val currentImageCapture = imageCapture

        if (currentImageCapture == null) {
            _captureState.value =
                CaptureState.Error(
                    "카메라가 아직 준비되지 않았습니다."
                )
            return
        }

        if (_captureState.value is CaptureState.Capturing) {
            return
        }

        _captureState.value =
            CaptureState.Capturing

        val photoFile = createOutputFile()

        pendingSourcePath =
            photoFile.absolutePath

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build()

        currentImageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    photoFile.delete()
                    pendingSourcePath = null

                    _captureState.value =
                        CaptureState.Error(
                            exception.message
                                ?: "사진 촬영에 실패했습니다."
                        )
                }

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {
                    preparePhotoForCropping(
                        sourceFile = photoFile
                    )
                }
            }
        )
    }

    private fun preparePhotoForCropping(
        sourceFile: File
    ) {
        viewModelScope.launch {
            try {
                val imageSize =
                    withContext(Dispatchers.IO) {
                        ImageUtils.getOrientedImageSize(
                            sourceFile = sourceFile
                        )
                    }

                _captureState.value =
                    CaptureState.CropReady(
                        sourcePath =
                            sourceFile.absolutePath,
                        imageWidth =
                            imageSize.width,
                        imageHeight =
                            imageSize.height
                    )
            } catch (exception: Exception) {
                withContext(Dispatchers.IO) {
                    sourceFile.delete()
                }

                pendingSourcePath = null

                _captureState.value =
                    CaptureState.Error(
                        exception.message
                            ?: "촬영한 사진을 불러오지 못했습니다."
                    )
            }
        }
    }

    fun saveCroppedPhoto(
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        viewportSize: Float
    ) {
        val cropState =
            _captureState.value
                    as? CaptureState.CropReady
                ?: return

        if (viewportSize <= 0f) {
            _captureState.value =
                CaptureState.Error(
                    "사진 자르기 영역을 확인하지 못했습니다."
                )
            return
        }

        _captureState.value =
            CaptureState.Saving

        val sourceFile =
            File(cropState.sourcePath)

        viewModelScope.launch {
            try {
                val capturedAt =
                    System.currentTimeMillis()

                /*
                 * 잘라낸 파일은 DB가 그 경로를 커밋하기 전까지 "임시 소유"다.
                 * insert가 실패하거나 화면 이탈로 coroutine이 취소되면 그 파일을
                 * 참조할 주체가 영원히 없으므로 withProvisionalFile이 지운다.
                 * 커밋에 성공하면 소유권이 DB로 넘어가 더는 건드리지 않는다.
                 */
                val savedImagePath =
                    withProvisionalFile(
                        produce = {
                            withContext(Dispatchers.IO) {
                                ImageUtils.cropToStampRatio(
                                    context = context,
                                    sourceFile = sourceFile,
                                    zoom = zoom,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    viewportSize = viewportSize
                                )
                            }
                        },
                        commit = { croppedFile ->
                            val dateFormatter =
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                )

                            val postcard =
                                Postcard(
                                    imagePath =
                                        croppedFile.absolutePath,
                                    title =
                                        dateFormatter.format(capturedAt),
                                    capturedAt =
                                        capturedAt
                                )

                            withContext(Dispatchers.IO) {
                                repository.insertPostcard(
                                    postcard
                                )
                            }

                            croppedFile.absolutePath
                        }
                    )

                _captureState.value =
                    CaptureState.Success(
                        imagePath = savedImagePath
                    )
            } catch (cancellation: CancellationException) {
                // 취소는 저장 실패가 아니다. 에러 화면을 띄우지 않고 그대로
                // 전파하되, 아래 finally에서 촬영 임시 파일은 반드시 정리한다.
                throw cancellation
            } catch (exception: Exception) {
                _captureState.value =
                    CaptureState.Error(
                        exception.message
                            ?: "우표 사진을 저장하지 못했습니다."
                    )
            } finally {
                /*
                 * NonCancellable: 취소로 여기 왔을 때 평범한 withContext는
                 * 즉시 다시 취소돼 아무것도 지우지 못한다. 정리 자체가 짧은
                 * 파일 삭제 한 번이라 취소 불가로 돌려도 안전하다. 지우는
                 * 대상은 앱이 만든 촬영 임시 파일(postcards_temp/)뿐이다.
                 */
                withContext(NonCancellable + Dispatchers.IO) {
                    if (sourceFile.exists()) {
                        sourceFile.delete()
                    }
                }

                pendingSourcePath = null
            }
        }
    }

    fun discardCapturedPhoto() {
        val cropState =
            _captureState.value
                    as? CaptureState.CropReady
                ?: return

        _captureState.value =
            CaptureState.Idle

        pendingSourcePath = null

        viewModelScope.launch(Dispatchers.IO) {
            File(cropState.sourcePath).delete()
        }
    }

    private fun createOutputFile(): File {
        val directory =
            File(
                context.filesDir,
                "postcards_temp"
            )

        if (!directory.exists()) {
            val created =
                directory.mkdirs()

            if (!created && !directory.exists()) {
                throw IllegalStateException(
                    "임시 사진 폴더를 만들지 못했습니다."
                )
            }
        }

        return File(
            directory,
            "temp_${System.currentTimeMillis()}.jpg"
        )
    }

    fun resetState() {
        _captureState.value =
            CaptureState.Idle
    }

    override fun onCleared() {
        pendingSourcePath?.let { path ->
            File(path).delete()
        }

        pendingSourcePath = null

        super.onCleared()
    }
}

sealed class CaptureState {

    object Idle : CaptureState()

    object Capturing : CaptureState()

    data class CropReady(
        val sourcePath: String,
        val imageWidth: Int,
        val imageHeight: Int
    ) : CaptureState()

    object Saving : CaptureState()

    data class Success(
        val imagePath: String
    ) : CaptureState()

    data class Error(
        val message: String
    ) : CaptureState()
}