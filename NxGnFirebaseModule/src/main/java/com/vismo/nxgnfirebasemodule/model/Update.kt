package com.vismo.nxgnfirebasemodule.model

import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName

data class Update(
    @SerializedName("id") val id: String,
    @SerializedName("completed_on") val completedOn: Timestamp?,
    @SerializedName("created_on") val createdOn: Timestamp,
    @SerializedName("description") val description: String,
    @SerializedName("type") val type: String,
    @SerializedName("must_update_before") val mustUpdateBefore: Timestamp,
    @SerializedName("snooze_until") val snoozeUntil: Timestamp?,
    @SerializedName("url") val url: String,
    @SerializedName("version") val version: String
)