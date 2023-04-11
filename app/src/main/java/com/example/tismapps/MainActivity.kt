package com.example.tismapps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.tismapps.ui.camera.CameraViewModel
import com.example.tismapps.ui.app.App
import com.example.tismapps.ui.theme.TismappsTheme

class MainActivity : ComponentActivity() {

    private val cameraViewModel = CameraViewModel()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraViewModel.updateCameraPermission(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TismappsTheme {
                App(
                    cameraViewModel,
                    requestPermissionLauncher
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraViewModel.destroy()
    }
}
