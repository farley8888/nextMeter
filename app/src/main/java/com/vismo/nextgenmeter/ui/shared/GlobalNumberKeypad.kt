package com.vismo.nextgenmeter.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vismo.nextgenmeter.R
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
            listOf(-2, 0, -3) // -1 for no number, -2 for delete, -3 for confirm
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
                                .background(nobel600)
                                .clickable { onDeleteClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.numberkeyboard_ic_backspace_normal),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .fillMaxWidth()
                            )
                        }
                    } else if (number == -3) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1.0f)
                                .background(nobel600)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_baseline_check_circle),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .fillMaxWidth()
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