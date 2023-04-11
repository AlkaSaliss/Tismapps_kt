package com.example.tismapps.ui.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class MenuItem(
    val id: String,
    val title: String,
    val contentDescription: String,
    val icon: ImageVector
)

val menuItemsList = listOf<MenuItem>(
    MenuItem(
        id = "home",
        title = "Home",
        contentDescription = "Go to home Screen",
        icon = Icons.Default.Home
    ),
    MenuItem(
        id = "settings",
        title = "Settings",
        contentDescription = "Go to settings Screen",
        icon = Icons.Default.Settings
    ),
    MenuItem(
        id = "help",
        title = "Help",
        contentDescription = "Go to help Screen",
        icon = Icons.Default.Info
    )
)
