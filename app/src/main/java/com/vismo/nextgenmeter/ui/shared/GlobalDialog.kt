package com.vismo.nextgenmeter.ui.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.ui.theme.nobel100
import com.vismo.nextgenmeter.ui.theme.nobel50
import com.vismo.nextgenmeter.ui.theme.nobel800
import kotlinx.coroutines.delay

@Composable
fun GlobalDialog(
    showDialog: MutableState<Boolean>,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
    isBlinking: Boolean = false, // Optional blinking effect
    shouldAutoDismissAfter: Long = 0, // Optional auto dismiss after x milliseconds
    usePlatformDefaultWidth: Boolean = true,
    width: Int? = null,
    height: Int? = null,
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
    }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = usePlatformDefaultWidth)) {
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
                content = content,
                isBlinking = isBlinking,
                usePlatformDefaultWidth = usePlatformDefaultWidth,
                width = width,
                height = height
            )
        }
    }
}

@Composable
private fun GlobalDialogUI(
    content: @Composable () -> Unit,
    isBlinking: Boolean = false, // Optional blinking effect
    usePlatformDefaultWidth: Boolean = true,
    width: Int? = null,
    height: Int? = null,
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
        val modifier = if (!usePlatformDefaultWidth && width != null && height != null) {
            Modifier.size(width.dp, height.dp)
        } else {
            Modifier
        }
        Card(
            shape = RoundedCornerShape(10.dp),
            modifier = modifier,
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            content() // Using the passed composable content
        }
    }
}

@Composable
fun BlinkingVisibility(
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))
    ) {
        content()
    }
}

@Composable
fun GenericDialogContent(
    title: String,
    message: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    cancelButtonText: String? = null, // Optional cancel button text
    onCancel: (() -> Unit)? = null,  // Optional cancel button action
    onDismiss: () -> Unit = {},
    confirmButtonColor : Color = nobel800,
    cancelButtonColor : Color = nobel800
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            text = title,
            color = nobel100,
            style = Typography.headlineSmall,
            textAlign = TextAlign.Left,
        )
        Text(
            text = message,
            color = nobel100,
            style = Typography.bodyLarge,
            textAlign = TextAlign.Left,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmButtonColor,
                    contentColor = nobel50
                ),
            ) {
                Text(
                    text = confirmButtonText,
                    style = Typography.bodySmall
                )
            }
            // Render the cancel button only if cancelButtonText and onCancel are provided
            if (cancelButtonText != null && onCancel != null) {
                Button(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cancelButtonColor,
                        contentColor = nobel50
                    ),
                ) {
                    Text(
                        text = cancelButtonText,
                        style = Typography.bodySmall
                    )
                }
            }
        }
    }
}

