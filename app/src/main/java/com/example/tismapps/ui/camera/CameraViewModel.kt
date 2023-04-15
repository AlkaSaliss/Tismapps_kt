package com.example.tismapps.ui.camera

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tismapps.MainActivity
import com.example.tismapps.R
import com.example.tismapps.assetFilePath
import com.example.tismapps.ui.data.IMAGENET_CLASSES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.MemoryFormat
import org.pytorch.torchvision.TensorImageUtils
import java.io.File


class CameraViewModel : ViewModel() {
    private val _cameraUiState = MutableStateFlow(CameraUiState())
    val cameraUiState: StateFlow<CameraUiState> = _cameraUiState.asStateFlow()


    /*fun updateCameraPermission(value: Boolean) {
        _cameraUiState.update { currentState ->
            currentState.copy(cameraPermissionGranted = value)
        }
    }*/

    fun handleImageCapture(uri: Uri) {
        _cameraUiState.update { currentState ->
            currentState.copy(
                photoUri = uri
            )
        }
        classify()
        _cameraUiState.update { currentState ->
            currentState.copy(
                shouldShowPhoto = true
            )
        }
    }

    fun loadModel(context: MainActivity) {
        val module = LiteModuleLoader.load(assetFilePath(context, "model.ptl"))
        _cameraUiState.update { currentState ->
            currentState.copy(module = module)
        }
    }

    private fun classify() {
        if (
            _cameraUiState.value.photoUri != null &&
            _cameraUiState.value.module != null
        ) {
            Log.d("SOMETHING_LOGS", "making predictions")
            val imageBitmap = BitmapFactory.decodeFile(_cameraUiState.value.photoUri!!.path)
            val imgTensor = TensorImageUtils.bitmapToFloat32Tensor(
                imageBitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                MemoryFormat.CHANNELS_LAST
            )

            val scores = _cameraUiState.value.module!!
                .forward(IValue.from(imgTensor))
                .toTensor()
                .dataAsFloatArray
            var maxScore = -Float.MAX_VALUE
            var maxScoreIdx = -1
            for (i in scores.indices) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i]
                    maxScoreIdx = i
                }
            }
            _cameraUiState.update { currentState ->
                currentState.copy(className = IMAGENET_CLASSES[maxScoreIdx])
            }
            Log.d("SOMETHING_LOGS_V2", _cameraUiState.value.className)
        } else
            Log.e("TISMAPPS", "Unable to make prediction")
    }



    /*fun initializeCameraExecutor(mainActivity: MainActivity) {
        mainActivity.initCameraExecutor(Executors.newSingleThreadExecutor())
    }*/
}