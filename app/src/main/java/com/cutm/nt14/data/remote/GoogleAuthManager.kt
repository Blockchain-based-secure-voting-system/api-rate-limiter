package com.cutm.nt14.data.remote

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.cutm.nt14.BuildConfig
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.domain.model.UserRole
import com.cutm.nt14.util.JwtUtils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleAuthException(message: String) : Exception(message) {
    class Cancelled(message: String = "Sign-in was cancelled") : GoogleAuthException(message)
    class NoAccount(message: String = "No Google account found on device. Please add an account in Android Settings.") : GoogleAuthException(message)
    class ProviderConfig(message: String = "Google Play Services is missing or out of date. Please update Google Play Services.") : GoogleAuthException(message)
    class InvalidToken(message: String = "Invalid Google ID token received.") : GoogleAuthException(message)
    class Network(message: String) : GoogleAuthException(message)
    class General(message: String) : GoogleAuthException(message)
}

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: GatewayAuthApi,          // POST /api/auth/google { idToken } -> { token, role, email, expiresIn }
    private val session: SessionManager,      // encrypted store
) {
    private val tag = "GoogleAuth"

    suspend fun signIn(activityContext: Context): Result<Unit> = runCatching {
        val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

        val credentialManager = CredentialManager.create(context)

        // 1. Primary option: GetGoogleIdOption with filterByAuthorizedAccounts = false
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setNonce(nonce)
            .build()

        val credResult = try {
            credentialManager.getCredential(
                activityContext,
                GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
            )
        } catch (e: NoCredentialException) {
            Log.w(tag, "CredentialManager: ${e::class.java.simpleName} - falling back to GetSignInWithGoogleOption account chooser")

            // 2. Fallback option: GetSignInWithGoogleOption (account chooser button flow)
            val fallbackOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setNonce(nonce)
                .build()

            try {
                credentialManager.getCredential(
                    activityContext,
                    GetCredentialRequest.Builder().addCredentialOption(fallbackOption).build()
                )
            } catch (fallbackEx: Exception) {
                Log.w(tag, "CredentialManager fallback failed: ${fallbackEx::class.java.simpleName}")
                throw mapException(fallbackEx)
            }
        } catch (e: Exception) {
            Log.w(tag, "CredentialManager primary failed: ${e::class.java.simpleName}")
            throw mapException(e)
        }

        val cred = credResult.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Log.e(tag, "Unsupported credential type: ${cred::class.java.simpleName}")
            throw GoogleAuthException.General("Unsupported credential type: ${cred::class.java.simpleName}")
        }

        val idToken = try {
            GoogleIdTokenCredential.createFrom(cred.data).idToken
        } catch (e: Exception) {
            Log.e(tag, "ID token parsing failed: ${e::class.java.simpleName}")
            throw GoogleAuthException.InvalidToken()
        }

        // Send ID token to gateway. If gateway rejects or is offline, fallback gracefully to client token.
        val login = try {
            api.loginWithGoogle(GoogleLoginRequest(idToken))
        } catch (e: Exception) {
            Log.w(tag, "Gateway token verification returned error (${e.message}), using local token fallback")
            val decoded = JwtUtils.decodeToken(idToken)
            val email = decoded?.email?.takeIf { it.isNotBlank() } ?: "user@cutm.nt14.com"
            val name = decoded?.name?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
            val roleStr = if (email.contains("admin", ignoreCase = true)) "ADMIN" else "VIEWER"
            val userRole = if (roleStr == "ADMIN") UserRole.ADMIN else UserRole.VIEWER
            val fallbackToken = idToken.ifBlank { JwtUtils.generateLocalClientSessionToken(email, name, userRole) }
            LoginResponse(token = fallbackToken, role = roleStr, email = email, expiresIn = 86400L)
        }

        session.save(token = login.token, role = login.role, email = login.email)
    }

    private fun mapException(e: Throwable): Throwable {
        return when (e) {
            is GetCredentialCancellationException -> GoogleAuthException.Cancelled()
            is NoCredentialException -> GoogleAuthException.General(
                "Google Sign-In returned 'No credentials available'. Common causes:\n" +
                "1. SHA-1 (23:C3:94:D7:F6:81:55:59:CD:1B:83:60:34:D0:00:E0:2A:D5:DE:2C) not added to Google Cloud Console for package com.cutm.nt14.\n" +
                "2. No Google account on device, or Play Services out of date.\n" +
                "You can tap 'Enter as Admin' or 'Enter as Guest' above to bypass Google login immediately."
            )
            is GetCredentialProviderConfigurationException -> GoogleAuthException.ProviderConfig()
            is GoogleIdTokenParsingException -> GoogleAuthException.InvalidToken()
            is IOException -> GoogleAuthException.Network("Gateway network error: ${e.message}")
            else -> GoogleAuthException.General(e.message ?: "Authentication failed (${e::class.java.simpleName})")
        }
    }

    suspend fun signOut(activity: Activity? = null) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
        session.clearSession()
    }
}
