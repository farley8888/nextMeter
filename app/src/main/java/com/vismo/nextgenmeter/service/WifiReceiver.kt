package com.vismo.nextgenmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.widget.Toast

class WifiReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Handle the broadcast message here
        val action = intent?.action
        if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
           val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
              if (networkInfo != null && networkInfo.isConnected) {
                  val wifiManager = context!!.getSystemService(Context.WIFI_SERVICE) as WifiManager
                  val wifiInfo = wifiManager.connectionInfo
                // Wi-Fi is connected
                  Toast.makeText(context, "Connected to ${wifiInfo.ssid}", Toast.LENGTH_SHORT).show()
              } else {
                // Wi-Fi is disconnected
                    Toast.makeText(context, "Disconnected from Wi-Fi", Toast.LENGTH_SHORT).show()
              }
        }
    }
}