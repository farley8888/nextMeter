package com.vismo.nextgenmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vismo.nextgenmeter.repository.UsbEventReceiver

class UsbBroadcastReceiver : BroadcastReceiver() {
    private var usbEventHandler: UsbEventReceiver? = null
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (USB_STATE_ACTION == action) {
            val connected: Boolean = intent.getBooleanExtra("connected", false)
            val hostConnected: Boolean = intent.getBooleanExtra("host_connected", false)
            val connectedState = connected || hostConnected
            if (lastConnectedState == connectedState) {
                // No change in USB state
                return
            }
            else if (connectedState) {
                usbEventHandler?.onUsbDeviceChanged(true)
                lastConnectedState = true
            } else {
                usbEventHandler?.onUsbDeviceChanged(false)
                lastConnectedState = false
            }
        }
    }

    companion object {
        var lastConnectedState: Boolean = false
        fun newInstance(usbEventHandler: UsbEventReceiver): UsbBroadcastReceiver {
            val usbBroadcastReceiver = UsbBroadcastReceiver()
            usbBroadcastReceiver.usbEventHandler = usbEventHandler
            return usbBroadcastReceiver
        }
        const val USB_STATE_ACTION = "android.hardware.usb.action.USB_STATE"
    }
}

sealed class USBReceiverStatus {
    data object Attached: USBReceiverStatus()
    data object Detached: USBReceiverStatus()
    data object Unknown: USBReceiverStatus()
}