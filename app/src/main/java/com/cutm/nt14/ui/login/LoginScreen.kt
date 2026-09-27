package com.cutm.nt14.ui.login

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cutm.nt14.ui.components.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        val fa = with(com.cutm.nt14.data.local.BiometricHelper::class.java) {
            var c: android.content.Context? = context
            while (c is android.content.ContextWrapper) {
                if (c is androidx.fragment.app.FragmentActivity) break
                c = c.baseContext
            }
            c as? androidx.fragment.app.FragmentActivity
        }
        fa ?: (context as? android.app.Activity)
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
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
                backgroundColor = Color.White.copy(alpha = 0.94f),
                elevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PolyPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security Gateway",
                            tint = PolyPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "NT14 GATEWAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolyPrimary,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Real-Time Rate Limiter",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolyTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Sign in to monitor live traffic and manage gateway security policies.",
                        fontSize = 13.sp,
                        color = PolyTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(color = PolyPrimary)
                    } else {
                        GlassButton(
                            text = "Sign in with Google",
                            accentColor = PolyPrimary,
                            isFilled = true,
                            onClick = { viewModel.signIn(activity) },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassButton(
                            text = "Continue as Guest (Admin)",
                            accentColor = PolyTextSecondary,
                            isFilled = false,
                            onClick = { viewModel.signInAsGuest() },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    }

                    if (uiState is LoginUiState.Error) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as LoginUiState.Error).message,
                            color = PolyDanger,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
