package com.example.tismapps.ui.camera

import android.net.Uri
import org.pytorch.Module
import java.io.File

data class CameraUiState(
    //val cameraPermissionGranted: Boolean = false,
    val shouldShowPhoto: Boolean = false,
    val className: String = "UNDEFINED",
    val outputDirectory: File? = null,
    val photoUri: Uri? = null,
    val module: Module? = null,
)
