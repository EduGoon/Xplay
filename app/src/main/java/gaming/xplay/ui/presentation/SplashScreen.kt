package gaming.xplay.ui.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SplashScreen() {
    LaunchedEffect(Unit) {
        delay(3000)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color.Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        FloatingBubbles()
        Text(
            text = "Xplay",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FloatingBubbles() {
    val density = LocalDensity.current
    val bubbleColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    val bubbles = remember {
        List(20) {
            Bubble(
                size = with(density) { Random.nextFloat().coerceIn(10f, 30f).toDp() },
                startPosition = Offset(
                    x = Random.nextFloat(),
                    y = Random.nextFloat()
                ),
                color = bubbleColors.random().copy(alpha = Random.nextFloat().coerceIn(0.1f, 0.5f)),
                duration = Random.nextInt(10000, 20000)
            )
        }
    }

    bubbles.forEach { bubble ->
        val animatableY = remember { Animatable(0f) }

        LaunchedEffect(bubble) {
            animatableY.animateTo(
                targetValue = -2f, // Move bubbles upwards
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = bubble.duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { translationY = animatableY.value * 1000 }) {
            drawCircle(
                color = bubble.color,
                radius = bubble.size.toPx(),
                center = Offset(
                    x = bubble.startPosition.x * size.width,
                    y = (bubble.startPosition.y + animatableY.value) * size.height
                )
            )
        }
    }
}

data class Bubble(
    val size: Dp,
    val startPosition: Offset,
    val color: Color,
    val duration: Int
)
