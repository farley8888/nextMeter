package com.vismo.nextgenmeter.ui.settings.admin.basic

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.RoundedCorner
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vismo.nextgenmeter.ui.settings.admin.EditAdminPropertiesViewModel
import com.vismo.nextgenmeter.ui.shared.GlobalSnackbarDelegate
import com.vismo.nextgenmeter.ui.shared.SnackbarState
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.ui.theme.gold350
import com.vismo.nextgenmeter.ui.theme.mineShaft100
import com.vismo.nextgenmeter.ui.theme.nobel600
import com.vismo.nextgenmeter.ui.theme.nobel900
import com.vismo.nextgenmeter.ui.theme.pastelGreen700
import com.vismo.nextgenmeter.ui.theme.primary800
import com.vismo.nextgenmeter.ui.theme.red
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun EditKValueAndLicensePlateScreen(
    viewModel: EditAdminPropertiesViewModel,
    snackbarDelegate: GlobalSnackbarDelegate,
    navigateToAdminAdvancedEdit: () -> Unit
) {
    val deviceIdData = viewModel.deviceIdData.collectAsState()
    val mcuPriceParams = viewModel.mcuPriceParams.collectAsState()
    var kValueEntered: String? by remember { mutableStateOf(null) }
    var licensePlateEntered: String? by remember { mutableStateOf(null) }
    val adbStatus = viewModel.currentADBStatus.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(nobel900)
    ) {
        val view = LocalView.current
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val licensePlate = deviceIdData.value?.licensePlate ?: ""
                Text(text = "車牌")
                TextField(
                    value = licensePlate.takeIf { licensePlateEntered == null } ?: licensePlateEntered!!,
                    onValueChange = { newText ->
                        // remove line breaks
                        licensePlateEntered = newText.replace("\n", "").uppercase()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    visualTransformation = VisualTransformation.None
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val kValue = mcuPriceParams.value?.kValue ?: ""
                Text(text = "K値")
                TextField(
                    value = kValue.takeIf { kValueEntered == null } ?: kValueEntered!!,
                    onValueChange = { newText -> kValueEntered = newText.uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                )
            }
            Button(
                onClick = {
                    if (licensePlateEntered != null && licensePlateEntered != deviceIdData.value?.licensePlate) {
                        viewModel.updateLicensePlate(licensePlateEntered.toString())
                    }
                    if (!kValueEntered.isNullOrBlank() && kValueEntered!!.toIntOrNull() != null) {
                        viewModel.updateKValue(kValue = kValueEntered!!.toInt())
                    }
                    if (kValueEntered?.toIntOrNull() == null && licensePlateEntered == deviceIdData.value?.licensePlate) {
                        snackbarDelegate.showSnackbar(SnackbarState.ERROR, "No changes made. Please check if values are correctly entered.")
                    } else {
                        snackbarDelegate.showSnackbar(SnackbarState.SUCCESS,"Values updated")
                        viewModel.reEnquireParameters()
                    }
                    performVirtualTapFeedback(view)
                },
                colors = ButtonDefaults.buttonColors(containerColor = gold350),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Update",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Box(
                modifier = Modifier
                    .timedClick(
                        timeInMillis = 10000,
                    ) { passed: Boolean ->

                        if (!passed) {
                            return@timedClick;
                        }

                        openSystemSettings(context = view.context)
                        performVirtualTapFeedback(view)
                    }
                    .height(108.dp)
                    .fillMaxWidth()
            ) {
                //
            }
        }

        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Surface(
                shape = ButtonDefaults.shape,
                color = primary800,
                contentColor = mineShaft100,
            ) {
                Row(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    performVirtualTapFeedback(view)
                                    navigateToAdminAdvancedEdit()
                                },
                            )
                        }
                        .fillMaxWidth()
                        .defaultMinSize(
                            minWidth = ButtonDefaults.MinWidth,
                            minHeight = ButtonDefaults.MinHeight
                        )
                        .padding(ButtonDefaults.ContentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Advance Settings", style = Typography.titleMedium)
                }
            }

            Divider(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
                color = nobel600,
                thickness = 2.dp
            )
            Text(text = "Current ADB Status")
            Text(text = adbStatus.value?.name ?: "",
                color = if (adbStatus.value == EditAdminPropertiesViewModel.ADBStatus.ENABLED) pastelGreen700 else red,
                fontWeight = FontWeight.Bold,
                style = Typography.headlineMedium,
            )
            Button(onClick = {
                performVirtualTapFeedback(view)
                viewModel.toggleADB()
                             },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary800,
                    contentColor = mineShaft100
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = "Toggle ADB", style = Typography.titleMedium)
            }
        }
    }
}


@Composable
private fun Modifier.timedClick(
    timeInMillis: Long,
    interactionSource: MutableInteractionSource = remember {MutableInteractionSource()},
    onClick: (Boolean) -> Unit
) = composed {

    var timeOfTouch = -1L
    LaunchedEffect(key1 = timeInMillis, key2 = interactionSource) {
        interactionSource.interactions
            .onEach { interaction: Interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        timeOfTouch = System.currentTimeMillis()
                    }
                    is PressInteraction.Release -> {
                        val currentTime = System.currentTimeMillis()
                        onClick(currentTime - timeOfTouch > timeInMillis)
                    }
                    is PressInteraction.Cancel -> {
                        onClick(false)
                    }
                }

            }
            .launchIn(this)
    }

    Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {}
    )
}


private fun openSystemSettings(context: Context) {
    val intent = Intent("android.intent.action.VIEW").apply {
        component = ComponentName("com.android.settings", "com.android.settings.Settings")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra("extra_prefs_show_button_bar", true)
        putExtra("extra_prefs_set_next_text", "完成")
        putExtra("extra_prefs_set_back_text", "返回")
    }
    context.startActivity(intent)
}
