package com.vismo.cablemeter.network.api

import com.vismo.cablemeter.model.AuthToken
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MeterOApi {
    @POST("auth")
    suspend fun postFirebaseAuthToken(
        @Body body: RequestBody,
        @Header("Content-Type") contentType: String = CONTENT_TYPE,
    ): Response<AuthToken>

    companion object {
        const val CONTENT_TYPE = "text/plain"
    }
}