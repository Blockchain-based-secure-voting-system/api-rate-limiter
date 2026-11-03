package com.cutm.nt14.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cutm.nt14.BuildConfig
import com.cutm.nt14.R
import com.cutm.nt14.ui.components.*
import com.cutm.nt14.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        var c: android.content.Context? = context
        while (c is android.content.ContextWrapper) {
            if (c is android.app.Activity) return@remember c
            c = c.baseContext
        }
        null
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
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Nt14Ui.Accent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security Shield",
                            tint = Nt14Ui.Accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "NT14 API GATEWAY",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Nt14Ui.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Realtime Rate Limiter & Abuse Prevention",
                        fontSize = 12.sp,
                        color = Nt14Ui.TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Enter directly as Admin or Guest Viewer, or authenticate with your Google account.",
                        fontSize = 13.sp,
                        color = Nt14Ui.TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Nt14Ui.Accent,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Authenticating session...",
                            fontSize = 12.sp,
                            color = Nt14Ui.TextMuted
                        )
                    } else {
                        // 1. Direct Admin Bypass
                        CardPillButton(
                            text = "Enter as Admin (Direct)",
                            icon = Icons.Default.Lock,
                            containerColor = Nt14Ui.Accent,
                            contentColor = Color.White,
                            borderColor = null,
                            onClick = { viewModel.signInAsAdmin() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Direct Guest Viewer Bypass
                        CardPillButton(
                            text = "Enter as Guest (Viewer)",
                            icon = Icons.Default.AccountCircle,
                            containerColor = Nt14Ui.TileFill,
                            contentColor = Nt14Ui.TextPrimary,
                            borderColor = Nt14Ui.Outline,
                            onClick = { viewModel.signInAsGuest() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Optional Google Sign-In divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Nt14Ui.Outline.copy(alpha = 0.6f))
                            Text(
                                text = "  OR GOOGLE SIGN-IN  ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Nt14Ui.TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Nt14Ui.Outline.copy(alpha = 0.6f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                if (activity != null) {
                                    viewModel.signInWithCredentialManager(activity)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Nt14Ui.Outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Nt14Ui.TextMuted
                            )
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Nt14Ui.TextMuted)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Sign In with Google Account",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (uiState is LoginUiState.Error) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as LoginUiState.Error).message,
                            color = Nt14Ui.Blocked,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    if (BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(24.dp))
                        val sha1 = remember(context) { getSigningCertificateSha1(context) }
                        Surface(
                            color = Nt14Ui.TileFill,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Nt14Ui.Outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "DEBUG OAUTH CONFIG",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Nt14Ui.Accent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Package: ${context.packageName}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Nt14Ui.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "SHA-1: $sha1",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Nt14Ui.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add this SHA-1 and package name to Google Cloud Console > Credentials > Android client.",
                                    fontSize = 10.sp,
                                    color = Nt14Ui.TextMuted,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getSigningCertificateSha1(context: android.content.Context): String {
    return try {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
        }
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        val cert = signatures?.firstOrNull()?.toByteArray() ?: return "Unavailable"
        val md = java.security.MessageDigest.getInstance("SHA-1")
        val digest = md.digest(cert)
        digest.joinToString(":") { "%02X".format(it) }
    } catch (e: Exception) {
        "Error: ${e::class.java.simpleName}"
    }
}
