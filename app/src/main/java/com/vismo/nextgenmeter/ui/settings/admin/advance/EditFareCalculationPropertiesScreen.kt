package com.vismo.nextgenmeter.ui.settings.admin.advance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.vismo.nextgenmeter.model.format
import com.vismo.nextgenmeter.ui.settings.admin.EditAdminPropertiesViewModel
import com.vismo.nextgenmeter.ui.shared.GlobalSnackbarDelegate
import com.vismo.nextgenmeter.ui.shared.SnackbarState
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.ui.theme.gold350
import com.vismo.nextgenmeter.ui.theme.mineShaft900
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun EditFareCalculationPropertiesScreen(
    viewModel: EditAdminPropertiesViewModel,
    snackbarDelegate: GlobalSnackbarDelegate
) {
    val mcuPriceParams = viewModel.mcuPriceParams.collectAsState().value?.format()
    var priceParam1Entered: String?  by remember { mutableStateOf(null) }
    var priceParam2Entered: String?  by remember { mutableStateOf(null) }
    var priceParam3Entered: String?  by remember { mutableStateOf(null) }
    var priceParam4Entered: String?  by remember { mutableStateOf(null) }
    val startPriceStr = mcuPriceParams?.startingPrice?.takeIf { priceParam1Entered == null } ?: priceParam1Entered!!
    val changePriceAtStr = mcuPriceParams?.changedPriceAt?.takeIf { priceParam2Entered == null } ?: priceParam2Entered!!
    val stepPriceStr = mcuPriceParams?.stepPrice?.takeIf { priceParam3Entered == null } ?: priceParam3Entered!!
    val changedStepPriceStr = mcuPriceParams?.changedStepPrice?.takeIf { priceParam4Entered == null } ?: priceParam4Entered!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "首2公里", modifier = Modifier.weight(1f))
            TextField(
                value = startPriceStr,
                onValueChange = { newText -> priceParam1Entered = newText },
                modifier = Modifier.weight(3f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),

            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "分水嶺", modifier = Modifier.weight(1f))
            TextField(
                value = changePriceAtStr,
                onValueChange = { newText -> priceParam2Entered = newText },
                modifier = Modifier.weight(3f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),

            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "每200米", modifier = Modifier.weight(1f))
            TextField(
                value = stepPriceStr,
                onValueChange = { newText -> priceParam3Entered = newText },
                modifier = Modifier.weight(3f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "後200米", modifier = Modifier.weight(1f))
            TextField(
                value = changedStepPriceStr,
                onValueChange = { newText -> priceParam4Entered = newText },
                modifier = Modifier.weight(3f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
            )
        }
        val view = LocalView.current
        Button(
            onClick = {
                val startPrice = (startPriceStr.toDoubleOrNull()?.times(100))?.toInt()
                val stepPrice = (stepPriceStr.toDoubleOrNull()?.times(5)?.times(100))?.toInt()
                val stepPrice2 = (changedStepPriceStr.toDoubleOrNull()?.times(5)?.times(100))?.toInt()
                val threshold = (changePriceAtStr.toDoubleOrNull()?.times(10))?.toInt()

                if (startPrice == null || stepPrice == null || stepPrice2 == null || threshold == null) {
                    snackbarDelegate.showSnackbar(SnackbarState.ERROR, "Invalid values entered.")
                    return@Button
                }
                viewModel.updatePriceParams(
                    startPrice = startPrice,
                    stepPrice = stepPrice,
                    stepPrice2nd = stepPrice2,
                    threshold = threshold
                )
                snackbarDelegate.showSnackbar(SnackbarState.SUCCESS,"Fare parameters updated")
                performVirtualTapFeedback(view)
            },
            colors = ButtonDefaults.buttonColors(containerColor = gold350, contentColor = mineShaft900),
            modifier = Modifier
                .padding(top = 16.dp)
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
    }
}