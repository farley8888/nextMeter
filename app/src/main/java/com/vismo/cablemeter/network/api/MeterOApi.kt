package com.vismo.cablemeter.network.api

import com.vismo.cablemeter.model.AuthToken
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path

interface MeterOApi {
    @POST("auth")
    suspend fun postFirebaseAuthToken(
        @Body body: RequestBody,
        @Header("Content-Type") contentType: String = CONTENT_TYPE,
    ): Response<AuthToken>

    @POST("/driver/v1.1/{driverId}/pair-pin")
    suspend fun postPairWithDriver(
        @Path("driverId") driverId: String,
        @HeaderMap headers: Map<String, String>,
    ): Response<Unit>

    companion object {
        const val CONTENT_TYPE = "text/plain"
    }
}