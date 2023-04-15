package com.example.tismapps.ui.camera

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tismapps.MainActivity
import java.io.File


@Composable
fun CameraScreen(
    context: MainActivity,
    outputDirectory: File,
    navigateToResultScreen: () -> Unit,
    /*navController: NavHostController,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
     */
    cameraViewModel: CameraViewModel = viewModel(LocalContext.current as ComponentActivity)

) {
    cameraViewModel.loadModel(context)

    CameraView(
        outputDirectory = outputDirectory,
        executor = context.cameraExecutor,
        onImageCaptured = {
            cameraViewModel.handleImageCapture(it)
            context.runOnUiThread {
                navigateToResultScreen()
            }
        },
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


