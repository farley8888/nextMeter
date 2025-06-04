package com.vismo.nxgnfirebasemodule.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.gson.annotations.SerializedName
import com.vismo.nxgnfirebasemodule.util.DashUtil.roundTo
import org.json.JSONObject
import java.io.Serializable


data class MeterTripInFirestore(
    @SerializedName("id") val tripId: String,
    @SerializedName("fare") val fare: Double? = null,
    @SerializedName("extra") val extra: Double? = null,
    @SerializedName("distance") val paidDistanceInMeters: Double? = null,
    @SerializedName("unpaid_distance") val unpaidDistanceInMeters: Double? = null,
    @SerializedName("wait_time") val waitTimeInSecond: Int? = null,
    @SerializedName("last_update_time") val lastUpdateTime: Timestamp? = null,
    @SerializedName("trip_start") val startTime: Timestamp? = null,
    @SerializedName("trip_end") val endTime: Timestamp? = null,
    @SerializedName("payment_type") val paymentType: String? = null,
    @SerializedName("auth_amount") val authAmount: Double? = null,
    @SerializedName("total") val total: Double? = null,
    @SerializedName("trip_total") val tripTotal: Double? = null,
    @SerializedName("estimated_dash_amount") val estimatedDashAmount: Double? = null,
    @SerializedName("total_fare") val totalFare: Double? = null,
    @SerializedName("trip_status") val tripStatus: TripStatus? = null,
    @SerializedName("location_start") val locationStart: GeoPoint? = null,
    @SerializedName("location_end") val locationEnd: GeoPoint? = null,
    @SerializedName("dash_tips") val tips: Double? = null,
    @SerializedName("dash_fee") val dashFee: Double? = null,
    @SerializedName("dash_fee_rate") val dashFeeRate: Double? = null,
    @SerializedName("dash_fee_constant") val dashFeeConstant: Double? = null,
    @SerializedName("user") val user: PairedUserInformation? = null,
    @SerializedName("payment_method_selected") val paymentMethodSelectedOnPOS: String? = null,
    @SerializedName("payment_information") val paymentInformation: List<Map<String, *>>? = null,
    @SerializedName("session") val session: TripSession? = null,
    @SerializedName("driver") val driver: Driver? = null,
    @SerializedName("creation_time") val creationTime: Timestamp? = null,
    @SerializedName("license_plate") val licensePlate: String? = null,
    @SerializedName("meter_software_version") val meterSoftwareVersion: String? = null,
    @SerializedName("meter_id") val meterId: String? = null,
    @SerializedName("discount_amount") val discountAmountNegative: Double? = null,
): Serializable

data class TripSession(
    @SerializedName("id") val id: String? = null,
): Serializable


enum class TripStatus {
    HIRED,
    STOP, // - this means the meter is PAUSED - TODO: rename later so that it is compatible with POS
    ENDED
}

data class PairedUserInformation(
    @SerializedName("id") val id: String? = null,
    @SerializedName("phone") val phone: String? = null,
): Serializable

enum class TripPaidStatus {
    NOT_PAID,
    PARTIALLY_PAID,
    COMPLETELY_PAID
}

fun MeterTripInFirestore.paidStatus(): TripPaidStatus {
    val amountPaid = amountPaid()
    Log.d("MeterTripInFirestore", "amountPaid: $amountPaid")
    return when {
        // if user is not null - it means the user's card is already processed by the user app
        isDashPayment() || user != null -> TripPaidStatus.COMPLETELY_PAID
        amountPaid > 0 -> TripPaidStatus.PARTIALLY_PAID
        else -> TripPaidStatus.NOT_PAID
    }
}

fun MeterTripInFirestore.isDashPayment(): Boolean {
    return paymentType?.lowercase() == "dash"
}

private fun MeterTripInFirestore.amountPaid(): Double {
    // Filter successful transactions - this assumes all successful transactions either AUTH or SALE type as we don't have VOID from POS anymore
    val successTransactions = paymentInformation?.filter { it["status"] == "SUCCESS" }.orEmpty()

    val latestTransaction = successTransactions.lastOrNull() ?: return 0.0

    val transactionResponse = latestTransaction["body"] as? String ?: return 0.0
    val transactionObject = JSONObject(transactionResponse)
    // Check if the transaction is approved and return the total amount
    val tranStatus = transactionObject.optString("tranStatus")
    return if (tranStatus == "APPROVED") {
        transactionObject.optDouble("totalAmount", 0.0)
    } else {
        0.0
    }
}

fun MeterTripInFirestore.getPricingResult(): PricingResult {
    val feeAndExtra = tripTotal ?: 0.0
    val applicableTip = tips ?: 0.0
    val discountAmount = discountAmountNegative ?: 0.0


    val applicableFee = dashFee ?: 0.0

    val applicableTotal = (feeAndExtra + applicableTip + applicableFee + discountAmount).roundTo(1)

    return PricingResult(
        applicableFee = applicableFee,
        applicableDiscount = discountAmount,
        applicableTotal = applicableTotal.toDouble()
    )
}