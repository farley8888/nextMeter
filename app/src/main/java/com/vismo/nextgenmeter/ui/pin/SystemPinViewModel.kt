package com.vismo.nextgenmeter.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vismo.nextgenmeter.api.NetworkResult
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.repository.MeterApiRepository
import com.vismo.nextgenmeter.repository.MeterPreferenceRepository
import com.vismo.nextgenmeter.service.DeviceGodCodeUnlockState
import com.vismo.nextgenmeter.util.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.time.SystemTimeProvider
import dev.samstevens.totp.time.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class SystemPinViewModel @Inject constructor(
    private val meterApiRepository: MeterApiRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val meterPreferenceRepository: MeterPreferenceRepository
): ViewModel() {
    private lateinit var verifier: DefaultCodeVerifier
    private val _totpStatus: MutableStateFlow<String> = MutableStateFlow("")
    val totpStatus: StateFlow<String> = _totpStatus
    private val _navigationToNextScreen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val navigationToNextScreen: StateFlow<Boolean> = _navigationToNextScreen

    init {
        initTotpVerifier()
        refreshTOTPData()
    }

    fun refreshTOTPData() {
        viewModelScope.launch(ioDispatcher) {
            _totpStatus.value = "Refreshing TOTP data..."
            val licensePlate = DeviceDataStore.deviceIdData.first()?.licensePlate ?: ""
            when (val result = meterApiRepository.getTOTPData(licensePlate)) {
                is NetworkResult.Success -> {
                    // Handle success
                    _totpStatus.value = "TOTP data refreshed"
                    val secret = result.data.value
                    encryptAndSaveSecret(secret)
                }
                is NetworkResult.Error -> {
                    // Handle error
                    _totpStatus.value = "Error refreshing TOTP data"
                }
                is NetworkResult.Exception -> {
                    // Handle exception
                    _totpStatus.value = "Error refreshing TOTP data"
                }
            }
        }
    }

    private fun encryptAndSaveSecret(secret: String?) {
        viewModelScope.launch(ioDispatcher) {
            if (secret.isNullOrBlank()) {
                return@launch
            }
            val byteArrayOutputStream = ByteArrayOutputStream()
            CryptoManager.encrypt(secret.toByteArray(Charsets.UTF_8), byteArrayOutputStream)
            val encryptedData = byteArrayOutputStream.toByteArray()
            meterPreferenceRepository.saveTotpSecret(encryptedData)
        }
    }

    private suspend fun getSecretAndDecrypt(): String? {
        val encryptedData = meterPreferenceRepository.getTotpSecret().first() ?: return null
        val byteArrayInputStream = encryptedData.inputStream()
        val decryptedData = CryptoManager.decrypt(byteArrayInputStream)
        return decryptedData.toString(Charsets.UTF_8)
    }

    private fun initTotpVerifier() {
        val timeProvider: TimeProvider = SystemTimeProvider()
        val codeGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA1, 6)

        this.verifier = DefaultCodeVerifier(codeGenerator, timeProvider)
        this.verifier.setTimePeriod(30)
        this.verifier.setAllowedTimePeriodDiscrepancy(0)
    }

    fun verify(code:String) {
        viewModelScope.launch {
            _totpStatus.value = "Verifying TOTP code..."
            val secret = getSecretAndDecrypt() ?: ""
            val isVerifiedByTOTP = verifier.isValidCode(secret, code) || (code == PIN_WITH_GOD_CODE &&
                    DeviceDataStore.deviceGodCodeUnlockState.first() is DeviceGodCodeUnlockState.Unlocked)
            _navigationToNextScreen.value = isVerifiedByTOTP
            if (isVerifiedByTOTP) {
                _totpStatus.value = "TOTP code verified"
            } else {
                _totpStatus.value = "Error verifying TOTP code"
            }
        }
    }

    fun resetNavigation() {
        _navigationToNextScreen.value = false
        _totpStatus.value = ""
    }

    companion object {
        private const val PIN_WITH_GOD_CODE = "191005"
    }
}