package com.vismo.nextgenmeter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.vismo.nextgenmeter.repository.NavigationLogger

/**
 * Observes navigation changes and logs page transitions
 *
 * @param navController The NavHostController to observe
 * @param navigationLogger The logger to use for tracking navigation
 */
@Composable
fun NavigationObserver(
    navController: NavHostController,
    navigationLogger: NavigationLogger
) {
    var previousRoute by remember { mutableStateOf<String?>(null) }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val currentRoute = destination.route

            if (currentRoute != null) {
                // Log navigation from previous to current route
                navigationLogger.logNavigation(
                    fromPage = previousRoute,
                    toPage = currentRoute
                )

                // Update previous route
                previousRoute = currentRoute
            }
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}
