package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.api.ApiHandler
import com.vismo.nextgenmeter.api.NetworkResult
import com.vismo.nextgenmeter.api.service.MeterOApiService
import com.vismo.nextgenmeter.api.service.NgrokApiService
import com.vismo.nextgenmeter.api.service.TimeResponse
import com.vismo.nextgenmeter.model.AuthToken
import okhttp3.RequestBody

@Deprecated("Use MeterApiRepository instead")
class MeterOApiRepository(
    private val meterOApiService: MeterOApiService,
    private val ngrokApiService: NgrokApiService
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

    suspend fun getCurrentTime(): NetworkResult<TimeResponse> {
        return handleApi {
            ngrokApiService.getCurrentTime()
        }
    }
}