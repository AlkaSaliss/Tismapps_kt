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
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class DetectorViewModel: ViewModel() {

    lateinit var cameraExecutor: ExecutorService
        private set
    private lateinit var modulePt: Module
    private lateinit var moduleTf: InterpreterApi
    private lateinit var classes: MutableList<String>

    private lateinit var globalContext: ComponentActivity

    var screenWidth = 1.dp
    var screenHeight = 1.dp

    private val modelNamePt = "yolov5s.torchscript.ptl"
    private val modelNameTf = "yolov5s-fp16.tflite"

    private var activeFramework = DLFrameworks.PYTORCH

    fun switchFramework(framework: DLFrameworks){
        activeFramework = framework
    }

    private fun predictPt(imageBitmap: Bitmap): FloatArray {
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
        return modulePt.forward(IValue.from(imgTensor)).toTuple()[0].toTensor().dataAsFloatArray
    }

    private fun predictTf(imageBitmap: Bitmap): FloatArray {
        val imgProcessor = ImageProcessor.Builder()
            .add(NormalizeOp(0f, 255f))
            .build()
        var yoloInput = TensorImage(DataType.FLOAT32)
        yoloInput.load(
            Bitmap.createScaledBitmap(
                imageBitmap,
                YoloV5PrePostProcessor.mInputWidth,
                YoloV5PrePostProcessor.mInputHeight,
                false
            )
        )
        yoloInput = imgProcessor.process(yoloInput)
        val outputShape = arrayOf(
            1,
            YoloV5PrePostProcessor.mOutputRow,
            YoloV5PrePostProcessor.mOutputColumn
        ).toIntArray()
        val yoloOutput = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)
        moduleTf.run(yoloInput.buffer, yoloOutput.buffer)
        return yoloOutput.floatArray
    }


    fun detect(imageProxy: ImageProxy): Bitmap {
        val startTime = System.currentTimeMillis()
        val rotation = imageProxy.imageInfo.rotationDegrees
        val imageBitmap = rotateImage(imageProxy.toBitmap(), rotation)
        val outputs = if (activeFramework == DLFrameworks.PYTORCH) predictPt(imageBitmap) else predictTf(imageBitmap)

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
            classes,
            activeFramework == DLFrameworks.TENSORFLOW
        )

        if (rects.size > 0) {

            val classNames = rects.map { it.className }
            rects[0].rect.left
        }

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
        Log.d("YOLO_FPS", "Start: $startTime  -- End:  $endTime  -- Diff: ${endTime-startTime}")
        return Bitmap.createScaledBitmap(
            imageBitmap,
            (screenWidth.value * dpToPx).toInt(),
            (screenHeight.value * dpToPx).toInt(),
            false
        )
    }


    private fun loadModel() {
        modulePt = LiteModuleLoader.load(assetFilePath(globalContext, modelNamePt))
        try {
            val tfliteModel = FileUtil.loadMappedFile(
                globalContext,
                modelNameTf
            )
            moduleTf = InterpreterApi.create(
                tfliteModel, InterpreterApi.Options()
            )
        } catch (e: IOException) {
            Log.e("YOLO_TENSORFLOW", "Error reading model", e)
        }
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