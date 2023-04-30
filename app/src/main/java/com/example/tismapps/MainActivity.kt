package com.example.tismapps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tismapps.ui.camera.CameraViewModel
import com.example.tismapps.ui.app.App
import com.example.tismapps.ui.theme.TismappsTheme

class MainActivity : ComponentActivity() {
    private lateinit var cameraViewModel : CameraViewModel
    private var shouldShowCamera = mutableStateOf(false)
    private var shouldShowResult = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        shouldShowCamera.value = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
       // requestCameraPermission()
        super.onCreate(savedInstanceState)
        setContent {
            cameraViewModel = viewModel()
            TismappsTheme {
                App(
                    cameraViewModel,
                    onCaptureButtonClicked = {
                        cameraViewModel.handleImageCapture(it, this)
                        shouldShowCamera.value = false
                        shouldShowResult.value = true
                    }
                )
            }
        }
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("TISMAPPS", "Camera permission granted")
                shouldShowCamera.value = true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.CAMERA
            ) -> {
                Log.i("TISMAPPS", "Showing camera permission dialog")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)

            }

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraViewModel.destroy()
    }
}
