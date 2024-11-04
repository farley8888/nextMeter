package com.vismo.cablemeter.repository

import com.vismo.cablemeter.api.ApiHandler
import com.vismo.cablemeter.api.NetworkResult
import com.vismo.cablemeter.api.service.MeterOApiService
import com.vismo.cablemeter.model.AuthToken
import okhttp3.RequestBody

@Deprecated("Use MeterApiRepository instead")
class MeterOApiRepository(
    private val meterOApiService: MeterOApiService,
) : ApiHandler {
    /*
     * Don't put apis that require Authorization header here - as firebase auth repository cannot be injected here
     * Use MeterApiRepository for apis that require Authorization header
     * This repository is only for old apis that are without auth - nothing new should be added there so that we can phase it out
    */
    suspend fun postFirebaseAuthToken(
        body: RequestBody,
        contentType: String = MeterOApiService.CONTENT_TYPE
    ): NetworkResult<AuthToken> {
        return handleApi {
            meterOApiService.postFirebaseAuthToken(
                body = body,
                contentType = contentType
            )
        }
    }
}