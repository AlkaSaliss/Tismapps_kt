package com.example.tismapps.ui.app

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
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
    cameraViewModel: CameraViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val cameraUiState by cameraViewModel.cameraUiState.collectAsState()
    Log.d("SOMETHING_LOGS_V3", cameraUiState.className)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberImagePainter(cameraUiState.photoUri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.75f)
        )
        Text(text = cameraUiState.className)
    }
}
