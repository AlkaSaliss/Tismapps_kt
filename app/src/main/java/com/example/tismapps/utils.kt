package com.example.tismapps

import android.content.Context
import android.graphics.Rect
import androidx.compose.material.ScaffoldState
import androidx.core.app.ComponentActivity
import androidx.navigation.NavHostController
import com.example.tismapps.ui.data.AppScreensRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.math.max
import kotlin.math.min

data class NavigationStuff(
    val navController: NavHostController,
    val scaffoldState: ScaffoldState,
    val scope: CoroutineScope
)

enum class DLFrameworks(val framework: String) {
    PYTORCH("gray"),
    TENSORFLOW("yellow")
}

fun navigateToScreen(
    navStuff: NavigationStuff,
    route: AppScreensRoutes,
) {
    navStuff.navController.navigate(route.name)
    navStuff.scope.launch {
        navStuff.scaffoldState.drawerState.close()
    }
}


fun assetFilePath(context: Context, assetName: String): String {
    val file = File(context.filesDir, assetName)
    if (file.exists() && file.length() > 0) {
        return file.absolutePath
    }
    context.assets.open(assetName).use { `is` ->
        FileOutputStream(file).use { os ->
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (`is`.read(buffer).also { read = it } != -1) {
                os.write(buffer, 0, read)
            }
            os.flush()
        }
        return file.absolutePath
    }
}

data class DetectionResult(var className: String, var score: Float, var rect: Rect) {
    override fun toString(): String {
        return "Prediction(Class: $className, Confidence: $score, Coords: $rect)Width:${rect.width()}, Height:${rect.height()}"
    }
}

object YoloV5PrePostProcessor {
    // for yolov5 model, no need to apply MEAN and STD
    var NO_MEAN_RGB = floatArrayOf(0.0f, 0.0f, 0.0f)
    var NO_STD_RGB = floatArrayOf(1.0f, 1.0f, 1.0f)

    // model input image size
    var mInputWidth = 640
    var mInputHeight = 640

    // model output is of size 4032*(num_of_class+5)
    const val mOutputRow =
        25200 // as decided by the YOLOv5 model for input image of size 640*640
    const val mOutputColumn = 85 // left, top, right, bottom, score and 80 class probabilities
    private const val mThreshold = 0.25f
    private const val mNmsLimit = 15
    // The two methods nonMaxSuppression and IOU below are ported from https://github.com/hollance/YOLO-CoreML-MPSNNGraph/blob/master/Common/Helpers.swift
    /**
     * Removes bounding boxes that overlap too much with other boxes that have
     * a higher score.
     * - Parameters:
     * - boxes: an array of bounding boxes and their scores
     * - limit: the maximum number of boxes that will be selected
     * - threshold: used to decide whether boxes overlap too much
     */
    private fun nonMaxSuppression(
        boxes: ArrayList<DetectionResult>,
    ): ArrayList<DetectionResult> {

        // Do an argsort on the confidence scores, from high to low.
        boxes.sortWith { o1, o2 -> o1.score.compareTo(o2.score) }
        val selected = ArrayList<DetectionResult>()
        val active = BooleanArray(boxes.size)
        Arrays.fill(active, true)
        var numActive = active.size

        // The algorithm is simple: Start with the box that has the highest score.
        // Remove any remaining boxes that overlap it more than the given threshold
        // amount. If there are any boxes left (i.e. these did not overlap with any
        // previous boxes), then repeat this procedure, until no more boxes remain
        // or the limit has been reached.
        var done = false
        var i = 0
        while (i < boxes.size && !done) {
            if (active[i]) {
                val boxA = boxes[i]
                selected.add(boxA)
                if (selected.size >= mNmsLimit) break
                for (j in i + 1 until boxes.size) {
                    if (active[j]) {
                        val boxB = boxes[j]
                        if (iOU(boxA.rect, boxB.rect) > mThreshold) {
                            active[j] = false
                            numActive -= 1
                            if (numActive <= 0) {
                                done = true
                                break
                            }
                        }
                    }
                }
            }
            i++
        }
        return selected
    }

    /**
     * Computes intersection-over-union overlap between two bounding boxes.
     */
    private fun iOU(a: Rect, b: Rect): Float {
        val areaA = ((a.right - a.left) * (a.bottom - a.top)).toFloat()
        if (areaA <= 0.0) return 0.0f
        val areaB = ((b.right - b.left) * (b.bottom - b.top)).toFloat()
        if (areaB <= 0.0) return 0.0f
        val intersectionMinX = max(a.left, b.left).toFloat()
        val intersectionMinY = max(a.top, b.top).toFloat()
        val intersectionMaxX = min(a.right, b.right).toFloat()
        val intersectionMaxY = min(a.bottom, b.bottom).toFloat()
        val intersectionArea = max(intersectionMaxY - intersectionMinY, 0f) *
                max(intersectionMaxX - intersectionMinX, 0f)
        return intersectionArea / (areaA + areaB - intersectionArea)
    }

    fun outputsToNMSPredictions(
        outputs: FloatArray,
        imgScaleX: Float,
        imgScaleY: Float,
        ivScaleX: Float,
        ivScaleY: Float,
        startX: Float,
        startY: Float,
        classes: MutableList<String>
    ): ArrayList<DetectionResult> {
        val results = ArrayList<DetectionResult>()
        for (i in 0 until mOutputRow) {
            if (outputs[i * mOutputColumn + 4] > mThreshold) {
                val x = outputs[i * mOutputColumn] * mInputWidth
                val y = outputs[i * mOutputColumn + 1] * mInputHeight
                val w = outputs[i * mOutputColumn + 2] * mInputWidth
                val h = outputs[i * mOutputColumn + 3] * mInputHeight
                val left = imgScaleX * (x - w / 2)
                val top = imgScaleY * (y - h / 2)
                val right = imgScaleX * (x + w / 2)
                val bottom = imgScaleY * (y + h / 2)
                var max = outputs[i * mOutputColumn + 5]
                var classIdx = 0
                for (j in 0 until mOutputColumn - 5) {
                    if (outputs[i * mOutputColumn + 5 + j] > max) {
                        max = outputs[i * mOutputColumn + 5 + j]
                        classIdx = j
                    }
                }
                val rect = Rect(
                    (startX + ivScaleX * left).toInt(),
                    (startY + top * ivScaleY).toInt(),
                    (startX + ivScaleX * right).toInt(),
                    (startY + ivScaleY * bottom).toInt()
                )
                val result = DetectionResult(
                    classes[classIdx],
                    outputs[i * mOutputColumn + 4], rect
                )
                results.add(result)
            }
        }
        return nonMaxSuppression(results)
    }
}


fun loadClasses(context: ComponentActivity): MutableList<String> {
    val br = BufferedReader(InputStreamReader(context.assets.open("classes.txt")))
    val classes: MutableList<String> = mutableListOf()
    while (true) {
        val line = br.readLine() ?: break
        classes.add(line)
    }
    return classes
}