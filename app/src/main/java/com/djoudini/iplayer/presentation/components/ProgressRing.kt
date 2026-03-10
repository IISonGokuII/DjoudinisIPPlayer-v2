package com.djoudini.iplayer.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated circular progress ring for dashboard tiles.
 *
 * @param progress 0.0f to 1.0f for determinate, -1f for indeterminate spinning.
 * @param size Diameter of the ring.
 * @param strokeWidth Width of the arc.
 * @param trackColor Background track color.
 * @param progressColor Foreground progress color.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
) {
    val isIndeterminate = progress < 0f

    // Smooth animation for determinate progress
    val animatedProgress by animateFloatAsState(
        targetValue = if (isIndeterminate) 0f else progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progress",
    )

    // Spinning animation for indeterminate state
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val innerSize = Size(
            this.size.width - strokeWidth.toPx(),
            this.size.height - strokeWidth.toPx()
        )
        val topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2)

        // Track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = innerSize,
            style = stroke,
        )

        if (isIndeterminate) {
            // Spinning arc
            drawArc(
                color = progressColor,
                startAngle = rotation - 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = topLeft,
                size = innerSize,
                style = stroke,
            )
        } else {
            // Determinate arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = innerSize,
                style = stroke,
            )
        }
    }
}
