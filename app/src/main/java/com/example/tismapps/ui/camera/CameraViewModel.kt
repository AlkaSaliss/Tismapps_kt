package com.example.tismapps.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.ComponentActivity
import androidx.lifecycle.ViewModel
import com.example.tismapps.R
import com.example.tismapps.assetFilePath
import com.example.tismapps.ui.data.IMAGENET_CLASSES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.MemoryFormat
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraViewModel: ViewModel() {
    private val _cameraUiState = MutableStateFlow(CameraUiState())
    val cameraUiState: StateFlow<CameraUiState> = _cameraUiState.asStateFlow()

    lateinit var outputDirectory: File
        private set
    lateinit var cameraExecutor: ExecutorService
        private set
    lateinit var photoUri: Uri
        private set
    lateinit var module: Module
        private set
    lateinit var imageBitmap: Bitmap
        private set

    private fun updatePredictedClass(value: String) {
        _cameraUiState.update { currentState ->
            currentState.copy(className = value)
        }
    }

    /*
    private fun updateShouldShowPhoto(value: Boolean) {
        _cameraUiState.update { currentState ->
            currentState.copy(shouldShowPhoto = value)
        }
    }
    fun updateCameraPermission(value: Boolean) {
        _cameraUiState.update { currentState ->
            currentState.copy(shouldShowCamera = value)
        }
    }
    private fun requestCameraPermission(
        activity: ComponentActivity,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        when {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                updateCameraPermission(true)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.CAMERA
            ) -> {}

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }*/

    fun handleImageCapture(uri: Uri) {
        //updateCameraPermission(false)
        photoUri = uri
        classify()
    }

    private fun loadModel(context: ComponentActivity) {
        module = LiteModuleLoader.load(assetFilePath(context, "model.ptl"))
    }

    private fun classify() {
        imageBitmap = BitmapFactory.decodeFile(photoUri.path)
        val imgTensor = TensorImageUtils.bitmapToFloat32Tensor(
            imageBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB,
            MemoryFormat.CHANNELS_LAST
        )

            val scores = module.forward(IValue.from(imgTensor)).toTensor().dataAsFloatArray
            var maxScore = -Float.MAX_VALUE
            var maxScoreIdx = -1
            for (i in scores.indices) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i]
                    maxScoreIdx = i
                }
            }
            updatePredictedClass(IMAGENET_CLASSES[maxScoreIdx])
    }

    private fun setOutputDirectory(context: ComponentActivity) {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        outputDirectory = if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir

    }

    fun initializeCameraExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun initializeCameraStuff(
        context: ComponentActivity,
    ) {
        //requestCameraPermission(context, requestPermissionLauncher)
        setOutputDirectory(context)
        loadModel(context)
    }

    fun destroy() {
        cameraExecutor.shutdown()
    }

}