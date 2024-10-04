package com.vismo.cablemeter.ui.admin.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.cablemeter.ui.admin.EditAdminPropertiesViewModel
import com.vismo.cablemeter.ui.shared.GlobalSnackbarDelegate
import com.vismo.cablemeter.ui.shared.SnackbarState
import com.vismo.cablemeter.ui.theme.gold350
import com.vismo.cablemeter.ui.theme.mineShaft100
import com.vismo.cablemeter.ui.theme.nobel600
import com.vismo.cablemeter.ui.theme.nobel900
import com.vismo.cablemeter.ui.theme.pastelGreen700
import com.vismo.cablemeter.ui.theme.primary800
import com.vismo.cablemeter.ui.theme.valencia700
import com.vismo.cablemeter.util.GlobalUtils.performVirtualTapFeedback

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
                    onValueChange = { newText -> licensePlateEntered = newText  },
                    modifier = Modifier.fillMaxWidth()
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
                        keyboardType = KeyboardType.Number
                    ),
                )
            }
            Button(
                onClick = {
                    if (!licensePlateEntered.isNullOrBlank()) {
                        viewModel.updateLicensePlate(licensePlateEntered!!)
                    }
                    if (!kValueEntered.isNullOrBlank() && kValueEntered!!.toIntOrNull() != null) {
                        viewModel.updateKValue(kValue = kValueEntered!!.toInt())
                    }
                    if (licensePlateEntered.isNullOrBlank() && kValueEntered?.toIntOrNull() == null) {
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { performVirtualTapFeedback(view) },
                colors = ButtonDefaults.buttonColors(containerColor = primary800, contentColor = mineShaft100),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = "App Settings")
            }
            Button(onClick = {
                performVirtualTapFeedback(view)
                navigateToAdminAdvancedEdit()
                             },
                colors = ButtonDefaults.buttonColors(containerColor = primary800, contentColor = mineShaft100),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)) {
                Text(text = "Advance Settings")
            }
            Divider(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
                color = nobel600,
                thickness = 2.dp
            )
            Text(text = "Current ADB Status")
            Text(text = adbStatus.value?.name ?: "",
                color = if (adbStatus.value == EditAdminPropertiesViewModel.ADBStatus.ENABLED) pastelGreen700 else valencia700,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
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
                Text(text = "Toggle ADB")
            }
        }
    }
}