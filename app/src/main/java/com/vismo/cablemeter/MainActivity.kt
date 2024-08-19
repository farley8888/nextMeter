package com.vismo.cablemeter

import android.app.AlarmManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amap.api.location.AMapLocation
import com.google.firebase.firestore.GeoPoint
import com.ilin.util.AmapLocationUtils
import com.vismo.cablemeter.ui.topbar.AppBar
import com.vismo.cablemeter.ui.NavigationGraph
import com.vismo.cablemeter.ui.theme.CableMeterTheme
import com.vismo.nxgnfirebasemodule.model.AGPS
import com.vismo.nxgnfirebasemodule.model.GPS
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import com.vismo.nxgnfirebasemodule.util.DashUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObservers()
        startAMapLocation()
        listenToSignalStrength()
        setWifiStatus()
        setContent {
            CableMeterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    // Observe the currentBackStackEntry
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    LaunchedEffect(currentBackStackEntry) {
                        mainViewModel.updateBackButtonVisibility(
                            navController.previousBackStackEntry != null
                        )
                    }

                    Scaffold(
                        topBar = {
                            AppBar(
                                viewModel = mainViewModel,
                                onBackButtonClick = { navController.popBackStack() }
                            )
                        }
                    ) { innerPadding ->
                        NavigationGraph(
                            navController = navController,
                            innerPadding = innerPadding
                        )
                    }
                }
            }
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.mcuTime.collectLatest {
                        it?.let { time ->
                            setDeviceTime(time)
                        }
                    }
                }
            }
        }
    }

    private fun setDeviceTime(mcuTimeStr: String) {
        try {
            val formatter = SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH)
            // set time zone to hong kong time
            formatter.timeZone = TimeZone.getTimeZone("Asia/Hong_Kong")

            val newTimeMillis = formatter.parse(mcuTimeStr)?.time
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (newTimeMillis != null) {
                alarmManager.setTime(newTimeMillis)
            }
        } catch (e: Exception) {
            Log.e(TAG,"Error setting device time: $e")
        }
    }

    private val signalStrengthListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            super.onSignalStrengthsChanged(signalStrength)
            // Check the signal strength values
            val level = signalStrength?.level ?: 0
            mainViewModel.updateSignalStrength(level)
        }
    }

    private fun listenToSignalStrength() {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    private fun setWifiStatus() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mainViewModel.setWifiIconVisibility(wifiManager.isWifiEnabled)
    }


    private fun startAMapLocation() {
        AmapLocationUtils.getInstance().init(applicationContext)
        AmapLocationUtils.getInstance()
            .setLocationListener { aMapLocation: AMapLocation ->
                val infoString = AmapLocationUtils.getInstance().getInfo(aMapLocation, false)
                Log.d(TAG, "onLocationChanged: $infoString")

                val lat = aMapLocation?.latitude
                val lng = aMapLocation?.longitude
                val bearing = aMapLocation?.bearing?.toDouble() ?: 0.0
                val speed = aMapLocation?.speed?.toDouble() ?: 0.0

                val location = if (lat != null && lng != null) {
                    val convertedLatLng = DashUtil.gcj2Wgs(lat, lng)
                    GeoPoint(convertedLatLng.first, convertedLatLng.second)
                } else {
                    null
                }

                // Convert the result to a JSONObject
                val jsonObject = JSONObject(infoString)

                // Retrieve the locType
                val locType = jsonObject.getInt("locType")
                Log.d(TAG, "onLocationChanged: locType = $locType")

                when (locType) {
                    1 -> {
                        location?.let {
                            mainViewModel.setLocation(
                                MeterLocation(
                                    geoPoint = it,
                                    gpsType = GPS(
                                        speed = speed,
                                        bearing = bearing
                                    )
                                )
                            )
                        }
                    }

                    else -> {
                        location?.let {
                            mainViewModel.setLocation(
                                MeterLocation(
                                    geoPoint = it,
                                    gpsType = AGPS(
                                        speed = speed,
                                        bearing = bearing
                                    )
                                )
                            )
                        }
                    }
                }
            }
        AmapLocationUtils.getInstance().startLocation()
    }

    companion object {
        private const val TAG = "MainActivity"

        sealed class NavigationDestination(
            val route: String,
        ) {
            data object MeterOps : NavigationDestination("meterOps")
            data object Pair : NavigationDestination("pair")
            data object TripHistory : NavigationDestination("tripHistory")
        }
    }
}
