package io.wedobooks.sdk.library.wedobookssdksampleapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun WeDoBooksSdkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        MaterialTheme.colorScheme.copy(
            onPrimary = darkOnPrimary,
            primary = darkPrimary,
            onPrimaryContainer = darkOnPrimary,
            primaryContainer = darkPrimary,
            onSecondary = darkOnSecondary,
            secondary = darkSecondary,
            onSecondaryContainer = darkOnSecondary,
            secondaryContainer = darkSecondary,
            surfaceContainer = darkBackground,
            surfaceVariant = darkBackground,
            surfaceContainerLow = darkBackground,
            surfaceContainerHigh = darkBackground,
            surface = darkBackground,
            surfaceBright = darkBackgroundBright,
            onSurface = darkTextColor,
            onSurfaceVariant = darkTextColorVar,
            background = darkBackground,
            onBackground = darkTextColor,
            outline = darkOutline,
        )
    } else {
        MaterialTheme.colorScheme.copy(
            onPrimary = lightOnPrimary,
            primary = lightPrimary,
            onPrimaryContainer = lightOnPrimary,
            primaryContainer = lightPrimary,
            onSecondary = lightOnSecondary,
            secondary = lightSecondary,
            onSecondaryContainer = lightOnSecondary,
            secondaryContainer = lightSecondary,
            surfaceContainer = lightBackground,
            surfaceVariant = lightBackground,
            surfaceContainerLow = lightBackground,
            surfaceContainerHigh = lightBackground,
            surface = lightBackground,
            surfaceBright = lightBackgroundBright,
            onSurface = lightTextColor,
            onSurfaceVariant = lightTextColorVar,
            background = lightBackground,
            onBackground = lightTextColor,
            outline = lightOutline,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}