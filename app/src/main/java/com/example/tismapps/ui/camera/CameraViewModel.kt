package com.example.tismapps.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.core.app.ComponentActivity
import androidx.lifecycle.ViewModel
import com.example.tismapps.DetectionResult
import com.example.tismapps.PrePostProcessor
import com.example.tismapps.R
import com.example.tismapps.assetFilePath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    lateinit var rects: ArrayList<DetectionResult>

    val imgHeight = 1024.dp
    val imgWidth = 512.dp
    private val imgName = "image2.jpg"

    fun handleImageCapture(uri: Uri, context: ComponentActivity) {
        //updateCameraPermission(false)
        //photoUri = uri
        photoUri = Uri.fromFile(File("//android_asset/$imgName"))

        classify(context)
    }

    private fun loadModel(context: ComponentActivity) {
        module = LiteModuleLoader.load(assetFilePath(context, "best.torchscript.ptl"))
    }

    private fun classify(
        context: ComponentActivity,

    ) {

        //imageBitmap = BitmapFactory.decodeFile(photoUri.path)
        imageBitmap = BitmapFactory.decodeStream(context.assets.open(imgName))
        val dpToPx = context.resources.displayMetrics.density

        val resizedImageBitmap = Bitmap.createScaledBitmap(
            imageBitmap,
            PrePostProcessor.mInputWidth,
            PrePostProcessor.mInputHeight,
            false
        )
        val imgTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedImageBitmap,
            PrePostProcessor.NO_MEAN_RGB,
            PrePostProcessor.NO_STD_RGB,
            MemoryFormat.CHANNELS_LAST
        )

        val outputs = module.forward(IValue.from(imgTensor)).toTuple()[0].toTensor().dataAsFloatArray
        val imgScaleX = imageBitmap.width.toFloat() / PrePostProcessor.mInputWidth
        val imgScaleY = imageBitmap.height.toFloat() / PrePostProcessor.mInputHeight
        val ivScaleX: Float = (imgWidth.value * dpToPx) / imageBitmap.width
        val ivScaleY: Float = (imgHeight.value * dpToPx) / imageBitmap.height

        val startX = 0f
        val startY = 0f
        rects = PrePostProcessor.outputsToNMSPredictions(
            outputs,
            imgScaleX,
            imgScaleY,
            ivScaleX,
            ivScaleY,
            startX,
            startY
        )
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
        setOutputDirectory(context)
        loadModel(context)
    }

    fun destroy() {
        cameraExecutor.shutdown()
    }

}