package com.vismo.nextgenmeter.repository

interface UsbEventReceiver {
    fun onUsbDeviceChanged(isConnected: Boolean)
}