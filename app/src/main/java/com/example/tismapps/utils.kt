package com.example.tismapps

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.navigation.NavHostController
import com.example.tismapps.ui.data.AppScreensRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


data class NavigationStuff(
    val navController: NavHostController,
    val scaffoldState: ScaffoldState,
    val scope: CoroutineScope
)

fun navigateToScreen(
    navStuff: NavigationStuff,
    route: AppScreensRoutes,
) {
    navStuff.navController.navigate(route.screenName)
    navStuff.scope.launch {
        navStuff.scaffoldState.drawerState.close()
    }
}


fun assetFilePath(context: Context, assetName: String): String {
    val file = File(context.filesDir, assetName)
    if (file.exists() && file.length() > 0) {
        return file.absolutePath
    }
    context.assets.open(assetName).use { `is` ->
        FileOutputStream(file).use { os ->
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (`is`.read(buffer).also { read = it } != -1) {
                os.write(buffer, 0, read)
            }
            os.flush()
        }
        return file.absolutePath
    }
}

object YoloV5PrePostProcessor {
    // model input image size
    var mInputWidth = 640
    var mInputHeight = 640
    const val iouThreshold = 0.45f
    const val confidenceThreshold = 0.25f
}
