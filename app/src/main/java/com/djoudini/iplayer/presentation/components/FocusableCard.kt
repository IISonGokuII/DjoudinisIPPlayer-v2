package com.djoudini.iplayer.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

/**
 * Card that responds to D-Pad focus with visual feedback (scale + border glow).
 * Critical for Fire TV navigation.
 *
 * @param onClick Action on DPAD_CENTER / Enter key press.
 * @param focusScale Scale factor when focused (1.05 = 5% larger).
 * @param focusBorderColor Border color when focused.
 */
@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusScale: Float = 1.05f,
    focusBorderColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusScale else 1f,
        animationSpec = tween(150),
        label = "focusScale",
    )

    val borderStroke = if (isFocused) {
        BorderStroke(5.dp, focusBorderColor)
    } else {
        null
    }

    val shadowElevation by animateFloatAsState(
        targetValue = if (isFocused) 16.dp.value else 2.dp.value,
        animationSpec = tween(150),
        label = "shadowElevation",
    )

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = shadowElevation.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isFocused) focusBorderColor else Color.Transparent,
                ambientColor = if (isFocused) focusBorderColor else Color.Transparent,
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        border = borderStroke,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 16.dp else 2.dp,
        ),
    ) {
        Box(content = content)
    }
}
