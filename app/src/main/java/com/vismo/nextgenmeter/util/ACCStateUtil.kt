package com.vismo.nextgenmeter.util

import com.ilin.util.ShellUtils

object ACCStateUtil {
    private const val ACC_SLEEP_STATUS = "1"
    
    fun isACCSleeping(): Boolean {
        val acc = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio75/value")
        return acc == ACC_SLEEP_STATUS
    }
    
    fun isACCAwake(): Boolean = !isACCSleeping()
} 