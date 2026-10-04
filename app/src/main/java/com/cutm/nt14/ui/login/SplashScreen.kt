package com.cutm.nt14.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cutm.nt14.ui.components.*

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

    GlassBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NT14 GATEWAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolyPrimary,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rate Limit Optimizer",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolyTextPrimary
                )
                Spacer(modifier = Modifier.height(24.dp))

                CircularProgressIndicator(
                    color = PolyPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )

                if (isOnline == false) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Offline Mode (Local database active)",
                        color = PolyTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
