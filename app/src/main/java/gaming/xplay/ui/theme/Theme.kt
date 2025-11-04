package gaming.xplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = VibrantRed,
    secondary = CyberpunkCyan,
    background = DarkBackground,
    surface = DarkBackground,
    onPrimary = LightText,
    onSecondary = DarkText,
    onBackground = LightText,
    onSurface = LightText
)

@Composable
fun XplayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
