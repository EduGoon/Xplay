package gaming.xplay.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class Graffiti(
    val background: Int,
    val overlay: Color
)

val LocalGraffiti = staticCompositionLocalOf<Graffiti> {
    error("No Graffiti provided")
}