package com.vismo.nextgenmeter.repository

import com.vismo.nextgenmeter.api.ApiHandler
import com.vismo.nextgenmeter.api.NetworkResult
import com.vismo.nextgenmeter.api.service.MeterApiService
import com.vismo.nextgenmeter.api.service.TotpData
import com.vismo.nextgenmeter.repository.FirebaseAuthRepository.Companion.AUTHORIZATION_HEADER


class MeterApiRepository(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val meterApiService: MeterApiService,
) : ApiHandler {

    suspend fun getTOTPData(
        licensePlate: String,
    ): NetworkResult<TotpData> {
        return handleApi {
            val headers = firebaseAuthRepository.getHeaders()
            val authorization = headers[AUTHORIZATION_HEADER] ?: ""
            meterApiService.getTOTPData(
                header = authorization,
                licensePlate = licensePlate,
            )
        }
    }
}