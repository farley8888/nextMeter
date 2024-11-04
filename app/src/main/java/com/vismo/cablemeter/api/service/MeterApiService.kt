package com.vismo.cablemeter.api.service

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface MeterApiService {
    @GET("meters/{licensePlate}/security/TOTP")
    suspend fun getTOTPData(
        @Header("Authorization") header: String,
        @Path("licensePlate") licensePlate: String,
    ): Response<TotpData>
}

data class TotpData(
    val type: String? = null,
    val value: String? = null
)