package com.example.tismapps

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.navigation.NavHostController
import com.example.tismapps.ui.app.AppScreensRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

fun navigateToScreen(
    navController: NavHostController,
    route: AppScreensRoutes,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope
) {
    navController.navigate(route.name)
    scope.launch {
        scaffoldState.drawerState.close()
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