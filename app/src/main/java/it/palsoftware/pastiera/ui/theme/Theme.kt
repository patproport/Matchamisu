package it.palsoftware.pastiera.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import it.palsoftware.pastiera.SettingsManager

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB5C8D3),
    onPrimary = Color(0xFF14232C),
    secondary = Color(0xFFB5C8D3),
    background = Color(0xFF11181D),
    onBackground = Color(0xFFE5EBEE),
    surface = Color(0xFF202B32),
    onSurface = Color(0xFFE5EBEE),
    surfaceVariant = Color(0xFF28353D),
    onSurfaceVariant = Color(0xFFACBDC5),
    outline = Color(0xFF71858F),
    outlineVariant = Color(0xFF42545E)
)

private val MatchaColorScheme = darkColorScheme(
    primary = Color(0xFFA9C99A),
    onPrimary = Color(0xFF121A15),
    secondary = Color(0xFFA9C99A),
    onSecondary = Color(0xFF121A15),
    tertiary = Color(0xFFA9C99A),
    onTertiary = Color(0xFF121A15),
    background = Color(0xFF121A15),
    onBackground = Color(0xFFF1F4E8),
    surface = Color(0xFF203027),
    onSurface = Color(0xFFF1F4E8),
    surfaceDim = Color(0xFF121A15),
    surfaceBright = Color(0xFF203027),
    surfaceContainerLowest = Color(0xFF121A15),
    surfaceContainerLow = Color(0xFF203027),
    surfaceContainer = Color(0xFF203027),
    surfaceContainerHigh = Color(0xFF203027),
    surfaceContainerHighest = Color(0xFF203027),
    surfaceVariant = Color(0xFF203027),
    onSurfaceVariant = Color(0xFF92A897),
    outline = Color(0xFFA9C99A),
    outlineVariant = Color(0xFF314136)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF176D78),
    onPrimary = Color.White,
    background = Color(0xFFF4F7F7),
    onBackground = Color(0xFF19272A),
    surface = Color.White,
    onSurface = Color(0xFF19272A),
    surfaceVariant = Color(0xFFE9EFF0),
    onSurfaceVariant = Color(0xFF53676C),
    outlineVariant = Color(0xFFCDDADC)
)

private val Q25Shapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp)
)

private tailrec fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}

@Composable
fun rememberQ25ThemeMode(): String? {
    val context = LocalContext.current
    val preferences = remember(context) { SettingsManager.getPreferences(context) }
    var mode by remember(context) { mutableStateOf(SettingsManager.getQ25ThemeMode(context)) }
    DisposableEffect(context, preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SettingsManager.KEY_Q25_THEME_MODE) {
                mode = SettingsManager.getQ25ThemeMode(context)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return mode
}

@Composable
fun PastieraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: String? = null,
    typography: androidx.compose.material3.Typography = Typography,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        SettingsManager.Q25_THEME_LIGHT -> false
        SettingsManager.Q25_THEME_DARK, SettingsManager.Q25_THEME_MATCHA -> true
        else -> darkTheme
    }
    val context = LocalContext.current
    val view = LocalView.current
    SideEffect {
        context.hostActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }
    val colorScheme = when {
        themeMode == SettingsManager.Q25_THEME_MATCHA -> MatchaColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = Q25Shapes,
        content = content
    )
}
