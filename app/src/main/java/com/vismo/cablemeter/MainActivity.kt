package com.vismo.cablemeter

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amap.api.location.AMapLocation
import com.google.firebase.firestore.GeoPoint
import com.ilin.util.AmapLocationUtils
import com.vismo.cablemeter.datastore.MCUParamsDataStore
import com.vismo.cablemeter.service.GlobalBackService
import com.vismo.cablemeter.ui.topbar.AppBar
import com.vismo.cablemeter.ui.NavigationGraph
import com.vismo.cablemeter.ui.shared.GlobalSnackbarDelegate
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
    private var navController: NavHostController? = null
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
                    navController = rememberNavController()
                    // Observe the currentBackStackEntry
                    val currentBackStackEntry by navController!!.currentBackStackEntryAsState()
                    LaunchedEffect(currentBackStackEntry) {
                        mainViewModel.updateBackButtonVisibility(
                            navController!!.previousBackStackEntry != null
                        )
                    }
                    val snackbarDelegate: GlobalSnackbarDelegate by remember { mutableStateOf(GlobalSnackbarDelegate()) }
                    val snackbarHostState = remember { SnackbarHostState() }
                    snackbarDelegate.apply {
                        sbHostState = snackbarHostState
                        coroutineScope = rememberCoroutineScope()
                    }
                    val isTripInProgress = mainViewModel.isTripInProgress.collectAsState().value
                    Scaffold(
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                snackbar = { data: SnackbarData ->
                                    Snackbar(
                                        snackbarData = data,
                                        containerColor = snackbarDelegate.snackbarBackgroundColor,
                                        contentColor = snackbarDelegate.snackbarContentColor
                                    )
                                }
                            )
                        },
                        topBar = {
                            AppBar(
                                viewModel = mainViewModel,
                                onBackButtonClick = {
                                    if (!isTripInProgress && navController?.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                        navController!!.popBackStack()
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        NavigationGraph(
                            navController = navController!!,
                            innerPadding = innerPadding,
                            snackbarDelegate = snackbarDelegate
                        )
                    }
                }
            }
        }
    }

    private fun isPairScreenInBackStack(): Boolean {
        return try {
            navController?.getBackStackEntry(NavigationDestination.Pair.route)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    MCUParamsDataStore.mcuTime.collectLatest {
                        it?.let { time ->
                            setDeviceTime(time)
                        }
                    }
                }
                launch {
                    mainViewModel.showLoginToggle.collectLatest {
                        if ((it == true && !isPairScreenInBackStack()) || (it == false && isPairScreenInBackStack())) {
                            // clean up the back stack and navigate to splash screen
                            navController?.navigate(NavigationDestination.Splash.route) {
                                navController?.graph?.id?.let { it1 -> popUpTo(it1) {
                                    inclusive = true
                                } } // Clear the backstack
                                restoreState = true
                                launchSingleTop = true // Prevent multiple instances
                            }
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

    override fun onPause() {
        super.onPause()
        val backButtonServiceIntent = Intent(this, GlobalBackService::class.java)
        ContextCompat.startForegroundService(this, backButtonServiceIntent)
    }

    override fun onResume() {
        super.onResume()
        val backButtonServiceIntent = Intent(this, GlobalBackService::class.java)
        stopService(backButtonServiceIntent)
    }

    companion object {
        private const val TAG = "MainActivity"

        sealed class NavigationDestination(
            val route: String,
        ) {
            data object Splash : NavigationDestination("splash")
            data object MeterOps : NavigationDestination("meterOps")
            data object Pair : NavigationDestination("pair")
            data object TripHistory : NavigationDestination("tripHistory")
            data object TripSummaryDashboard : NavigationDestination("tripSummaryDashboard")
            data object MCUSummaryDashboard : NavigationDestination("mcuSummaryDashboard")
            data object SystemPin : NavigationDestination("systemPin")
            data object AdminBasicEdit : NavigationDestination("adminBasicEdit")
            data object AdminAdvancedEdit : NavigationDestination("adminAdvancedEdit")
            data object AdjustBrightnessOrVolume : NavigationDestination("adjustBrightnessOrVolume")
        }
    }
}
