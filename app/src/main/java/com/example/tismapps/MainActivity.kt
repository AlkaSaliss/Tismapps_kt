package com.example.tismapps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tismapps.ui.app.App
import com.example.tismapps.ui.theme.TismappsTheme
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val context = this
    private var cameraPermissionGranted: MutableState<Boolean> = mutableStateOf(false)
    lateinit var cameraExecutor: ExecutorService
        private set
    private lateinit var outputDirectory: File

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { cameraPermissionGranted.value = it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TismappsTheme {
                if (cameraPermissionGranted.value) {
                    App(
                        context,
                        outputDirectory
                    )
                } else {
                    PermissionDeniedDialog(
                        this,
                        requestPermissionLauncher
                    )
                }
            }
        }

        requestCameraPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
        setOutputDirectory()
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                cameraPermissionGranted.value = true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.CAMERA
            ) -> {
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setOutputDirectory() {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        outputDirectory = if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}


@Composable
private fun PermissionDeniedDialog(
    context: MainActivity,
    requestPermissionLauncher: ActivityResultLauncher<String>,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {
            context.finish()
        },
        title = { Text("Camera Permission") },
        text = { Text("Camera Permission not granted. Exiting") },
        modifier = modifier,
        dismissButton = {
            TextButton(
                onClick = {
                    context.finish()
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
