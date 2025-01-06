package com.vismo.nextgenmeter

import android.app.ActivityManager
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Process
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amap.api.location.AMapLocation
import com.google.firebase.firestore.GeoPoint
import com.ilin.util.AmapLocationUtils
import com.vismo.nextgenmeter.datastore.DeviceDataStore
import com.vismo.nextgenmeter.repository.UsbEventReceiver
import com.vismo.nextgenmeter.service.StorageBroadcastReceiver
import com.vismo.nextgenmeter.service.USBReceiverStatus
import com.vismo.nextgenmeter.service.UsbBroadcastReceiver
import com.vismo.nextgenmeter.ui.NavigationGraph
import com.vismo.nextgenmeter.ui.shared.GenericDialogContent
import com.vismo.nextgenmeter.ui.shared.GlobalDialog
import com.vismo.nextgenmeter.ui.shared.GlobalSnackbarDelegate
import com.vismo.nextgenmeter.ui.shared.GlobalToastHolder
import com.vismo.nextgenmeter.ui.theme.CableMeterTheme
import com.vismo.nextgenmeter.ui.theme.pastelGreen600
import com.vismo.nextgenmeter.ui.topbar.AppBar
import com.vismo.nextgenmeter.util.GlobalUtils.performVirtualTapFeedback
import com.vismo.nxgnfirebasemodule.model.AGPS
import com.vismo.nxgnfirebasemodule.model.GPS
import com.vismo.nxgnfirebasemodule.model.MeterLocation
import com.vismo.nxgnfirebasemodule.model.canBeSnoozed
import com.vismo.nxgnfirebasemodule.util.DashUtil
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


@AndroidEntryPoint
class MainActivity : ComponentActivity(), UsbEventReceiver {
    private val mainViewModel: MainViewModel by viewModels()
    private var navController: NavHostController? = null
    private var storageReceiver : StorageBroadcastReceiver? = null
    private var usbBroadcastReceiver: UsbBroadcastReceiver? = null

    private fun registerStorageReceiver() {
        storageReceiver = StorageBroadcastReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addDataScheme("file");
        }

