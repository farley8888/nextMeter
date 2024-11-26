package com.vismo.nextgenmeter.ui.shared

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FlippableCard(
    isFlipped: Boolean,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    rotationDuration: Int = 600
) {
    // Animation state for rotation
    val transition = updateTransition(targetState = isFlipped, label = "Flip Transition")

    val rotation by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = rotationDuration, easing = FastOutSlowInEasing)
        },
        label = "Rotation Animation"
    ) { flipped ->
        if (flipped) 180f else 0f
    }

    // Determine if the back should be visible based on rotation
    val isBackVisible = rotation > 90f && rotation < 270f

    // Adjust the camera distance for a better 3D effect
    val cameraDistance = 8 * LocalDensity.current.density

    Box(
        modifier = modifier
            .size(240.dp)
            .graphicsLayer {
                rotationY = rotation
                this.cameraDistance = cameraDistance
                // Removed alpha modification
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)

    ) {
        // Front Side
        if (!isBackVisible) {
            frontContent()
        }

        // Back Side with reversed rotation to face the user
        if (isBackVisible) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        rotationY = 180f
                        this.cameraDistance = cameraDistance
                    }
                    .fillMaxSize()
            ) {
                backContent()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FlippableCardPreview() {
    var isFlipped by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .size(240.dp)
            .padding(16.dp)
            .clickable { isFlipped = !isFlipped }
    ) {
        FlippableCard(
            isFlipped = isFlipped,
            frontContent = {
                // Front Side
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Blue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Front", color = Color.White)
                }
            },
            backContent = {
                // Back Side
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Back", color = Color.White)
                }
            }
        )
    }
}

