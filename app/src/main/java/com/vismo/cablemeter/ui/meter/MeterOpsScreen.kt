package com.vismo.cablemeter.ui.meter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vismo.cablemeter.R
import com.vismo.cablemeter.ui.theme.Black
import com.vismo.cablemeter.ui.theme.LightGray
import com.vismo.cablemeter.ui.theme.Red
import com.vismo.cablemeter.ui.theme.SuperLightGray
import com.vismo.cablemeter.ui.theme.White

@Preview(
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape"
)
@Composable
fun MeterOpsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            ExtrasGroup(
                modifier = Modifier
                    .weight(1f)
            )
            FareGroup(
                modifier = Modifier
                    .weight(2f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        PriceSeparatedLine()
        Spacer(modifier = Modifier.height(8.dp))
        MetricGroup()
    }
}

@Composable
fun FareGroup(modifier: Modifier) {
    Column(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
    Text(
            text = "FARE",
            color = White,
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
        )
        ViewSeparator(Modifier.fillMaxWidth())
        ExtrasDetails()
    }
}

@Composable
fun ExtrasGroup(modifier: Modifier) {
    Column(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
        Text(
            text = "EXTRAS",
            color = White,
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
        )
        ViewSeparator(Modifier.fillMaxWidth())
        ExtrasDetails()
    }
}

@Composable
fun ViewSeparator(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(4.dp)
            .background(color = SuperLightGray)
    )
}

@Composable
fun ExtrasDetails() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 6.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.hkd_label),
                color = White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "¢",
                color = White,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Text(
                text = "",
                color = Red,
                fontSize = 78.sp,
                modifier = Modifier
                    .weight(1f)
//                    .padding(top = (-30).dp)
                    .align(Alignment.CenterVertically)
            )
            // Image composable for the fare label
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Text(
                text = "",
                color = Red,
                fontSize = 78.sp,
                modifier = Modifier
                    .weight(1f)
//                    .padding(top = (-35).dp)
                    .align(Alignment.CenterVertically)
            )
            // Image composable for the extra label
        }
    }
}

@Composable
fun PriceSeparatedLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(color = LightGray)
    )
}

@Composable
fun MetricGroup() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp)
    ) {
        MetricItem(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            label = "DIST. ( K M )",
            value = "",
            valueColor = LightGray
        )
        MetricItem(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            label = "TIME",
            value = "",
            valueColor = LightGray
        )
        MeterGoButton(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, end = 8.dp, bottom = 8.dp)
                .background(color = Color.Gray)
        )
    }
}

@Composable
fun MetricItem(modifier: Modifier, label: String, value: String, valueColor: Color) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            color = LightGray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        ViewSeparator(Modifier.fillMaxWidth())
        Text(
            text = value,
            color = valueColor,
            fontSize = 50.sp,
            modifier = Modifier
                .fillMaxWidth()
//                .padding(top = (-35).dp)
                .align(Alignment.End)
        )
    }
}

@Composable
fun MeterGoButton(modifier: Modifier) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Text(
                text = "粵",
                color = White,
                fontSize = 32.sp,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            Column(
                modifier = Modifier.weight(2f)
            ) {
                Text(
                    text = "往",
                    color = White,
                    fontSize = 26.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "HIRED",
                    color = White,
                    fontSize = 26.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}