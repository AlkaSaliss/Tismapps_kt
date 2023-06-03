package com.example.tismapps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.tismapps.ui.theme.TismappsTheme

class MainActivity : ComponentActivity() {

    private var srcImg: Bitmap? = null
    private var dstImg: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        srcImg = BitmapFactory.decodeFile(assetFilePath(this, "test1.png"))
        dstImg = srcImg!!.copy(srcImg!!.config, true)

        setContent {

            TismappsTheme {
                AppScreen(
                    img=dstImg!!,
                    onClick={flipImg(dstImg!!)}
                )
            }
        }
    }

    private fun flipImg(img: Bitmap){
        myFlip(img, img)
        Log.d("OPENCV", "flipped")
    }
    companion object {
        init {
            System.loadLibrary("tismapps_cpp")
        }
    }

    external fun stringFromJNI(): String
    private external fun myFlip(bitmapIn: Bitmap, bitmapOut: Bitmap)
}


@Composable
fun AppScreen(
    img: Bitmap,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(bitmap = img.asImageBitmap(), contentDescription = "test1;png")

        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(
                text = "<Flip/>",
            )
        }
    }
}
