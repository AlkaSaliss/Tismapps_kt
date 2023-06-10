package com.example.tismapps.ui.data

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ComponentActivity
import androidx.lifecycle.ViewModel
import com.example.tismapps.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class DetectorViewModel: ViewModel() {

    lateinit var cameraExecutor: ExecutorService
        private set

    private lateinit var globalContext: ComponentActivity

    private var screenWidth = 1.dp
    private  var screenHeight = 1.dp

    fun detect(imageProxy: ImageProxy): Bitmap {
        val rotation = imageProxy.imageInfo.rotationDegrees
        var imageBitmap = rotateImage(imageProxy.toBitmap(), rotation)

        imageBitmap = Bitmap.createScaledBitmap(
            imageBitmap,
            YoloV5PrePostProcessor.mInputWidth,
            YoloV5PrePostProcessor.mInputHeight,
            false
        )

        imageBitmap = predictNative(imageBitmap)
        val dpToPx = globalContext.resources.displayMetrics.density

        return Bitmap.createScaledBitmap(
            imageBitmap,
            (screenWidth.value * dpToPx).toInt(),
            (screenHeight.value * dpToPx).toInt(),
            false
        )
    }

    private fun rotateImage(srcImg: Bitmap, rotation: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        return Bitmap.createBitmap(srcImg, 0, 0, srcImg.width, srcImg.height, matrix, srcImg.hasAlpha())
    }

    fun initializeCameraStuff(
        context: ComponentActivity
    ) {
        globalContext = context
        cameraExecutor = Executors.newSingleThreadExecutor()
        loadModuleNative(
            File(assetFilePath(globalContext, "yolov5s.torchscript.ptl")).absolutePath,
            File(assetFilePath(globalContext, "classes.txt")).absolutePath,
            YoloV5PrePostProcessor.confidenceThreshold,
            YoloV5PrePostProcessor.iouThreshold
        )
    }

    fun setScreenDimensions(width: Dp, height: Dp){
        screenWidth = width
        screenHeight = height
    }

    fun destroy() {
        cameraExecutor.shutdown()
    }

    private external fun predictNative(
        imgBitmap: Bitmap
    ): Bitmap

    private external fun loadModuleNative(
        modulePath: String,
        classNamesPath: String,
        confidenceThreshold: Float,
        iouThreshold: Float
    )
}
