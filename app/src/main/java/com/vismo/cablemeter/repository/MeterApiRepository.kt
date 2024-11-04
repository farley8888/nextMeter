package com.vismo.cablemeter.repository

import com.vismo.cablemeter.api.ApiHandler
import com.vismo.cablemeter.api.NetworkResult
import com.vismo.cablemeter.api.service.MeterApiService
import com.vismo.cablemeter.api.service.TotpData
import com.vismo.cablemeter.repository.FirebaseAuthRepository.Companion.AUTHORIZATION_HEADER


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