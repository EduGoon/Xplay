package gaming.xplay.presentation.ui.State

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

data class Bubble(
    val size: Dp,
    val startPosition: Offset,
    val color: Color,
    val duration: Int
)
