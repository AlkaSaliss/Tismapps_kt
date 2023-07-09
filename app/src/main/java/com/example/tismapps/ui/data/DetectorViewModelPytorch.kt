package com.example.tismapps.ui.data

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.app.ComponentActivity
import androidx.core.graphics.applyCanvas
import androidx.lifecycle.ViewModel
import com.example.tismapps.*
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.MemoryFormat
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class DetectorViewModelPytorch: ViewModel() {

    lateinit var cameraExecutor: ExecutorService
        private set
    private lateinit var module: Module
    private lateinit var classes: MutableList<String>

    private lateinit var globalContext: ComponentActivity

    var screenWidth = 1.dp
    var screenHeight = 1.dp

    private val modelName = "yolov5s.torchscript.ptl"

    fun detect(imageProxy: ImageProxy): Bitmap {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val imageBitmap = rotateImage(imageProxy.toBitmap(), rotation)

        val imgTensor = TensorImageUtils.bitmapToFloat32Tensor(
            Bitmap.createScaledBitmap(
                imageBitmap,
                YoloV5PrePostProcessor.mInputWidth,
                YoloV5PrePostProcessor.mInputHeight,
                false
            ),
            YoloV5PrePostProcessor.NO_MEAN_RGB,
            YoloV5PrePostProcessor.NO_STD_RGB,
            MemoryFormat.CHANNELS_LAST
        )

        val startTime = System.currentTimeMillis()
        val outputs =
            module.forward(IValue.from(imgTensor)).toTuple()[0].toTensor().dataAsFloatArray

        val imgScaleX = imageBitmap.width.toFloat() / YoloV5PrePostProcessor.mInputWidth
        val imgScaleY = imageBitmap.height.toFloat() / YoloV5PrePostProcessor.mInputHeight

        val startX = 0f
        val startY = 0f
        val rects = YoloV5PrePostProcessor.outputsToNMSPredictions(
            outputs,
            imgScaleX,
            imgScaleY,
            1f,
            1f,
            startX,
            startY,
            classes
        )

        imageBitmap.applyCanvas {
            val paintImage = Paint().apply {
                color = Color(0xFF00FF00).toArgb()
                style = Paint.Style.STROKE
                strokeWidth = 2F
            }
            val paintText = Paint().apply {
                color = Color(0, 255, 0, 255).toArgb()
                textSize = 20f
            }
            rects.forEach {
                this.drawRect(
                    it.rect.left.toFloat(),
                    it.rect.top.toFloat(),
                    (it.rect.left + it.rect.width()).toFloat(),
                    (it.rect.top + it.rect.height()).toFloat(),
                    paintImage
                )
                this.drawText(
                    it.className,
                    it.rect.left.toFloat(),
                    it.rect.top.toFloat(),
                    paintText
                )
            }
        }
        val dpToPx = globalContext.resources.displayMetrics.density
        val endTime = System.currentTimeMillis()
        Log.d("FPS", "Start: $startTime  -- End:  $endTime  -- Diff: ${endTime-startTime}")
        return Bitmap.createScaledBitmap(
            imageBitmap,
            (screenWidth.value * dpToPx).toInt(),
            (screenHeight.value * dpToPx).toInt(),
            false
        )
    }


    private fun loadModel() {
        module = LiteModuleLoader.load(assetFilePath(globalContext, modelName))
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
        loadModel()
        classes = loadClasses(globalContext)
    }

    fun destroy() {
        cameraExecutor.shutdown()
    }

}