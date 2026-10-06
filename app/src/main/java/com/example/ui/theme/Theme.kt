package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ChannelACyan,
    onPrimary = Color.Black,
    primaryContainer = ChannelACyanDark,
    onPrimaryContainer = Color.White,
    secondary = ChannelBAmber,
    onSecondary = Color.Black,
    secondaryContainer = ChannelBAmberDark,
    onSecondaryContainer = Color.White,
    tertiary = HoregCrimson,
    onTertiary = Color.White,
    background = RackDarkBackground,
    onBackground = TextSilver,
    surface = RackSurface,
    onSurface = TextSilver,
    surfaceVariant = RackSurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = RackBorder,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
