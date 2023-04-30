package com.example.tismapps.ui.app

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import com.example.tismapps.ui.camera.CameraViewModel
import kotlin.math.roundToInt


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

@OptIn(ExperimentalTextApi::class)
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

        Box(
            modifier = Modifier
                .size(cameraViewModel.imgWidth, cameraViewModel.imgHeight)
        ) {

            val w = LocalDensity.current.run { cameraViewModel.imgWidth.toPx() }
            val h = LocalDensity.current.run { cameraViewModel.imgHeight.toPx() }

            val img = cameraViewModel.imageBitmap
            val textMeasurer = rememberTextMeasurer()

            Canvas(modifier = Modifier.size(cameraViewModel.imgWidth, cameraViewModel.imgHeight), onDraw = {
                drawImage(
                    img.scale(w.toInt(), h.toInt())
                        .asImageBitmap()
                )
                for (bbox in cameraViewModel.rects) {
                    val resultOffset = Offset(bbox.rect.left.toFloat(), bbox.rect.top.toFloat())
                    drawRect(
                        color = Color.Magenta,
                        topLeft = resultOffset,
                        size = Size(bbox.rect.width().toFloat(), bbox.rect.height().toFloat()),
                        alpha = 0.3f
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "Score: ${(100*bbox.score).roundToInt()}%",
                        topLeft = resultOffset,
                        style = TextStyle(fontSize = 32.sp, color = Color.White)

                    )
                }

            })
        }
        Text(text = cameraUiState.className, modifier = Modifier.padding(top = 16.dp))
    }

}
