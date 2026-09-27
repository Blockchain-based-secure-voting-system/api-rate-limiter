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
    onNavigateToDashboard: () -> Unit
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == true) {
            onNavigateToDashboard()
        } else if (isLoggedIn == false) {
            onNavigateToLogin()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NT14 Optimizer",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isOnline == false) {
                Text(
                    text = "Offline. Please check your connection.",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
