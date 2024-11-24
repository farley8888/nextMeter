package com.vismo.nextgenmeter.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.ui.theme.mineShaft100
import com.vismo.nextgenmeter.ui.theme.nobel600
import com.vismo.nextgenmeter.ui.theme.valencia900

@Composable
fun GlobalNumberKeypad (
onNumberClick: (Int) -> Unit,
onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),  // 2024 11 24 VAN - not enough time to figure out the real way to make the container width responsive
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val numberMatrix = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
            listOf(-1, 0, -2) // -1 for no number, -2 for delete
        )

        for (row in numberMatrix) {
            Row(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (number in row) {
                    if (number >= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1.0f)
                                .background(nobel600)
                                .clickable { onNumberClick(number) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = number.toString(),
                                color = mineShaft100,
                                style = Typography.headlineLarge,
                            )
                        }
                    } else if (number == -2) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1.0f)
                                .background(valencia900)
                                .clickable { onDeleteClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Del",
                                color = mineShaft100,
                                style = Typography.headlineLarge,
                            )
                        }
                    } else {
                        Spacer(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1.0f)
                        )
                    }
                }
            }
        }
    }
}