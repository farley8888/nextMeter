package com.vismo.nextgenmeter.repository

import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.firebase.auth.FirebaseAuth
import com.vismo.nextgenmeter.module.IoDispatcher
import com.vismo.nextgenmeter.api.NetworkResult
import com.vismo.nextgenmeter.util.Constant.PRIVATE_KEY
import com.vismo.nextgenmeter.util.Constant.PUBLIC_KEY
import com.vismo.nextgenmeter.util.GlobalUtils
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val meterOApiRepository: MeterOApiRepository,
    private val meterPreferenceRepository: MeterPreferenceRepository
){
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception", throwable)
        Sentry.addBreadcrumb("FirebaseAuthRepository Scope exception", "FirebaseAuthRepository Scope exception")
        Sentry.captureException(throwable) { scope ->
            scope.level = SentryLevel.ERROR
        }
    }

    private var externalScope: CoroutineScope? = null

    private val _isFirebaseAuthSuccess: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFirebaseAuthSuccess: StateFlow<Boolean> = _isFirebaseAuthSuccess

    fun initToken(scope: CoroutineScope) {
        Log.d(TAG, "FirebaseRepositoryImpl: initToken")
        externalScope = scope
        externalScope?.launch(ioDispatcher + exceptionHandler) {
            Log.d(TAG, "FirebaseRepositoryImpl: scope.launch")
            val user = auth.currentUser
            if(user != null && !user.isAnonymous) {
                Log.d(TAG, "FirebaseRepositoryImpl: ${user.uid}")
                refreshIdToken(
                    onSuccess = {
                        //init firestore listener
                        _isFirebaseAuthSuccess.value = true
                        Log.w(TAG, "refreshIdToken:success")
                    },
                    onError = {
                        Log.w(TAG, "refreshIdToken:error")
                        _isFirebaseAuthSuccess.value = false
                        externalScope?.launch(ioDispatcher + exceptionHandler) {
                            renewCustomToken()
                        }
                    })
                Log.d(TAG, "FirebaseRepositoryImpl: existing user")
            }
            else {
                //renew the customToken
                Log.d(TAG, "FirebaseRepositoryImpl: anonymous")
                renewCustomToken()
            }
        }
    }

    private suspend fun renewCustomToken() {
        renewToken(
            onSuccess = { customToken ->
                customToken?.let {
                    signInWithCustomToken(customToken)
                }
            },
            onError = {
                //nothing
                Log.d(TAG, "renewCustomToken: error")
            }
        )
    }

    private fun signInWithCustomToken(customToken: String) {
        auth.signInWithCustomToken(customToken).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //refresh the Id token every one hour
                refreshIdToken(
                    onSuccess = {
                        _isFirebaseAuthSuccess.value = true
                        Log.d(TAG, "signInAnonymously:success ${task.result}")
                    },
                    onError = {
                        //nothing
                        _isFirebaseAuthSuccess.value = false
                    })
            } else {
                // If sign in fails, display a message to the user.
                Log.w(TAG, "signInAnonymously:failure ${task.exception}", task.exception)
                _isFirebaseAuthSuccess.value = false
            }
        }
    }

    private fun createSignedJwtRS256(licensePlate: String, privateKey: RSAPrivateKey, publicKey: RSAPublicKey): String {
        val algorithm = Algorithm.RSA256(publicKey, privateKey)

        return JWT.create()
            .withKeyId("CM-$licensePlate")
            .withExpiresAt(Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 hour expiration
            .withClaim("name", licensePlate)
            .withClaim("admin", true)
            .sign(algorithm)
    }

    private suspend fun renewToken(onSuccess: (String?) -> Unit, onError: (IOException) -> Unit) {
        val licensePlate = meterPreferenceRepository.getLicensePlate().first() ?: "CM-CABLE01"

        val body = createSignedJwtRS256(
            licensePlate,
            GlobalUtils.loadPrivateKey(PRIVATE_KEY),
            GlobalUtils.loadPublicKey(PUBLIC_KEY)
        )
        Log.d(TAG, "renewToken: $body")
        postApiCall(body, onSuccess, onError)
    }

    private suspend fun postApiCall(
        body: String,
        onSuccess: (String?) -> Unit,
        onError: (IOException) -> Unit
    ) {
        Log.d(TAG, "postApiCall - pre call")
        val mediaType = "text/plain".toMediaTypeOrNull()

        val requestBody = body.toRequestBody(mediaType)
        when(val response = meterOApiRepository.postFirebaseAuthToken(requestBody)) {
            is NetworkResult.Success -> {
                Log.d(TAG, "postApiCall - success")
                onSuccess(response.data.customToken)
            }
            is NetworkResult.Error -> {
                Log.d(TAG, "postApiCall - error")
                onError(IOException("Error getting custom token"))
            }
            is NetworkResult.Exception -> {
                Log.d(TAG, "postApiCall - exception")
                onError(IOException("Error getting custom token"))
            }
        }
    }

    private fun refreshIdToken(onSuccess: (String?) -> Unit, onError: (Exception?) -> Unit) {
        auth.currentUser?.getIdToken(false)?.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "signInAnonymously: currentUser: claims: ${it.result.claims}")
                onSuccess(it.result.token)
            } else {
                Log.d(
                    TAG,
                    "signInAnonymously: currentUser: failure: ${it.exception}"
                )
                onError(it.exception)
            }
        }
        if (auth.currentUser == null) {
            Log.d(TAG, "signInAnonymously: currentUser: null")
            onError(null)
        }
    }

    suspend fun getHeaders(): Map<String, String> {
        return suspendCoroutine { continuation ->
            val headers = mutableMapOf<String, String>()
            refreshIdToken(
                onSuccess = {
                    headers[AUTHORIZATION_HEADER] = "$BEARER $it"
                    continuation.resume(headers)
                    Log.d(TAG, "getHeaders:success")
                },
                onError = {
                    continuation.resume(headers)
                    Log.d(TAG, "getHeaders:error")
                }
            )
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthRepository"
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER = "Bearer"
    }
}