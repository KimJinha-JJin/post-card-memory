package com.postcardmemory.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: PostcardRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState

    private var imageCapture: ImageCapture? = null

    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                _captureState.value = CaptureState.Error(e.message ?: "Camera setup failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        _captureState.value = CaptureState.Capturing

        val photoFile = createOutputFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    _captureState.value = CaptureState.Error(exc.message ?: "Capture failed")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    processAndSavePostcard(photoFile)
                }
            }
        )
    }

    private fun processAndSavePostcard(sourceFile: File) {
        viewModelScope.launch {
            try {
                val croppedFile = ImageUtils.cropToStampRatio(context, sourceFile)
                sourceFile.delete()

                val dateFormatter = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
                val postcard = Postcard(
                    imagePath = croppedFile.absolutePath,
                    title = dateFormatter.format(System.currentTimeMillis()),
                    capturedAt = System.currentTimeMillis()
                )
                repository.insertPostcard(postcard)
                _captureState.value = CaptureState.Success
            } catch (e: Exception) {
                _captureState.value = CaptureState.Error(e.message ?: "Processing failed")
            }
        }
    }

    private fun createOutputFile(): File {
        val dir = File(context.filesDir, "postcards_temp")
        dir.mkdirs()
        return File(dir, "temp_${System.currentTimeMillis()}.jpg")
    }

    fun resetState() {
        _captureState.value = CaptureState.Idle
    }
}

sealed class CaptureState {
    object Idle : CaptureState()
    object Capturing : CaptureState()
    object Success : CaptureState()
    data class Error(val message: String) : CaptureState()
}
