package com.vismo.nxgnfirebasemodule.model

import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName

data class UpdateMCUParamsRequest(
    @SerializedName("id") val id: String,
    @SerializedName("created_on") val createdOn: Timestamp,
    @SerializedName("k_value") val kValue: Int?,
    @SerializedName("acc_off_android_board_shutdown_delay_mins") val accOffAndroidBoardShutdownDelayMins: Int?,
    @SerializedName("completed_on") val completedOn: Timestamp? = null,
)

fun UpdateMCUParamsRequest.isCompleted(): Boolean {
    return completedOn != null
}
