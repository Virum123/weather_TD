package com.example.weathertd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = InkBlue,
    onPrimary = CloudWhite,
    secondary = CoralRed,
    tertiary = GoodGreen,
    background = CloudWhite,
    surface = CloudWhite,
    onSurface = InkBlue,
    onSurfaceVariant = MistGray,
)

@Composable
fun WeatherTDTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = WeatherTdTypography,
        content = content,
    )
}
