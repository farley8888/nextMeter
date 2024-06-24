package com.vismo.cablemeter

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vismo.cablemeter.ui.meter.MeterOpsScreen
import com.vismo.cablemeter.ui.meter.MeterOpsViewModel
import com.vismo.cablemeter.ui.theme.CableMeterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObservers()
        setContent {
            CableMeterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = NavigationDestination.MeterOps.route) {
                            composable(NavigationDestination.MeterOps.route) {
                                val viewModel = hiltViewModel<MeterOpsViewModel>()
                                MeterOpsScreen(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.measureBoardData.collect { measureBoardData ->
                        measureBoardData?.let {

                        }
                    }
                }
            }
        }
    }
//
//    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
//        val code = event.scanCode
//
//        fun isWithinRange(code: Int): Boolean {
//            return code in 248..255
//        }
//
//        fun isLongPress(repeatCount: Int): Boolean {
//            return repeatCount > 0
//        }
//
//        if (isWithinRange(code)) {
//            if (isLongPress(event.repeatCount)) {
//                onButtonLongPressed(code, event.repeatCount)
//            } else {
//                onButtonPressed(code)
//            }
//        }
//        return super.onKeyDown(keyCode, event)
//
//    }

    private fun onButtonPressed(code: Int) {
        when(code) {
            248 -> {
                // ready for hire
                Log.d(TAG, "onButtonPressed: ready for hire")
            }
            249 -> {
                // start/resume trip
            }
            250 -> {
                // pause trip
            }
            253 -> {
                // add extras - $10
            }
            254 -> {
                // add extras - $1
            }
            255 -> {
                // print receipt
            }
        }
    }

    private fun onButtonLongPressed(code: Int, repeatCount: Int) {

    }

    companion object {
        private const val TAG = "MainActivity"

        sealed class NavigationDestination(
            val route: String,
        ) {
            data object MeterOps : NavigationDestination("meterOps")
        }
    }
}
