package com.example.tismapps.ui.screens.classifier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.tismapps.NavigationStuff
import com.example.tismapps.navigateToScreen
import com.example.tismapps.ui.data.AppScreensRoutes

@Composable
fun ClassifierScreen(
    navStuff: NavigationStuff
) {
    BackHandler {
        navigateToScreen(
            navStuff,
            AppScreensRoutes.Home
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Welcome Classifier Screen !!!", textAlign = TextAlign.Center)
    }
}
