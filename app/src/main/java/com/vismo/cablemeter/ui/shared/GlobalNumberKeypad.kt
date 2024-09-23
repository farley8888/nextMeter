package com.vismo.cablemeter.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vismo.cablemeter.ui.theme.mineShaft100
import com.vismo.cablemeter.ui.theme.nobel700
import com.vismo.cablemeter.ui.theme.valencia800

@Composable
fun GlobalNumberKeypad (
onNumberClick: (Int) -> Unit,
onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val numberMatrix = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
            listOf(-1, 0, -2) // -1 for no number, -2 for delete
        )

        for (row in numberMatrix) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (number in row) {
                    if (number >= 0) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .width(60.dp)
                                .background(nobel700)
                                .clickable { onNumberClick(number) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = number.toString(), color = mineShaft100)
                        }
                    } else if (number == -2) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .width(60.dp)
                                .background(valencia800)
                                .clickable { onDeleteClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Del", color = mineShaft100)
                        }
                    } else {
                        Spacer(
                            modifier = Modifier.height(40.dp)
                            .width(60.dp)
                        )
                    }
                }
            }
        }
    }
}