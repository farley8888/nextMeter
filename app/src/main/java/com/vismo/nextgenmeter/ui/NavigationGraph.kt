package com.vismo.nextgenmeter.ui


import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vismo.nextgenmeter.MainActivity
import com.vismo.nextgenmeter.MainActivity.Companion.NavigationDestination
import com.vismo.nextgenmeter.ui.settings.admin.EditAdminPropertiesViewModel
import com.vismo.nextgenmeter.ui.settings.admin.advance.EditFareCalculationPropertiesScreen
import com.vismo.nextgenmeter.ui.settings.admin.basic.EditKValueAndLicensePlateScreen
import com.vismo.nextgenmeter.ui.dashboard.mcu.MCUSummaryDashboard
import com.vismo.nextgenmeter.ui.dashboard.mcu.MCUSummaryDashboardViewModel
import com.vismo.nextgenmeter.ui.dashboard.trip.TripSummaryDashboard
import com.vismo.nextgenmeter.ui.dashboard.trip.TripSummaryDashboardViewModel
import com.vismo.nextgenmeter.ui.history.LocalTripHistoryScreen
import com.vismo.nextgenmeter.ui.history.LocalTripHistoryViewModel
import com.vismo.nextgenmeter.ui.meter.MeterOpsScreen
import com.vismo.nextgenmeter.ui.meter.MeterOpsViewModel
import com.vismo.nextgenmeter.ui.pair.DriverPairScreen
import com.vismo.nextgenmeter.ui.pair.DriverPairViewModel
import com.vismo.nextgenmeter.ui.pin.SystemPinScreen
import com.vismo.nextgenmeter.ui.pin.SystemPinViewModel
import com.vismo.nextgenmeter.ui.settings.AdjustBrightnessOrVolumeScreen
import com.vismo.nextgenmeter.ui.settings.AdjustBrightnessOrVolumeViewModel
import com.vismo.nextgenmeter.ui.shared.GlobalSnackbarDelegate
import com.vismo.nextgenmeter.ui.splash.SplashScreen
import com.vismo.nextgenmeter.ui.splash.SplashScreenViewModel
import com.vismo.nextgenmeter.ui.update.UpdateScreen
import com.vismo.nextgenmeter.ui.update.UpdateViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    snackbarDelegate: GlobalSnackbarDelegate,
) {
    // Shared ViewModels
    val editAdminPropertiesViewModel = hiltViewModel<EditAdminPropertiesViewModel>()

    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Splash.route,
        modifier = Modifier.padding(innerPadding),
        enterTransition = {
            EnterTransition.None
        },
        exitTransition = {
            ExitTransition.None
        }
    ) {
        composable(route = NavigationDestination.Splash.route, arguments = listOf(navArgument(MainActivity.SPLASH_ARG) {
            defaultValue = false
            type = NavType.BoolType
        })) {
            val alwaysNavigateToPair = it.arguments?.getBoolean(MainActivity.SPLASH_ARG) ?: false
            val viewModel = hiltViewModel<SplashScreenViewModel>()
            SplashScreen(viewModel, navigateToPair = {
                navController.navigate(NavigationDestination.Pair.route) {
                    navController.graph.id.let { it1 -> popUpTo(it1) {
                        inclusive = true
                    } } // Clear the backstack
                    restoreState = true
                    launchSingleTop = true
                }
            }, navigateToMeterOps = {
                navController.navigate(NavigationDestination.MeterOps.route) {
                    navController.graph.id.let { it1 -> popUpTo(it1) {
                        inclusive = true
                    } } // Clear the backstack
                    restoreState = true
                    launchSingleTop = true
                }
            },
                alwaysNavigateToPair = alwaysNavigateToPair
                )
        }
        composable(NavigationDestination.MeterOps.route) {
            val viewModel = hiltViewModel<MeterOpsViewModel>()
            MeterOpsScreen(viewModel, snackbarDelegate,
                navigateToDashBoard = {
                navController.navigate(NavigationDestination.TripSummaryDashboard.route)
            })
        }
        composable(NavigationDestination.Pair.route) {
            val viewModel = hiltViewModel<DriverPairViewModel>()
            DriverPairScreen(viewModel, snackbarDelegate,
                navigateToMeterOps = {
                navController.navigate(NavigationDestination.MeterOps.route) {
                    popUpTo(NavigationDestination.Pair.route) { inclusive = true }
                    restoreState = true
                    launchSingleTop = true
                }
            })
        }
        composable(NavigationDestination.TripHistory.route) {
            val viewModel = hiltViewModel<LocalTripHistoryViewModel>()
            LocalTripHistoryScreen(viewModel)
        }
        composable(NavigationDestination.TripSummaryDashboard.route) {
            val viewModel = hiltViewModel<TripSummaryDashboardViewModel>()
            TripSummaryDashboard(viewModel, navigateToTripHistory = {
                navController.navigate(NavigationDestination.TripHistory.route)
            }, navigateToAdjustBrightnessOrVolume = {
               navController.navigate(NavigationDestination.AdjustBrightnessOrVolume.route)
            }, navigateToMCUSummary = {
                navController.navigate(NavigationDestination.MCUSummaryDashboard.route)
            })
        }
        composable(NavigationDestination.MCUSummaryDashboard.route) {
            val viewModel = hiltViewModel<MCUSummaryDashboardViewModel>()
            MCUSummaryDashboard(viewModel, navigate = {
                navController.navigate(NavigationDestination.SystemPin.route)
            })
        }
        composable(NavigationDestination.SystemPin.route) {
            SystemPinScreen(viewModel = hiltViewModel<SystemPinViewModel>(), navigate = {
                navController.navigate(NavigationDestination.AdminBasicEdit.route)
            })
        }
        composable(NavigationDestination.AdminBasicEdit.route) {
            EditKValueAndLicensePlateScreen(
                viewModel = editAdminPropertiesViewModel,
                snackbarDelegate = snackbarDelegate,
                navigateToAdminAdvancedEdit = {
                navController.navigate(NavigationDestination.AdminAdvancedEdit.route)
            })
        }
        composable(NavigationDestination.AdminAdvancedEdit.route) {
            EditFareCalculationPropertiesScreen(
                viewModel = editAdminPropertiesViewModel,
                snackbarDelegate = snackbarDelegate
            )
        }
        composable(NavigationDestination.AdjustBrightnessOrVolume.route) {
            AdjustBrightnessOrVolumeScreen(viewModel = hiltViewModel<AdjustBrightnessOrVolumeViewModel>())
        }
        composable(NavigationDestination.UpdateApk.route) {
            UpdateScreen(viewModel = hiltViewModel<UpdateViewModel>())
        }
    }
}