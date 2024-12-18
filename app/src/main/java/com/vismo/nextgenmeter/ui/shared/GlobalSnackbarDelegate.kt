package com.vismo.nextgenmeter.ui.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.vismo.nextgenmeter.ui.theme.gold300
import com.vismo.nextgenmeter.ui.theme.mineShaft50
import com.vismo.nextgenmeter.ui.theme.mineShaft900
import com.vismo.nextgenmeter.ui.theme.nobel100
import com.vismo.nextgenmeter.ui.theme.pastelGreen600
import com.vismo.nextgenmeter.ui.theme.valencia600
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class SnackbarState {
    DEFAULT,
    ERROR,
    SUCCESS,
    WARNING
}

class GlobalSnackbarDelegate(
    var sbHostState: SnackbarHostState? = null,
    var coroutineScope: CoroutineScope? = null
) {

    private var snackbarState: SnackbarState = SnackbarState.DEFAULT

    val snackbarBackgroundColor: Color
        @Composable
        get() = when (snackbarState) {
            SnackbarState.DEFAULT -> nobel100
            SnackbarState.ERROR -> valencia600
            SnackbarState.SUCCESS -> pastelGreen600
            SnackbarState.WARNING -> gold300
        }
    val snackbarContentColor: Color
        @Composable
        get() = when (snackbarState) {
            SnackbarState.DEFAULT -> mineShaft900
            SnackbarState.ERROR -> mineShaft50
            SnackbarState.SUCCESS -> mineShaft50
            SnackbarState.WARNING -> mineShaft900
        }

    fun showSnackbar(
        state: SnackbarState,
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        this.snackbarState = state
        coroutineScope?.launch {
            sbHostState?.showSnackbar(message, actionLabel, duration = duration)
        }
    }
}