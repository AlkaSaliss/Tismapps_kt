package com.example.tismapps.ui.screens.detector

import android.content.Context
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.tismapps.NavigationStuff
import com.example.tismapps.navigateToScreen
import com.example.tismapps.ui.data.AppScreensRoutes
import com.example.tismapps.ui.data.DetectorViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun DetectorScreen (
    detectorViewModel: DetectorViewModel,
    navStuff: NavigationStuff,
    onExitButtonClicked: () -> Unit = { println("Exiting") },
){
    BackHandler {
        navigateToScreen(
            navStuff,
            AppScreensRoutes.Home
        )
    }
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifeCycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()

    val imgView = ImageView(context)

    val imageAnalyzer = ImageAnalysis
        .Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(detectorViewModel.cameraExecutor, YoloDetector(detectorViewModel, imgView))
        }
    val previewView = remember { PreviewView(context)}

    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifeCycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView({previewView}, modifier = Modifier.size(0.dp, 0.dp))
        AndroidView({imgView}, modifier = Modifier.fillMaxSize())

        IconButton(
            modifier = Modifier.padding(bottom = 20.dp),
            onClick = onExitButtonClicked,
            content = {
                Icon(
                    imageVector = Icons.Outlined.StopCircle,
                    contentDescription = "Stop capture",
                    tint = Color(0xffdb544f),
                    modifier = Modifier
                        .size(75.dp)
                        .padding(1.dp)
                )
            }
        )
    }
}

@ExperimentalGetImage class YoloDetector(private val detectorViewModel: DetectorViewModel, private val imgView: ImageView) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val imgBitmap = detectorViewModel.detect(imageProxy)
        imgView.setImageBitmap(imgBitmap)
        imageProxy.close()
    }

}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also {cameraProvider ->
        cameraProvider.addListener({
            continuation.resume(cameraProvider.get())
        }, ContextCompat.getMainExecutor(this))
    }
}