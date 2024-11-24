package com.vismo.nextgenmeter.ui.shared

import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

@Deprecated("Use GlobalDialog instead - this is for TD approval only")
@Composable
fun NxGnMeterDialog(
    showDialog: MutableState<Boolean>,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
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
    }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        // Set the dialog's window background to transparent
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(view.context, android.R.color.transparent)))
        }

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
            NxGnMeterDialogUI(
                content = content,
                isBlinking = isBlinking
            )
        }
    }
}

@Composable
private fun NxGnMeterDialogUI(
    content: @Composable () -> Unit,
    isBlinking: Boolean = false // Optional blinking effect
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
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Transparent)
                .size(width = 400.dp, height = 200.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // show bordered box with content
            Box(
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(16.dp)
                    .fillMaxSize()
                    .border(1.dp, Color.White, RoundedCornerShape(10.dp))
            ) {
                content() // Using the passed composable content
            }
        }
    }
}

@Composable
fun NxGnDialogContent(
    message: String,
    textColor: Color = Color.White,
) {
    Column(
        Modifier
            .background(Color.Transparent)
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = message,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 24.dp),
            style = MaterialTheme.typography.displayMedium,
            color = textColor,
        )
    }
}
