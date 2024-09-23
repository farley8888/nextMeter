package com.vismo.cablemeter.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vismo.cablemeter.ui.theme.mineShaft100
import com.vismo.cablemeter.ui.theme.nobel600

@Composable
fun GlobalPinTextField(
    pin: String,
) {
    val pinLength = 6

    Row(
        modifier = Modifier
            .wrapContentSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (i in 0 until pinLength) {
            Row(
                modifier = Modifier.size(40.dp).background(nobel600),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BasicTextField(
                    value = pin.getOrNull(i)?.toString() ?: "",
                    onValueChange = {},
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            innerTextField()
                        }
                    },
                    textStyle = TextStyle(color = mineShaft100, textAlign = TextAlign.Center),
                )
            }
        }
    }
}