        registerReceiver(storageReceiver, filter)
    }

    private fun registerUsbReceiver() {
        usbBroadcastReceiver = UsbBroadcastReceiver.newInstance(this)
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbBroadcastReceiver.USB_STATE_ACTION)
        }

        registerReceiver(usbBroadcastReceiver, filter)
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.startCommunicate()
        Log.d(TAG, "onResume: start communicating")
    }

    override fun onPause() {
        super.onPause()
        mainViewModel.stopCommunicate()
        Log.d(TAG, "onPause: stop communicating")
    }

    private fun enableLogcatPrint() {
        val pid = Process.myPid()
        // Whitelist process and set the logcat main buffer size to 5MB
        // val whiteList = "logcat -P '$pid'; logcat -b main -G 5M; logcat -b system -G 1M; logcat -b crash -G 1M"
        val whiteList = "logcat -P '$pid'; logcat -G 5M"
        Runtime.getRuntime().exec(whiteList).waitFor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        enableLogcatPrint()
        initObservers()
        startAMapLocation()
        listenToSignalStrength()
        setWifiStatus()
        registerStorageReceiver()
        registerUsbReceiver()
        SentryAndroid.init(this) { options ->
            options.environment = BuildConfig.FLAVOR.uppercase()
        }
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
                    val showSnackBar = mainViewModel.snackBarContent.collectAsState().value
                    LaunchedEffect(showSnackBar) {
                        if (showSnackBar != null) {
                            snackbarDelegate.showSnackbar(showSnackBar.second, showSnackBar.first)
                            mainViewModel.resetSnackBarContent()
                        }
                    }

                    val aValidUpdate = mainViewModel.aValidUpdate.collectAsState().value
                    val isTripInProgress = mainViewModel.isTripInProgress.collectAsState().value
                    val showUpdateDialog = remember { mutableStateOf(false) }
                    val isDialogShown = remember {
                        mutableStateOf(false)
                    }
                    showUpdateDialog.value = aValidUpdate != null && !isDialogShown.value && !isTripInProgress
                    val clearAppCache = mainViewModel.clearApplicationCache.collectAsState().value
                    if (clearAppCache) {
                        clearApplicationCache()
                        mainViewModel.setClearApplicationCache(false)
                    }
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
                                    lifecycleScope.launch {
                                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                                            val isTripInProgress = mainViewModel.isTripInProgress.first()
                                            if (!isTripInProgress && navController?.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                                navController!!.popBackStack()
                                            }
                                        }
                                    }
                                },
                                onLogoLongPress = {
                                    lifecycleScope.launch {
                                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                                            val isTripInProgress = mainViewModel.isTripInProgress.first()
                                            if (mainViewModel.showLoginToggle.first() == true && isCurrentScreenMeterOps() && !isTripInProgress && !isCurrentScreenUpdateApk()) {
                                                navigateToSplashScreen(alwaysNavigateToPair = true)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                        ) {
                            NavigationGraph(
                                navController = navController!!,
                                innerPadding = innerPadding,
                                snackbarDelegate = snackbarDelegate
                            )

                            GlobalToastHolder()
                            GlobalDialog(
                                showDialog = showUpdateDialog,
                                onDismiss = { /* do nothing */ },
                                content = {
                                    GenericDialogContent(
                                        title = "設備需升級軟件",
                                        message = aValidUpdate?.description ?: "",
                                        confirmButtonText = "立即更新",
                                        onConfirm = {
                                            // navigate to update
                                            navController?.navigate(NavigationDestination.UpdateApk.route) {
                                                navController?.graph?.id?.let { it1 -> popUpTo(it1) {
                                                    inclusive = true
                                                } } // Clear the backstack
                                                restoreState = true
                                                launchSingleTop = true
                                            }
                                        },
                                        confirmButtonColor = pastelGreen600,
                                        cancelButtonText = if(aValidUpdate?.canBeSnoozed() == true) "稍后更新" else null,
                                        onCancel = {
                                            mainViewModel.snoozeUpdate(update = aValidUpdate)
                                        },
                                        onDismiss = {
                                            showUpdateDialog.value = false
                                            isDialogShown.value = true
                                        }
                                    )
                                },
                                usePlatformDefaultWidth = false,
                                width = 350,
                                height = 200
                            )
                        }
                    }
                }
            }
        }
    }

    private fun clearApplicationCache() {
        (this.getSystemService(ACTIVITY_SERVICE) as? ActivityManager).let { activityManager ->
            Toast.makeText(this, "Clearing cache...Please restart device after", Toast.LENGTH_LONG).show()
            if (activityManager?.clearApplicationUserData() == true) {
                Log.d( TAG, "Cache cleared. Restarting app")
            } else {
                Log.d( TAG, "Failed to clear cache")
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.action == KeyEvent.ACTION_DOWN) {
            val scanCode = event.scanCode
            val repeatCount = event.repeatCount
            val isLongPress = event.isLongPress

            if (scanCode == 248 && repeatCount == 0 && !isLongPress && !isCurrentScreenMeterOps() && !isCurrentScreenUpdateApk()) {
                navigateToMeterOpsScreen() // navigate to the MeterOps screen from any other screen
                mainViewModel.emitBeepSound()
                performVirtualTapFeedback(window.decorView)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isPairScreenInBackStack(): Boolean {
        return try {
            navController?.getBackStackEntry(NavigationDestination.Pair.route)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun isCurrentScreenMeterOps(): Boolean {
        return try {
            navController?.currentDestination?.route == NavigationDestination.MeterOps.route
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun isCurrentScreenUpdateApk(): Boolean {
        return try {
            navController?.currentDestination?.route == NavigationDestination.UpdateApk.route
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    DeviceDataStore.mcuTime.collectLatest {
                        it?.let { time ->
                            setDeviceTime(time)
                        }
                    }
                }
                launch {
                    combine(
                        mainViewModel.showLoginToggle,
                        mainViewModel.showConnectionIconsToggle
                    ) { showLoginToggle, showConnectionIconsToggle ->
                        Pair(showLoginToggle, showConnectionIconsToggle)
                    }.collectLatest {
                        if ((it.first == true && it.second == true && !isPairScreenInBackStack()) || (it.first == false && isPairScreenInBackStack())) {
                            // clean up the back stack and navigate to splash screen
                           navigateToSplashScreen()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToSplashScreen(alwaysNavigateToPair: Boolean = false) {
        navController?.navigate(NavigationDestination.Splash.route.replace("{$SPLASH_ARG}", "$alwaysNavigateToPair")) {
            navController?.graph?.id?.let { it1 -> popUpTo(it1) {
                inclusive = true
            } } // Clear the backstack
            restoreState = true
            launchSingleTop = true // Prevent multiple instances
        }
    }

    private fun navigateToMeterOpsScreen() {
        navController?.navigate(NavigationDestination.MeterOps.route) {
            navController?.graph?.id?.let { it1 -> popUpTo(it1) {
                inclusive = true
            } } // Clear the backstack
            restoreState = true
            launchSingleTop = true
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

    override fun onDestroy() {
        super.onDestroy()
        storageReceiver?.let {
            unregisterReceiver(it)
        }
        usbBroadcastReceiver?.let {
            unregisterReceiver(it)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        const val SPLASH_ARG = "alwaysNavigateToPair"
        sealed class NavigationDestination(
            val route: String,
        ) {
            data object Splash : NavigationDestination("splash/?alwaysNavigateToPair={$SPLASH_ARG}")
            data object MeterOps : NavigationDestination("meterOps")
            data object Pair : NavigationDestination("pair")
            data object TripHistory : NavigationDestination("tripHistory")
            data object TripSummaryDashboard : NavigationDestination("tripSummaryDashboard")
            data object MCUSummaryDashboard : NavigationDestination("mcuSummaryDashboard")
            data object SystemPin : NavigationDestination("systemPin")
            data object AdminBasicEdit : NavigationDestination("adminBasicEdit")
            data object AdminAdvancedEdit : NavigationDestination("adminAdvancedEdit")
            data object AdjustBrightnessOrVolume : NavigationDestination("adjustBrightnessOrVolume")
            data object UpdateApk : NavigationDestination("updateApk")
        }
    }

    override fun onUsbDeviceChanged(isConnected: Boolean) {
        val status = if (isConnected) {
            USBReceiverStatus.Attached
        } else {
            USBReceiverStatus.Detached
        }
        DeviceDataStore.setUSBReceiverStatus(status)
    }
}
