package com.vismo.nextgenmeter.api.service

import com.google.gson.annotations.SerializedName
import com.vismo.nextgenmeter.model.AuthToken
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface MeterOApiService {
    @POST("auth")
    suspend fun postFirebaseAuthToken(
        @Body body: RequestBody,
        @Header("Content-Type") contentType: String = CONTENT_TYPE,
    ): Response<AuthToken>

    companion object {
        const val CONTENT_TYPE = "text/plain"
    }
}


interface NgrokApiService {
    @GET("get_time")
    suspend fun getCurrentTime(): Response<TimeResponse>
}

data class TimeResponse(
    @SerializedName("current_time") val time: String
)