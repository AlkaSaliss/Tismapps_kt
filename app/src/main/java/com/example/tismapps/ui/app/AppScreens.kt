package com.example.tismapps.ui.app

import android.Manifest
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import coil.compose.rememberImagePainter
import com.example.tismapps.ui.camera.CameraViewModel


enum class AppScreensRoutes{
    Home,
    Result,
    Settings,
    Help
}


@Composable
fun MenuScreen(screenName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Welcome $screenName !!!", textAlign = TextAlign.Center)
    }
}

@Composable
fun ResultScreen(
    cameraViewModel: CameraViewModel
) {
    val cameraUiState by cameraViewModel.cameraUiState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Log.d(
            "ALKA",
            "RESULT SCREEN ${cameraUiState.className}, ${cameraViewModel.photoUri}, ${cameraViewModel.outputDirectory}"
        )
        Image(
            painter = rememberImagePainter(cameraViewModel.photoUri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.75f)
        )
        Text(text = cameraUiState.className)
    }

}


@Composable
fun PermissionDeniedDialog(
    activity: ComponentActivity,
    requestPermissionLauncher: ActivityResultLauncher<String>,
    modifier: Modifier = Modifier
) {

    AlertDialog(
        onDismissRequest = {
            activity.finish()
        },
        title = { Text("Camera Permission") },
        text = { Text("Camera Permission not granted. Exiting") },
        modifier = modifier,
        dismissButton = {
            TextButton(
                onClick = {
                    activity.finish()
                }
            ) {
                Text(text = "Exit")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            ) {
                Text(text = "Request Permission")
            }
        }
    )
}
