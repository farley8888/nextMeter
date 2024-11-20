package com.vismo.nextgenmeter.model

sealed class OngoingMeasureBoardStatus(val num: Int) {
    companion object {
        fun fromInt(status: Int): OngoingMeasureBoardStatus {
            return when (status) {
                0 -> OngoingMeasureBoardStatusHired(status)
                1 -> OngoingMeasureBoardStatusStopped(status)
                2, 3 -> OngoingMeasureBoardStatusOverspeed(status)  // 2 - hired and overspeed, 3 - stopped and overspeed
                4, 5 -> OngoingMeasureBoardStatusFault(status) // 4 - hired and fault, 5 - stopped and fault
                else -> OngoingMeasureBoardStatusUnknown(status)
            }
        }
    }
}
data class OngoingMeasureBoardStatusHired(val status: Int) :
    OngoingMeasureBoardStatus(status) {
    override fun toString(): String {
        return "Hired: $status"
    }
}

data class OngoingMeasureBoardStatusStopped(val status: Int) :
    OngoingMeasureBoardStatus(status) {
    override fun toString(): String {
        return "Stopped: $status"
    }
}

data class OngoingMeasureBoardStatusOverspeed(val status: Int) :
    OngoingMeasureBoardStatus(status) {
    override fun toString(): String {
        return "Overspeed: $status"
    }
}

data class OngoingMeasureBoardStatusFault(val status: Int) :
    OngoingMeasureBoardStatus(status) {
    override fun toString(): String {
        return "Fault: $status"
    }
}

data class OngoingMeasureBoardStatusUnknown(val status: Int) :
    OngoingMeasureBoardStatus(status) {
    override fun toString(): String {
        return "Unknown: $status"
    }
}
