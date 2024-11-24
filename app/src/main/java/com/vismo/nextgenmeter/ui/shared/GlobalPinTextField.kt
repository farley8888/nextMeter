package com.vismo.nextgenmeter.ui.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.nextgenmeter.ui.theme.mineShaft50
import com.vismo.nextgenmeter.ui.theme.nobel400

@Composable
fun GlobalPinTextField(
    pin: String,
) {
    val pinLength = 6

    Row(
        modifier = Modifier
            .wrapContentSize(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (i in 0 until pinLength) {
            Row(
                modifier = Modifier
                    .size(40.dp)
                    .border(BorderStroke(1.dp, nobel400), shape = RoundedCornerShape(8.dp))
                    .background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                BasicTextField(
                    enabled = false,
                    value = pin.getOrNull(i)?.toString() ?: "",
                    onValueChange = {},
                    singleLine = true,
                    textStyle = TextStyle(color = mineShaft50, textAlign = TextAlign.Center, fontSize = 18.sp),
                )
            }
        }
    }
}
