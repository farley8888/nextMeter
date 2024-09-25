package com.vismo.cablemeter.ui.admin.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vismo.cablemeter.ui.theme.mineShaft100
import com.vismo.cablemeter.ui.theme.nobel600
import com.vismo.cablemeter.ui.theme.nobel900
import com.vismo.cablemeter.ui.theme.primary800

@Composable
fun EditKValueAndLicensePlateScreen(navigateToAdminAdvancedEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(nobel900)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "車牌")
                TextField(value = "", onValueChange = { /*TODO*/ }, modifier = Modifier.fillMaxWidth(),)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "K値")
                TextField(value = "", onValueChange = { /*TODO*/ }, modifier = Modifier.fillMaxWidth(),)
            }
            Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(containerColor = primary800, contentColor = mineShaft100),
                modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
            ) {
                Text(text = "Update")
            }
        }

        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(containerColor = primary800, contentColor = mineShaft100),
                modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
            ) {
                Text(text = "Settings")
            }
            Button(onClick = { navigateToAdminAdvancedEdit() },
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
            Text(text = "Enabled")
            Button(onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(containerColor = primary800, contentColor = mineShaft100),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = "Toggle ADB")
            }
        }
    }
}