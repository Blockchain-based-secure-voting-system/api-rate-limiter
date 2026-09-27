package com.cutm.nt14.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cutm.nt14.ui.abuse.IncidentScreen
import com.cutm.nt14.ui.biometric.BiometricLockScreen
import com.cutm.nt14.ui.dashboard.DashboardScreen
import com.cutm.nt14.ui.endpoints.EndpointScreen
import com.cutm.nt14.ui.login.LoginScreen
import com.cutm.nt14.ui.login.LoginViewModel
import com.cutm.nt14.ui.login.SplashScreen
import com.cutm.nt14.ui.login.SplashViewModel
import com.cutm.nt14.ui.logs.LogScreen
import com.cutm.nt14.ui.ratelimits.RateLimitScreen
import com.cutm.nt14.ui.reports.ReportScreen

sealed class Screen(val route: String, val title: String = "", val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object BiometricLock : Screen("biometric_lock")
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Endpoints : Screen("endpoints", "Endpoints", Icons.Default.List)
    object Logs : Screen("logs", "Logs", Icons.Default.Info)
    object RateLimits : Screen("ratelimits", "Limits", Icons.Default.Settings)
    object Incidents : Screen("incidents", "Security", Icons.Default.Warning)
    object Reports : Screen("reports", "Reports", Icons.Default.Share)
}

@Composable
fun NT14NavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavScreens = listOf(Screen.Dashboard, Screen.Endpoints, Screen.Logs, Screen.RateLimits, Screen.Incidents, Screen.Reports)

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Splash.route && 
                currentRoute != Screen.Login.route && 
                currentRoute != Screen.BiometricLock.route) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                val viewModel: SplashViewModel = hiltViewModel()
                SplashScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToBiometricLock = {
                        navController.navigate(Screen.BiometricLock.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.BiometricLock.route) {
                BiometricLockScreen(
                    onUnlockSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.BiometricLock.route) { inclusive = true }
                        }
                    },
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.BiometricLock.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                val viewModel: LoginViewModel = hiltViewModel()
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Endpoints.route) { EndpointScreen() }
            composable(Screen.Logs.route) { LogScreen() }
            composable(Screen.RateLimits.route) { RateLimitScreen() }
            composable(Screen.Incidents.route) { IncidentScreen() }
            composable(Screen.Reports.route) { ReportScreen() }
        }
    }
}
