package com.vismo.cablemeter.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.vismo.cablemeter.module.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class InternetConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class Status {
        InternetAvailable, InternetUnavailable
    }

    val internetStatus: Flow<Status> = observeInternetStatus()

    private fun observeInternetStatus(): Flow<Status> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                // Check for NET_CAPABILITY_VALIDATED
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (isValidated) {
                    trySend(Status.InternetAvailable)
                } else {
                    trySend(Status.InternetUnavailable)
                }
                Log.d(TAG, "Internet capabilities changed - validated: $isValidated")
            }

            override fun onLost(network: Network) {
                trySend(Status.InternetUnavailable)
                Log.d(TAG, "Internet lost")
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        Log.d(TAG, "Network callback registered")

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(TAG, "Network callback unregistered")
        }
    }

    companion object {
        private const val TAG = "InternetConnectivityObserver"
    }
}