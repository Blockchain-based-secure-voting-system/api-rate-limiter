package com.cutm.nt14.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToBiometricLock: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val destination by viewModel.destination.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    LaunchedEffect(destination) {
        when (destination) {
            is SplashDestination.Login -> onNavigateToLogin()
            is SplashDestination.BiometricLock -> onNavigateToBiometricLock()
            is SplashDestination.Dashboard -> onNavigateToDashboard()
            null -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NT14 Optimizer",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressIndicator()

            if (isOnline == false) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Offline Mode (Local database active)",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
