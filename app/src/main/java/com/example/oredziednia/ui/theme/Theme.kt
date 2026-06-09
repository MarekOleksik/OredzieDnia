package com.example.oredziednia.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DeepIndigoLight,
    onPrimary = Color.White,
    secondary = DeepIndigoLight,
    background = SkyBlueDark,
    surface = SurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = Color.White,
    secondary = DeepIndigoLight,
    background = SkyBlue,
    surface = Color.White,
    onBackground = DeepIndigo,
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun OredzieDniaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Wyłączone domyślnie: aplikacja ma własną tożsamość kolorystyczną (granat/błękit),
    // którą dynamiczne kolory systemowe (Android 12+, oparte na tapecie) by przesłoniły.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}