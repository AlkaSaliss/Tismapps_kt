package com.example.tismapps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


/*
private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)
*/


/*private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

 */

private val DarkColorPalette = darkColors(
    primary = Color(0xff66ffd9),
    onPrimary = Color(0xFF003326),
    primaryVariant = Color(0xFF003300),
    secondary = Color(0xffa3c2c2),
    onSecondary = Color(0xff1a3300),
    surface = Color(0xff66ffd9),
    onSurface = Color(0xFF003326)
)
private val LightColorPalette = lightColors()

@Composable
fun TismappsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme)  DarkColorPalette else LightColorPalette

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}