package com.vismo.nxgnfirebasemodule.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.gson.annotations.SerializedName


data class MeterTripInFirestore(
    @SerializedName("id") val tripId: String,
    @SerializedName("fare") val fare: Double? = null,
    @SerializedName("extra") val extra: Double? = null,
    @SerializedName("distance") val distanceInMeter: Double? = null,
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
    @SerializedName("trip_status") val tripStatus: String? = null,
    @SerializedName("location_start") val locationStart: GeoPoint? = null,
    @SerializedName("location_end") val locationEnd: GeoPoint? = null,
    @SerializedName("dash_tips") val tips: Double? = null,
    @SerializedName("dash_fee") val dashFee: Double? = null,
    @SerializedName("dash_fee_rate") val dashFeeRate: Double? = null,
    @SerializedName("dash_fee_constant") val dashFeeConstant: Double? = null,
    @SerializedName("dash_discount") val dashDiscount: Double? = null,
    @SerializedName("dash_discount_rate") val dashDiscountRate: Double? = null,
    @SerializedName("dash_discount_constant") val dashDiscountConstant: Double? = null
)