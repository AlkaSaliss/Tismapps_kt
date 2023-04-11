package com.example.tismapps.ui.camera

import android.Manifest
import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.example.tismapps.navigateToScreen
import com.example.tismapps.ui.app.AppScreensRoutes
import kotlinx.coroutines.CoroutineScope


@Composable
fun CameraScreen(
    cameraViewModel: CameraViewModel,
    requestPermissionLauncher: ActivityResultLauncher<String>,
    navController: NavHostController,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,

    ) {
    val activity = LocalContext.current as ComponentActivity
    val cameraUiState by cameraViewModel.cameraUiState.collectAsState()

    cameraViewModel.initializeCameraStuff(activity, requestPermissionLauncher)
    cameraViewModel.initializeCameraExecutor()

    if (!cameraUiState.shouldShowCamera)
        PermissionDeniedDialog(activity, requestPermissionLauncher)

    if (cameraUiState.shouldShowCamera)
        CameraView(
            outputDirectory = cameraViewModel.outputDirectory,
            executor = cameraViewModel.cameraExecutor,
            onImageCaptured = {
                cameraViewModel.handleImageCapture(it)
                navigateToScreen(navController, AppScreensRoutes.Result, scaffoldState, scope)
            } ,
        ) {
            Log.e("TISMAPPS", "View error", it)
        }

    /*if (cameraUiState.shouldShowPhoto)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = rememberImagePainter(cameraViewModel.photoUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.75f)
            )
            Text(text = cameraUiState.className)
        }*/
}


@Composable
private fun PermissionDeniedDialog(
    activity: ComponentActivity,
    requestPermissionLauncher: ActivityResultLauncher<String>,
    modifier: Modifier = Modifier
) {
    //val activity = (LocalContext.current as Activity)

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

