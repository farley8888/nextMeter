package com.vismo.nextgenmeter.util

import com.ilin.util.ShellUtils

object ShellStateUtil {
    private const val ACC_SLEEP_STATUS = "1"
    
    fun isACCSleeping(): Boolean {
        val acc = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio75/value")
        return acc == ACC_SLEEP_STATUS
    }

    fun getAndroidId(): String {
        return ShellUtils.execShellCmd("getprop ro.serialno")
    }

    fun getROMVersion(): String {
        return ShellUtils.execShellCmd("getprop firmwareVersion")
    }
} 