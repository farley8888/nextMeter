package com.vismo.nextgenmeter.ui.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.nextgenmeter.ui.theme.Typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


data class ToastMessage(val message: String)


object GlobalToast {
    private val _toastMessages: SnapshotStateMap<ToastMessage, Boolean> = mutableStateMapOf()
    val toastMessages: Map<ToastMessage, Boolean> get() = _toastMessages

    fun show(message: String) {
        val toastMessage = ToastMessage(message)
        apply(toastMessage)

        // show
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(30)
            applyShow(toastMessage)

            // hide
            CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(3000)
                applyHide(toastMessage)

                // kill
                CoroutineScope(Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(500)
                    dismiss(toastMessage)
                }
            }
        }
    }

    private fun apply(toast: ToastMessage) {
        if (_toastMessages.containsKey(toast)) { return }
        applyHide(toast)
    }

    private fun applyShow(toast: ToastMessage) {
        _toastMessages[toast] = true
    }

    private fun applyHide(toast: ToastMessage) {
        _toastMessages[toast] = false
    }

    private fun dismiss(toast: ToastMessage) {
        _toastMessages.remove(toast)
    }
}


@Composable
fun GlobalToastHolder() {
    // Display all active toasts stacked
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter // Align to the top center
    ) {
        GlobalToast.toastMessages.forEach { (toastMessage, isVisible) ->
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            ) {
                GlobalToastUI(
                    message = toastMessage.message,
                )
            }
            // Here, we don't need to manage the dismissal anymore
        }
    }
}

@Composable
fun GlobalToastUI(message: String) {
    // Box to center the toast
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Toast message container
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(8.dp)
                .fillMaxWidth(0.75f)
                .fillMaxHeight(0.7f),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .border(BorderStroke(4.dp, Color.White), shape = RoundedCornerShape(10.dp))
                    .fillMaxSize()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        style = Typography.displayMedium.copy(
                            fontSize = 24.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

            }

        }
    }
}