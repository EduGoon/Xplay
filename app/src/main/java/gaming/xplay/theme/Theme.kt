package gaming.xplay.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import gaming.xplay.R
import gaming.xplay.presentation.theme.Graffiti
import gaming.xplay.presentation.theme.LocalGraffiti

// ============ DARK THEME ============
private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = Color(0xFF00171D),
    primaryContainer = NeonBlueDim,
    onPrimaryContainer = TextPrimaryDark,

    secondary = UltraViolet,
    onSecondary = Color(0xFF120022),
    secondaryContainer = UltraVioletDim,
    onSecondaryContainer = TextPrimaryDark,

    tertiary = CrimsonPulse,
    onTertiary = Color(0xFF190006),
    tertiaryContainer = CrimsonPulseDim,
    onTertiaryContainer = TextPrimaryDark,

    error = FlameOrange,
    onError = Color.Black,
    errorContainer = FlameOrangeDim,
    onErrorContainer = TextPrimaryDark,

    background = DarkBackground,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,

    outline = BorderDark,
    outlineVariant = DividerDark,

    surfaceTint = NeonBlue,
    scrim = Color(0xFF000000),
)

// ============ LIGHT THEME ============
private val LightColorScheme = lightColorScheme(
    primary = NeonBlue,
    onPrimary = TextPrimaryLight,
    primaryContainer = NeonBlueDim,
    onPrimaryContainer = TextPrimaryLight,

    secondary = UltraViolet,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = UltraVioletDim,
    onSecondaryContainer = TextPrimaryLight,

    tertiary = CrimsonPulse,
    onTertiary = TextPrimaryLight,
    tertiaryContainer = CrimsonPulseDim,
    onTertiaryContainer = TextPrimaryLight,

    error = FlameOrange,
    onError = Color(0xFFFFFFFF),
    errorContainer = FlameOrangeDim,
    onErrorContainer = TextPrimaryLight,

    background = LightBackground,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,

    outline = BorderLight,
    outlineVariant = DividerLight,

    surfaceTint = NeonBlue,
    scrim = Color(0xFF000000),
)

private val LightGraffiti = Graffiti(
    background = R.drawable.graffitilightmode,
    overlay = Color.Black.copy(alpha = 0.6f)
)

private val DarkGraffiti = Graffiti(
    background = R.drawable.graffitidarkmode,
    overlay = Color.Black.copy(alpha = 0.6f)
)

@Composable
fun XplayTheme(
    darkTheme: Boolean,
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

    val graffiti = if (darkTheme) DarkGraffiti else LightGraffiti

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set the status bar color to a gradient for dark theme, or a solid color for light theme
            window.statusBarColor = if (darkTheme) {
                // Create a gradient drawable or use a library to apply a gradient to the status bar
                // For simplicity, we'll use the start color of the gradient
                DarkGradientStart.toArgb()
            } else {
                colorScheme.background.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalGraffiti provides graffiti) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
