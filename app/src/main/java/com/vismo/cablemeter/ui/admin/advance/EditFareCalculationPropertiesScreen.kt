package com.vismo.cablemeter.ui.admin.advance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vismo.cablemeter.ui.theme.mineShaft100
import com.vismo.cablemeter.ui.theme.primary800

@Composable
fun EditFareCalculationPropertiesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "首2公里", modifier = Modifier.weight(1f))
            TextField(value = "", onValueChange = { /*TODO*/ }, modifier = Modifier.weight(3f),)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "分水嶺", modifier = Modifier.weight(1f))
            TextField(value = "", onValueChange = { /*TODO*/ }, modifier = Modifier.weight(3f),)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "每200米", modifier = Modifier.weight(1f))
            TextField(value = "", onValueChange = { /*TODO*/ }, modifier = Modifier.weight(3f),)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "後200米", modifier = Modifier.weight(1f))
            TextField(value = "", onValueChange = { /*TODO*/ }, modifier = Modifier.weight(3f),)
        }
        Button(
            onClick = { /*TODO*/ },
            colors = ButtonDefaults.buttonColors(containerColor = primary800, contentColor = mineShaft100),
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Update")
        }
    }
}