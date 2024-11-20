package com.vismo.nextgenmeter.model

import com.google.gson.annotations.SerializedName

data class SessionInfo (
    @SerializedName("carousel_assets") val carouselAssets: CarouselAssets? = null,
    @SerializedName("session") val session: Map<String, *>? = null,
    @SerializedName("settings") val settings: Map<String, *>? = null,
)

data class CarouselAssets (
    @SerializedName("marketing") val marketingAssets: List<Map<String, *>?>? = null,
    @SerializedName("post_payment") val postPaymentAssets: List<Map<String, *>?>? = null,
)

data class AuthToken(
    @SerializedName("custom_token")
    val customToken: String?,
    @SerializedName("id_token")
    val idToken: String?,
)