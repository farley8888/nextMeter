package com.vismo.cablemeter.ui.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vismo.cablemeter.ui.theme.Black
import com.vismo.cablemeter.ui.theme.Purple40
import com.vismo.cablemeter.ui.theme.Purple80
import com.vismo.cablemeter.ui.theme.mineShaft700
import kotlinx.coroutines.delay

@Composable
fun GlobalDialog(
    title: String,
    message: String,
    iconResId: Any,
    actions: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit,
    showDialog: MutableState<Boolean>,
    backgroundColor: Color,
    isBlinking: Boolean = false, // Optional blinking effect
    shouldAutoDismissAfter: Long = 0 // Optional auto dismiss after x milliseconds
) {
    if (!showDialog.value) return
    // Auto dismiss after x milliseconds
    if (shouldAutoDismissAfter > 0) {
        LaunchedEffect(Unit) {
            delay(shouldAutoDismissAfter)
            showDialog.value = false
            onDismiss()
        }
    }
    Dialog(onDismissRequest = {
        showDialog.value = false
        onDismiss()
    }) {
        AnimatedVisibility(
            visible = showDialog.value,
            enter = fadeIn(animationSpec = tween(700)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(500)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        ) {
            GlobalDialogUI(
                title = title,
                message = message,
                iconResId = iconResId,
                actions = actions,
                onDismiss = {
                    showDialog.value = false
                    onDismiss()
                },
                isBlinking = isBlinking,
                backgroundColor = backgroundColor
            )
        }
    }
}

@Composable
private fun GlobalDialogUI(
    title: String,
    message: String,
    iconResId: Any,
    actions: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit,
    isBlinking: Boolean = false, // Optional blinking effect
    backgroundColor: Color
) {
    var isVisible by remember { mutableStateOf(true) }
    if (isBlinking) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                isVisible = !isVisible
            }
        }
    }

    BlinkingVisibility(isVisible = isVisible || !isBlinking) {
        Card(
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(10.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                Modifier
                    .background(backgroundColor)
                    .padding(16.dp)
            ) {
                Image(
                    painter = when (iconResId) {
                        is Int -> painterResource(id = iconResId)
                        is androidx.compose.ui.graphics.vector.ImageVector -> rememberVectorPainter(
                            image = iconResId
                        )

                        else -> throw IllegalArgumentException("Unsupported icon type")
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Purple40),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .height(70.dp)
                        .fillMaxWidth()
                )

                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Black
                )

                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 24.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = mineShaft700,
                )

                if (actions.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Purple80),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        actions.forEach { (label, action) ->
                            TextButton(onClick = {
                                action()
                                onDismiss()
                            }) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    color = Black,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlinkingVisibility(
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500)),
        exit = fadeOut(animationSpec = tween(durationMillis = 500))
    ) {
        content()
    }
}