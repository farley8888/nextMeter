package com.vismo.nextgenmeter.model

import com.google.gson.annotations.SerializedName
import java.util.Locale

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

fun MCUFareParams.format() : MCUFareParams {
    fun getFormattedStartPrice(startingPrice: String): String =
        "$${String.format(Locale.US, "%.2f", (startingPrice).toDouble() / 100)}"

    fun getFormattedStepPrice(stepPrice: String): String =
        "$${String.format(Locale.US, "%.2f", (stepPrice).toDouble() / 5 / 100)}"

    fun getFormattedChangedPriceAt(changedPriceAt: String): String =
        "$${String.format(Locale.US, "%.2f", (changedPriceAt).toDouble() / 10)}"

    fun getFormattedChangedStepPrice(changedStepPrice: String): String =
        "$${String.format(Locale.US, "%.2f", (changedStepPrice).toDouble() / 5 / 100)}"

    return MCUFareParams(
        parametersVersion = this.parametersVersion,
        firmwareVersion = this.firmwareVersion,
        kValue = this.kValue,
        startingDistance = this.startingDistance,
        startingPrice = getFormattedStartPrice(this.startingPrice),
        stepPrice = getFormattedStepPrice(this.stepPrice),
        changedPriceAt = getFormattedChangedPriceAt(this.changedPriceAt),
        changedStepPrice = getFormattedChangedStepPrice(this.changedStepPrice),
    )
}
