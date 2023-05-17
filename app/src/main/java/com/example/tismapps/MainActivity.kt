package com.example.tismapps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tismapps.ui.camera.CameraViewModel
import com.example.tismapps.ui.app.App
import com.example.tismapps.ui.theme.TismappsTheme

class MainActivity : ComponentActivity() {
    private lateinit var cameraViewModel : CameraViewModel
    private var shouldShowCamera = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        shouldShowCamera.value = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            cameraViewModel = viewModel()
            cameraViewModel.initializeCameraStuff(this)
            val configuration = LocalConfiguration.current
            configuration.densityDpi
            cameraViewModel.screenWidth = configuration.screenWidthDp.dp
            cameraViewModel.screenHeight = configuration.screenHeightDp.dp
            //Log.d("DIMS", "${configuration.screenWidthDp.dp.value}, ${configuration.screenHeightDp.dp.value}")
            TismappsTheme {
                App(
                    cameraViewModel
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
