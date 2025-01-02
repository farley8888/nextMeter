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
    @SerializedName("version") val version: String,
)

fun Update.canBeSnoozed(): Boolean {
    val now = Timestamp.now()
    val snoozeUntil = now.seconds + 86400 // 24 hours
    return snoozeUntil < mustUpdateBefore.seconds
}

fun Update.snoozeForADay(): Update {
    val now = Timestamp.now()
    val snoozeUntil = now.seconds + 86400 // 24 hours
    return this.copy(snoozeUntil = Timestamp(snoozeUntil, 0))
}

fun Update.shouldPrompt(): Boolean {
    // complete on timestamp is null and snooze until is either null or before current time
    return (completedOn == null) && (snoozeUntil == null || snoozeUntil.seconds < Timestamp.now().seconds)
}