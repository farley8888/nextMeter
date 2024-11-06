package com.vismo.cablemeter.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vismo.cablemeter.ui.theme.mineShaft900
import com.vismo.cablemeter.ui.theme.nobel300
import com.vismo.cablemeter.util.GlobalUtils.performVirtualTapFeedback

@Composable
fun FloatingBackView(
    onClick: () -> Unit,
) {
    val view = LocalView.current
    TextButton(
        onClick = {
            performVirtualTapFeedback(view = view)
            onClick()
                  },
        modifier =  Modifier
            .size(70.dp)  // Customize the bubble size here
            .background(nobel300, shape = CircleShape)
    ) {
        Text(text = "Go Back", textAlign = TextAlign.Center, color = mineShaft900)
    }
}
