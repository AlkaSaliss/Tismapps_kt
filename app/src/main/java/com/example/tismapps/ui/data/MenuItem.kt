package com.example.tismapps.ui.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreensRoutes{
    Home,
    DetectorPytorch,
    DetectorTensorflow
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
        id = "pytorch",
        title = "Pytorch",
        contentDescription = "Go to Pytorch YoloV5",
        icon = Icons.Outlined.ArrowCircleRight
    ),
    MenuItem(
        id = "tensorflow",
        title = "Tensorflow",
        contentDescription = "Go to Tensorflow YoloV5",
        icon = Icons.Outlined.ArrowCircleRight
    )
)
