package com.vismo.nextgenmeter.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vismo.nextgenmeter.BuildConfig
import com.vismo.nextgenmeter.ui.shared.GenericDialogContent
import com.vismo.nextgenmeter.ui.shared.GlobalDialog
import com.vismo.nextgenmeter.ui.theme.Typography
import com.vismo.nextgenmeter.util.GlobalUtils
import com.vismo.nxgnfirebasemodule.util.Constant

@Composable
fun UpdateScreen(
    viewModel: UpdateViewModel,
    navigateToMeterOps: () -> Unit
) {
    val showRetryDialog = remember { mutableStateOf(false) }
    val updateState by viewModel.updateState.collectAsState()
    val updateDetails by viewModel.updateDetails.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val currentVersionName = if (updateDetails?.type == Constant.OTA_METERAPP_TYPE) "${BuildConfig.VERSION_NAME} -> " else ""
            // details of the update
            Text("升級部份: \n計費軟件 $currentVersionName${updateDetails?.version}", style = Typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("升級細節: ${updateDetails?.description}", style = Typography.headlineMedium)
            Text("升級期限:" + GlobalUtils.formatTimestamp(updateDetails?.mustUpdateBefore, showTime = false, showDate = true), style = Typography.headlineMedium)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (updateState) {
                is UpdateState.Idle -> {
                    Text("檢查更新", style = Typography.headlineMedium)
                }
                is UpdateState.Downloading -> {
                    // Downloading UI
                    val download = (updateState as UpdateState.Downloading)
                    Text(text = if (download.progress > 0) {
                        "下载: ${download.progress}%"
                    } else {
                        "下载..."
                    }, style = Typography.headlineMedium, textAlign = TextAlign.Center)
                    if (download.isCancellable) {
                        Text("整個更新需時約 15 分鐘。如暫時不方便，您可選擇暫時跳過更新，在下次著車時再處理", style = Typography.bodyMedium, textAlign = TextAlign.Center)
                        Button(
                            onClick = {
                                viewModel.skipAndroidROMOta()
                                navigateToMeterOps()
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("暫且跳過")
                        }
                    }

                }
                is UpdateState.Installing -> {
                    Text(text = "安装中，請稍候", style = Typography.headlineMedium)
                }
                is UpdateState.Success -> {
                    Text(text = "更新成功。正在重新啟動。請稍候...", style = Typography.headlineMedium, color = androidx.compose.ui.graphics.Color.Green)
                }
                is UpdateState.Error -> {
                    val error = (updateState as UpdateState.Error)
                    Text("出錯: ${error.message}", style = Typography.headlineMedium, color = androidx.compose.ui.graphics.Color.Red)
                    showRetryDialog.value = error.allowRetry
                }

                is UpdateState.NoUpdateFound -> {
                    Text("未找到更新", style = Typography.headlineMedium)
                    Button(
                        onClick = {
                            viewModel.skipAndroidROMOta()
                            navigateToMeterOps()
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("跳過並繼續")
                    }
                }
            }
        }
    }

    GlobalDialog(
        onDismiss = {},
        showDialog = showRetryDialog,
        isBlinking = false,
        content = {
            GenericDialogContent(
                title = "更新失敗",
                message = "下載失敗，請重試。",
                confirmButtonText = "稍後再試",
                cancelButtonText = "重試",
                onConfirm = {
                    showRetryDialog.value = false
                    navigateToMeterOps()
                },
                onCancel = {
                    viewModel.retryDownload()
                    showRetryDialog.value = false
                },
            )
        },
    )
}