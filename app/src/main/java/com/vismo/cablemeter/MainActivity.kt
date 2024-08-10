package com.vismo.cablemeter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vismo.cablemeter.ui.AppBar
import com.vismo.cablemeter.ui.meter.MeterOpsScreen
import com.vismo.cablemeter.ui.meter.MeterOpsViewModel
import com.vismo.cablemeter.ui.theme.CableMeterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

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
                    val navController = rememberNavController()
                    mainViewModel.updateBackButtonVisibility(
                        navController.previousBackStackEntry != null
                    )

                    Scaffold(
                        topBar = {
                            AppBar(
                                viewModel = mainViewModel,
                                onBackButtonClick = { navController.popBackStack() }
                            )
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = NavigationDestination.MeterOps.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
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
                    //TODO: dummy - remove later
                    mainViewModel.sendPrintCmd()
                }
            }
        }
    }


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

    companion object {
        private const val TAG = "MainActivity"

        sealed class NavigationDestination(
            val route: String,
        ) {
            data object MeterOps : NavigationDestination("meterOps")
        }
    }
}
