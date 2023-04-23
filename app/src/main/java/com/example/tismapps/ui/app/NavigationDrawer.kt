package com.example.tismapps.ui.app

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tismapps.R
import com.example.tismapps.ui.data.MenuItem
import com.example.tismapps.ui.data.menuItemsList
import com.example.tismapps.navigateToScreen
import com.example.tismapps.ui.camera.CameraScreen
import com.example.tismapps.ui.camera.CameraViewModel
import kotlinx.coroutines.launch

@Composable
fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Header", fontSize = 60.sp)
    }
}

@Composable
fun DrawerBody(
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
    itemTextStyle: TextStyle = TextStyle(fontSize = 18.sp),
    onItemClick: (MenuItem) -> Unit
) {
    LazyColumn(modifier = modifier) {
        items(items) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onItemClick(item)
                    }
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.contentDescription
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = item.title,
                    style = itemTextStyle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AppBar(
    onNavigationClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.app_name))
        },
        backgroundColor = MaterialTheme.colors.background,
        contentColor = MaterialTheme.colors.primary,
        navigationIcon = {
            IconButton(onClick = onNavigationClicked) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Toggle Drawer"
                )
            }
        }
    )
}

@Composable
fun App(
    cameraViewModel: CameraViewModel,
    onCaptureButtonClicked: (Uri) -> Unit
) {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            AppBar(
                onNavigationClicked = {
                    scope.launch {
                        scaffoldState.drawerState.open()
                    }
                }
            )
        },
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
        drawerContent = {
            DrawerHeader()
            DrawerBody(
                items = menuItemsList,
                onItemClick = {
                    when (it.title) {
                        "Home" -> navigateToScreen(
                            navController,
                            AppScreensRoutes.Home,
                            scaffoldState,
                            scope
                        )
                        "Settings" -> navigateToScreen(
                            navController,
                            AppScreensRoutes.Settings,
                            scaffoldState,
                            scope
                        )
                        else -> navigateToScreen(
                            navController,
                            AppScreensRoutes.Help,
                            scaffoldState,
                            scope
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreensRoutes.Home.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = AppScreensRoutes.Home.name) {
                CameraScreen(
                    cameraViewModel,
                    onCaptureButtonClicked = {
                        onCaptureButtonClicked(it)
                        cameraViewModel.viewModelScope.launch {
                            Log.d("ALKA", "GOING  RESULT SCREEN")
                            navController.navigate(AppScreensRoutes.Result.name)
                        }
                    }
                )
            }
            composable(route = AppScreensRoutes.Result.name) {
                ResultScreen(cameraViewModel)
            }
            composable(route = AppScreensRoutes.Settings.name) {
                MenuScreen(screenName = AppScreensRoutes.Settings.name)
            }
            composable(route = AppScreensRoutes.Help.name) {
                MenuScreen(screenName = AppScreensRoutes.Help.name)
            }
        }
    }
}

