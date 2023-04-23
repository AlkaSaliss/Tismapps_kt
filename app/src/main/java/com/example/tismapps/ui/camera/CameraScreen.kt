package com.example.tismapps.ui.camera

import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext


@Composable
fun CameraScreen(
    cameraViewModel: CameraViewModel,
    onCaptureButtonClicked: (Uri) -> Unit

    ) {
    val activity = LocalContext.current as ComponentActivity

    cameraViewModel.initializeCameraStuff(activity)
    cameraViewModel.initializeCameraExecutor()
    CameraView(
        outputDirectory = cameraViewModel.outputDirectory,
        executor = cameraViewModel.cameraExecutor,
        onImageCaptured = onCaptureButtonClicked
    ) {
        Log.e("TISMAPPS", "View error", it)
    }
}




