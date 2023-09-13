package com.example.tismapps.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tismapps.DLFrameworks
import com.example.tismapps.NavigationStuff
import com.example.tismapps.R
import com.example.tismapps.ui.data.MenuItem
import com.example.tismapps.ui.data.menuItemsList
import com.example.tismapps.navigateToScreen
import com.example.tismapps.ui.data.AppScreensRoutes
import com.example.tismapps.ui.data.DetectorViewModel
import com.example.tismapps.ui.screens.detector.DetectorScreen
import kotlinx.coroutines.launch

@Composable
fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .padding(start = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            fontSize = 24.sp,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DrawerBody(
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
    itemTextStyle: TextStyle = TextStyle(fontSize = 18.sp),
    onItemClick: (MenuItem) -> Unit
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(items) { item ->
            Column {
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
                        contentDescription = item.contentDescription,
                        tint = Color(0xff00b300)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = item.title,
                        style = itemTextStyle,
                        modifier = Modifier.weight(1f)
                    )
                }
                Divider(startIndent = 0.dp, thickness = 0.75.dp, color = Color(0xffb3daff))
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
            Text(
                text = stringResource(id = R.string.app_title),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        },
        backgroundColor = Color(0xff009999),
        contentColor = MaterialTheme.colors.primary,
        navigationIcon = {
            IconButton(onClick = onNavigationClicked) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Toggle Drawer",
                    tint = Color.White
                )
            }
        }
    )
}

@Composable
fun AppScreen(
    detectorViewModel: DetectorViewModel,
) {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    val navStuff = NavigationStuff(navController, scaffoldState, scope)

    Scaffold(
        drawerShape = NavShape(0.dp, 0.75f),
        scaffoldState = scaffoldState,
        drawerBackgroundColor = Color(0xffe6ffe6),
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
                    when (it.id) {
                        "pytorch" -> navigateToScreen(
                            navStuff,
                            AppScreensRoutes.DetectorPytorch
                        )
                        "tensorflow" -> navigateToScreen(
                            navStuff,
                            AppScreensRoutes.DetectorTensorflow
                        )
                        "tensorflow_gpu" -> navigateToScreen(
                            navStuff,
                            AppScreensRoutes.DetectorTensorflowGpu
                        )
                        "onnx" -> navigateToScreen(
                            navStuff,
                            AppScreensRoutes.DetectorOnnx
                        )
                        else -> navigateToScreen(
                            navStuff,
                            AppScreensRoutes.Home
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
                HomeScreen(navStuff)
            }
            composable(route = AppScreensRoutes.DetectorPytorch.name) {
                DetectorScreen(
                    detectorViewModel =  detectorViewModel,
                    framework = DLFrameworks.PYTORCH,
                    onExitButtonClicked = {
                        navigateToScreen(
                            navStuff,
                            AppScreensRoutes.Home
                        )
                    }
                )
            }
            composable(route = AppScreensRoutes.DetectorTensorflow.name) {
                DetectorScreen(
                    detectorViewModel =  detectorViewModel,
                    framework = DLFrameworks.TENSORFLOW,
                    onExitButtonClicked = {
                        navigateToScreen(
                            navStuff,
                            AppScreensRoutes.Home
                        )
                    }
                )
            }
            composable(route = AppScreensRoutes.DetectorTensorflowGpu.name) {
                DetectorScreen(
                    detectorViewModel =  detectorViewModel,
                    framework = DLFrameworks.TENSORFLOW_GPU,
                    onExitButtonClicked = {
                        navigateToScreen(
                            navStuff,
                            AppScreensRoutes.Home
                        )
                    }
                )
            }
            composable(route = AppScreensRoutes.DetectorOnnx.name) {
                DetectorScreen(
                    detectorViewModel =  detectorViewModel,
                    framework = DLFrameworks.ONNX,
                    onExitButtonClicked = {
                        navigateToScreen(
                            navStuff,
                            AppScreensRoutes.Home
                        )
                    }
                )
            }
        }
    }
}


class NavShape(
    private val widthOffset: Dp,
    private val scale: Float
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rectangle(
            Rect(
                Offset.Zero,
                Offset(
                    size.width * scale + with(density) { widthOffset.toPx() },
                    size.height
                )
            )
        )
    }
}