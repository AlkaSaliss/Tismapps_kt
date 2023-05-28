package com.example.tismapps.ui.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreensRoutes{
    Home,
    Detector,
    Classifier
}

data class MenuItem(
    val id: String,
    val title: String,
    val contentDescription: String,
    val icon: ImageVector
)

val menuItemsList = listOf(
    MenuItem(
        id = "home",
        title = "Home",
        contentDescription = "Go to home Screen",
        icon = Icons.Outlined.Home
    ),
    MenuItem(
        id = "pastai",
        title = "PastAI",
        contentDescription = "Go to Pastai Screen",
        icon = Icons.Outlined.ArrowCircleRight
    ),
    MenuItem(
        id = "whichflower",
        title = "WhichFlower",
        contentDescription = "Go to Whichflower Scree,",
        icon = Icons.Outlined.ArrowCircleRight
    )
)
