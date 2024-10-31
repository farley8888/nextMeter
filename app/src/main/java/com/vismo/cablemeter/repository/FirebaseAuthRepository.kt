package com.vismo.cablemeter.repository

import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.firebase.auth.FirebaseAuth
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.network.api.MeterOApi
import com.vismo.cablemeter.util.Constant.PRIVATE_KEY
import com.vismo.cablemeter.util.Constant.PUBLIC_KEY
import com.vismo.cablemeter.util.GlobalUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import java.io.IOException
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Date
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val meterOApi: MeterOApi
){
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _isFirebaseAuthSuccess: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFirebaseAuthSuccess: StateFlow<Boolean> = _isFirebaseAuthSuccess

    fun initToken() {
        Log.d(TAG, "FirebaseRepositoryImpl: initToken")
        scope.launch {
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
                        scope.launch {
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

    private fun createSignedJwtRS256(privateKey: RSAPrivateKey, publicKey: RSAPublicKey): String {
        val algorithm = Algorithm.RSA256(publicKey, privateKey)

        return JWT.create()
            .withKeyId("CM-CABLE01")
            .withExpiresAt(Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 hour expiration
            .withClaim("name", "CABLE01")
            .withClaim("admin", true)
            .sign(algorithm)
    }

    private suspend fun renewToken(onSuccess: (String?) -> Unit, onError: (IOException) -> Unit) {
        Log.d(TAG, "renewToken")
        val body = createSignedJwtRS256(
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

        val requestBody = RequestBody.create(mediaType, body)
        val response = meterOApi.postFirebaseAuthToken(requestBody)
        val responseBody = response.body()
        Log.d(TAG, "postApiCall - post call: ${response.code()}")
        if (response.isSuccessful && responseBody != null && responseBody.customToken != null) {
            Log.d(TAG, "Auth API - success: $responseBody")
            onSuccess(responseBody.customToken)
        } else {
            Log.d(TAG, "postApiCall - failure: ${response.code()}")
            onError(IOException("Error getting custom token"))

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
    }

    fun cancel() {
        scope.cancel()
    }

    companion object {
        private const val TAG = "FirebaseAuthRepository"
    }
}