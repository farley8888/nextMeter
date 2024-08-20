package com.vismo.cablemeter.model

import com.google.gson.annotations.SerializedName

data class MCUFareParams(
    @SerializedName("parameters_version") val parametersVersion: String,
    @SerializedName("firmware_version") val firmwareVersion: String,
    @SerializedName("k_value") val kValue: String,
    @SerializedName("starting_distance") val startingDistance: String,
    @SerializedName("start_price") val startingPrice: String,
    @SerializedName("step_price") val stepPrice: String,
    @SerializedName("changed_step_price") val changedPriceAt: String,
    @SerializedName("step_price_change_at") val changedStepPrice: String,
)
