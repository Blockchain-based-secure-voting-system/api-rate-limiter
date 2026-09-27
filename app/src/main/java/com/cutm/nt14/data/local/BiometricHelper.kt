package com.cutm.nt14.data.local

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    data class Error(val errorCode: Int, val errString: String) : BiometricAuthResult()
    object Failed : BiometricAuthResult()
}

/**
 * Utility to unwrap ContextWrapper layers in Jetpack Compose to reach the FragmentActivity.
 */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Checks if device supports strong biometric, weak biometric (optical fingerprint on OnePlus),
     * or device credential (PIN/pattern).
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canStrong = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        if (canStrong == BiometricManager.BIOMETRIC_SUCCESS) return true

        val canWeak = biometricManager.canAuthenticate(BIOMETRIC_WEAK)
        if (canWeak == BiometricManager.BIOMETRIC_SUCCESS) return true

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    /**
     * Displays the BiometricPrompt dialog supporting both Class 3 (strong) and Class 2 (weak/optical)
     * sensors, along with device credential fallback.
     */
    fun showPrompt(
        activity: FragmentActivity,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(BiometricAuthResult.Error(errorCode, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Failed)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        // Try BIOMETRIC_STRONG or DEVICE_CREDENTIAL first
        val promptInfo = try {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock NT14 Gateway")
                .setSubtitle("Confirm fingerprint, face, or device PIN")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()
        } catch (e: Exception) {
            // Fallback for Class 2 optical sensors (e.g. OnePlus OxygenOS / ColorOS)
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock NT14 Gateway")
                .setSubtitle("Confirm fingerprint or face identity")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                .setNegativeButtonText("Cancel")
                .build()
        }

        prompt.authenticate(promptInfo)
    }

    /**
     * Creates intent for device lock PIN/Pattern screen via KeyguardManager
     * for 100% reliable system PIN unlock.
     */
    fun createDeviceCredentialIntent(): Intent? {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.createConfirmDeviceCredentialIntent(
            "Unlock NT14 Gateway",
            "Enter your phone PIN, pattern, or password"
        )
    }
}
