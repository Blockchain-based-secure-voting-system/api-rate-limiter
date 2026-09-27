package com.cutm.nt14.ui.biometric

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BiometricLockScreen(
    viewModel: BiometricLockViewModel = hiltViewModel(),
    onUnlockSuccess: () -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Automatically prompt biometric auth on screen entry
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock Icon",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "NT14 App Locked",
            style = MaterialTheme.typography.headlineMedium
        )

        if (!email.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Logged in as $email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Device-level security is active. Confirm your fingerprint or device PIN to continue.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState is BiometricLockUiState.Prompting) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Waiting for biometric verification...",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Button(
                onClick = {
                    if (activity != null) {
                        viewModel.authenticate(activity)
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Unlock with Fingerprint / PIN")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    viewModel.signOut(onSignOut)
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Sign out instead")
            }
        }

        if (uiState is BiometricLockUiState.Error) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = (uiState as BiometricLockUiState.Error).message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
