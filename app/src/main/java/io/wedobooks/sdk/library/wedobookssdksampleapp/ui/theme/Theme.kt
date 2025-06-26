package io.wedobooks.sdk.library.wedobookssdksampleapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun WeDoBooksSDKSampleAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

internal val lightPrimary = Color(0xFF4B2323)
internal val lightOnPrimary = Color(0xFFFFFFFF)
internal val lightSecondary = Color(0xFFF6E6E6)
internal val lightOnSecondary = Color(0xFF4B2323)
internal val lightBackground = Color(0xFFFFFFFF)
internal val lightBackgroundBright = Color(0xFFFFFFFF)
internal val lightOutline = Color(0xFFCCCCCC)
internal val lightTextColor = Color(0xFF222222)
internal val lightTextColorVar = Color(0xFF444444)

internal val darkPrimary = Color(0xFFF6E6E6)
internal val darkOnPrimary = Color(0xFF4B2323)
internal val darkSecondary = Color(0xFF4B2323)
internal val darkOnSecondary = Color(0xFFFFFFFF)
internal val darkBackground = Color(0xFF141218)
internal val darkBackgroundBright = Color(0xFF3B383E)
internal val darkOutline = Color(0xFF49454F)
internal val darkTextColor = Color(0xFFFFFFFF)
internal val darkTextColorVar = Color(0xFFCCCCCC)
