package com.vismo.nextgenmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast

interface WifiStateChangeListener {
    fun onWifiStateChanged(isEnabled: Boolean)
}

class WifiBroadcastReceiver(
    private val wifiStateChangeListener: WifiStateChangeListener
): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(TAG, "Null context or intent")
            return
        }

        val action = intent.action
        Log.d(TAG, "Received action: $action")

        when (action) {
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                Log.d(TAG, "WiFi state changed to: $state")
                handleWifiStateChange(context, state)
            }
            WifiManager.SUPPLICANT_STATE_CHANGED_ACTION -> {
                val supplicantState = intent.getParcelableExtra<SupplicantState>(WifiManager.EXTRA_NEW_STATE)
                val error = intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR)
                if (error) {
                    val errorReason = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1)
                    Log.e(TAG, "Supplicant error: $errorReason")
                }
                Log.d(TAG, "Supplicant state changed to: $supplicantState, error: $error")
                handleSupplicantStateChange(context, supplicantState, error)
            }
            "android.net.wifi.STATE_CHANGE" -> {
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                val wifiInfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
                Log.d(TAG, "Network state changed: ${networkInfo?.detailedState}, SSID: ${wifiInfo?.ssid}")
                handleNetworkStateChange(context, networkInfo, wifiInfo)
            }
        }
    }

    private fun handleWifiStateChange(context: Context, state: Int) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        when (state) {
            WifiManager.WIFI_STATE_ENABLED -> {
                Log.d(TAG, "WiFi is enabled")

                wifiStateChangeListener.onWifiStateChanged(wifiManager.isWifiEnabled)
            }
            WifiManager.WIFI_STATE_DISABLED -> {
                Log.d(TAG, "WiFi is disabled")
                wifiStateChangeListener.onWifiStateChanged( false)
            }
            WifiManager.WIFI_STATE_ENABLING -> {
                Log.d(TAG, "WiFi is enabling")
            }
            WifiManager.WIFI_STATE_DISABLING -> {
                Log.d(TAG, "WiFi is disabling")
            }
            else -> {
                Log.d(TAG, "WiFi state unknown")
            }
        }
    }

    private fun handleSupplicantStateChange(context: Context, state: SupplicantState?, error: Boolean) {
        if (error) {
            Log.e(TAG, "Error in supplicant state change")
            wifiStateChangeListener.onWifiStateChanged( false)
            return
        }

        when (state) {
            SupplicantState.COMPLETED -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                Log.d(TAG, "Connected to WiFi: ${wifiInfo.ssid}")
                wifiStateChangeListener.onWifiStateChanged( true)
            }
            SupplicantState.DISCONNECTED -> {
                Log.d(TAG, "Disconnected from WiFi")
                wifiStateChangeListener.onWifiStateChanged( false)
            }
            else -> {
                Log.d(TAG, "Supplicant state changed: $state")
            }
        }
    }

    private fun handleNetworkStateChange(context: Context, networkInfo: NetworkInfo?, wifiInfo: WifiInfo?) {
        if (networkInfo?.isConnected == true) {
            Log.d(TAG, "Network is connected: ${wifiInfo?.ssid}")
            Toast.makeText(context, "Connected to ${wifiInfo?.ssid}", Toast.LENGTH_SHORT).show()
            wifiStateChangeListener.onWifiStateChanged( true)
        } else {
            Log.d(TAG, "Network is disconnected")
            wifiStateChangeListener.onWifiStateChanged( false)
        }
    }

    companion object {
        private const val TAG = "WifiBroadcastReceiver"
    }
}