package com.vismo.cablemeter.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ilin.util.ShellUtils
import com.torrydo.floatingbubbleview.service.expandable.BubbleBuilder
import com.torrydo.floatingbubbleview.service.expandable.ExpandableBubbleService
import com.torrydo.floatingbubbleview.service.expandable.ExpandedBubbleBuilder
import com.vismo.cablemeter.R
import com.vismo.cablemeter.ui.shared.FloatingBackView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GlobalBackService: ExpandableBubbleService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceWithNotification()
        minimize()
    }

    // optional, only required if you want to call minimize()
    override fun configBubble(): BubbleBuilder? {
        return BubbleBuilder(this)
            .bubbleCompose {
               FloatingBackView {
                   scope.launch {
                          ShellUtils.execShellCmd("input keyevent 4")
                   }
               }
            }
            .startLocation(750, 50)
            .startLocationPx(750, 50)
            .triggerClickablePerimeterPx(5f)
    }

    override fun configExpandedBubble(): ExpandedBubbleBuilder? {
        // optional, only required if you want to call expand()
        return null
    }

    private fun startForegroundServiceWithNotification() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Back Button Service Running")
            .setContentText("Tap to show the back button")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Background Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for background service"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val TAG = "GlobalBackService"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "GLOBAL_BACK_SERVICE"
    }

}