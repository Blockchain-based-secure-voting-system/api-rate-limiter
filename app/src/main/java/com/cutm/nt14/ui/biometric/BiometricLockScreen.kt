package com.cutm.nt14.ui.biometric

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.cutm.nt14.ui.components.*

@Composable
fun BiometricLockScreen(
    viewModel: BiometricLockViewModel = hiltViewModel(),
    onUnlockSuccess: () -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val context = LocalContext.current

    // Traverses any ContextWrapper layers (such as Hilt/Theme wrappers) to get the real FragmentActivity
    val activity = remember(context) {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is FragmentActivity) break
            c = c.baseContext
        }
        c as? FragmentActivity
    }

    // System Device PIN / Pattern activity launcher as a 100% reliable fallback
    val pinLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onBiometricSuccess()
        }
    }

    // Automatically prompt biometric auth on screen entry once activity is ready
    LaunchedEffect(activity) {
        if (activity != null && uiState is BiometricLockUiState.Idle) {
            viewModel.authenticate(activity)
        }
    }

    // React to successful authentication
    LaunchedEffect(uiState) {
        if (uiState is BiometricLockUiState.Success) {
            onUnlockSuccess()
        }
    }

    GlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF141829).copy(alpha = 0.82f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AppleCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Icon",
                            modifier = Modifier.size(36.dp),
                            tint = AppleCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "NT14 LOCKED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleCyan,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Biometric Security",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (!email.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Logged in as $email",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Device-level biometric or PIN verification required to access live gateway controls.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (uiState is BiometricLockUiState.Prompting) {
                        CircularProgressIndicator(color = AppleCyan)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Waiting for fingerprint / face / PIN...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    } else {
                        GlassButton(
                            text = "Unlock with Fingerprint",
                            accentColor = AppleBlue,
                            onClick = {
                                if (activity != null) {
                                    viewModel.authenticate(activity)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassButton(
                            text = "Unlock with Device PIN / Pattern",
                            accentColor = AppleCyan,
                            onClick = {
                                val pinIntent = viewModel.getDeviceCredentialIntent()
                                if (pinIntent != null) {
                                    pinLauncher.launch(pinIntent)
                                } else if (activity != null) {
                                    viewModel.authenticate(activity)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassButton(
                            text = "Sign out instead",
                            accentColor = Color.White.copy(alpha = 0.5f),
                            onClick = {
                                viewModel.signOut(onSignOut)
                            },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    }

                    if (uiState is BiometricLockUiState.Error) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = (uiState as BiometricLockUiState.Error).message,
                            color = AppleRed,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
