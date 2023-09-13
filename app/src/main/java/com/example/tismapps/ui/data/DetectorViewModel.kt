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
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class DetectorViewModel: ViewModel() {

    lateinit var cameraExecutor: ExecutorService
        private set
    private lateinit var modulePt: Module
    private lateinit var moduleTf: InterpreterApi
    private lateinit var moduleTfGpu: Interpreter
    private lateinit var classes: MutableList<String>

    private lateinit var globalContext: ComponentActivity

    var screenWidth = 1.dp
    var screenHeight = 1.dp

    private val modelNamePt = "yolov5s.torchscript.ptl"
    private val modelNameTf = "yolov5s-fp16.tflite"
    private val modelNameOnnx = "yolov5s.onnx"

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

    private fun predictTf(imageBitmap: Bitmap, useGpu: Boolean): FloatArray {
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
        if (useGpu)
            moduleTfGpu.run(yoloInput.buffer, yoloOutput.buffer)
        else
            moduleTf.run(yoloInput.buffer, yoloOutput.buffer)

        return yoloOutput.floatArray
    }


    fun detect(imageProxy: ImageProxy): Bitmap {
        val startTime = System.currentTimeMillis()
        val rotation = imageProxy.imageInfo.rotationDegrees
        var imageBitmap = rotateImage(imageProxy.toBitmap(), rotation)
            val outputs = when(activeFramework){
                DLFrameworks.TENSORFLOW_GPU -> predictTf(imageBitmap, true)
                DLFrameworks.TENSORFLOW -> predictTf(imageBitmap, false)
                DLFrameworks.ONNX -> FloatArray(85 * 25200){0f}
                else -> predictPt(imageBitmap)
            }

        if (activeFramework == DLFrameworks.ONNX) {
            val scaledBitmap = Bitmap.createScaledBitmap(
                imageBitmap,
                YoloV5PrePostProcessor.mInputWidth,
                YoloV5PrePostProcessor.mInputHeight,
                false
            )
            Log.d("ONNXCPP", "GPU OK ✅ ✅${scaledBitmap.width} ${scaledBitmap.height}")
            //imageBitmap = predictNativeOnnx(scaledBitmap)
            imageBitmap = predictNativeOnnx(scaledBitmap)
        }
        else {
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
                activeFramework != DLFrameworks.PYTORCH
            )

            if (rects.size > 0) {

                val classNames = rects.map { it.className }
                rects[0].rect.left
            }

            drawPredictionsOnBitmap(imageBitmap, rects)
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

    private fun drawPredictionsOnBitmap(
        imageBitmap: Bitmap,
        rects: ArrayList<DetectionResult>
    ) {
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
    }


    private fun loadModel() {
        // Pytorch model loading
        modulePt = LiteModuleLoader.load(assetFilePath(globalContext, modelNamePt))

        // TF cpu model loading
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

        // TF gpu model loading
        val compatList = CompatibilityList()

        val options = Interpreter.Options().apply{
            if(compatList.isDelegateSupportedOnThisDevice){
                // if the device has a supported GPU, add the GPU delegate
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegateOptions))
                Log.d("YOLOGPU", "GPU OK ✅ ✅")
            } else {
                // if the GPU is not supported, run on 4 threads
                Log.d("YOLOGPU", "GPU NOT OK")
                this.numThreads = 4
            }
        }

        moduleTfGpu = Interpreter(File(assetFilePath(globalContext, modelNameTf)), options)

        loadOnnxModuleNative(
            assetFilePath(globalContext, modelNameOnnx),
            assetFilePath(globalContext, "classes.txt"),
            YoloV5PrePostProcessor.confidenceThreshold,
            YoloV5PrePostProcessor.iouThreshold
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
        loadModel()
        classes = loadClasses(globalContext)
    }

    fun destroy() {
        cameraExecutor.shutdown()
    }

    private external fun loadOnnxModuleNative(
        modulePath: String,
        classNamesPath: String,
        confidenceThreshold: Float,
        iouThreshold: Float
    )

    private external fun predictNativeOnnx(imgBitmap: Bitmap): Bitmap

}