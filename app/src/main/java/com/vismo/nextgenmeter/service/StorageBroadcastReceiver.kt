package com.vismo.nextgenmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.util.Log
import com.vismo.nextgenmeter.datastore.DeviceDataStore

class StorageBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val status = when (intent?.action) {
            Intent.ACTION_MEDIA_MOUNTED -> {
                StorageReceiverStatus.Mounted
            }
            Intent.ACTION_MEDIA_UNMOUNTED -> {
                StorageReceiverStatus.Unmounted
            }
            else -> {
                StorageReceiverStatus.Unknown
            }
        }
        DeviceDataStore.setStorageReceiverStatus(status)
        Log.d(TAG, "StorageBroadcastReceiver: ${intent?.action}")
    }

    companion object {
        private const val TAG = "StorageBroadcastReceiver"
    }
}

sealed class StorageReceiverStatus {
    data object Mounted: StorageReceiverStatus()
    data object Unmounted: StorageReceiverStatus()
    data object Unknown: StorageReceiverStatus()
}
