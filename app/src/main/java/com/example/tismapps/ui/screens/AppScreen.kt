package com.example.tismapps.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tismapps.NavigationStuff
import com.example.tismapps.R
import com.example.tismapps.ui.data.MenuItem
import com.example.tismapps.ui.data.menuItemsList
import com.example.tismapps.navigateToScreen
import com.example.tismapps.ui.data.AppScreensRoutes
import com.example.tismapps.ui.data.DetectorViewModel
import com.example.tismapps.ui.screens.classifier.ClassifierScreen
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
    onNavigationClicked: () -> Unit,
    currentScreenName: String
) {
    TopAppBar(
        title = {
            Text(
                text = currentScreenName,
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
    val backStack by navController.currentBackStackEntryAsState()
    var currentScreenName = stringResource(id = R.string.app_title)
    if (backStack?.destination?.route != null)
        currentScreenName = backStack!!.destination.route.toString()

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
                },
                currentScreenName=currentScreenName
            )
        },
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
        drawerContent = {
            DrawerHeader()
            DrawerBody(
                items = menuItemsList,
                onItemClick = {
                    when (it.id) {
                        "home" -> navigateToScreen(
                            navStuff,
                            AppScreensRoutes.Home
                        )
                        "pastai" -> navigateToScreen(
                            navStuff,
                            AppScreensRoutes.Detector
                        )
                        else -> navigateToScreen(
                            navStuff,
                            AppScreensRoutes.Classifier
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreensRoutes.Home.screenName,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = AppScreensRoutes.Home.screenName) {
                HomeScreen(navStuff)
            }
            composable(route = AppScreensRoutes.Detector.screenName) {
                DetectorScreen(
                    detectorViewModel =  detectorViewModel,
                    navStuff = navStuff,
                    onExitButtonClicked = {
                        navigateToScreen(
                            navStuff,
                            AppScreensRoutes.Home
                        )
                    }
                )
            }
            composable(route = AppScreensRoutes.Classifier.screenName) {
                ClassifierScreen(
                    navStuff = navStuff,
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