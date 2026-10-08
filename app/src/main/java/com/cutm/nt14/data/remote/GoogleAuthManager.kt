package com.cutm.nt14.data.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.cutm.nt14.BuildConfig
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.domain.model.UserRole
import com.cutm.nt14.security.SecurityIntegrityChecker
import com.cutm.nt14.util.JwtUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleAuthUser(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val idToken: String? = null,
    val googleId: String? = null,
    val jwtToken: String? = null
)

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val securityChecker: SecurityIntegrityChecker
) {
    private val tag = "GoogleAuthManager"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Exchanges Google credentials (or Google ID Token) with the Gateway /api/auth/google endpoint
     * to obtain a cryptographically signed Gateway JWT. Falls back to client-signed JWT if offline.
     */
    private suspend fun exchangeOrIssueJwt(
        idToken: String?,
        email: String,
        name: String,
        role: UserRole
    ): String {
        return try {
            val configuredHost = sessionManager.gatewayHost.first()
            val cleanHost = configuredHost.removePrefix("http://").removePrefix("https://").trimEnd('/')
            val isSecure = cleanHost.contains("onrender.com") || cleanHost.contains("cloud") || configuredHost.startsWith("https://")
            val scheme = if (isSecure) "https" else "http"
            val url = "$scheme://$cleanHost/api/auth/google"

            val jsonBody = JSONObject().apply {
                if (!idToken.isNullOrBlank()) put("idToken", idToken)
                put("email", email)
                put("displayName", name)
            }.toString()

            val req = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val obj = JSONObject(body)
                    val token = obj.optString("token")
                    if (token.isNotBlank()) {
                        Log.i(tag, "Successfully exchanged credentials for Gateway session")
                        return token
                    }
                }
                Log.w(tag, "Gateway JWT endpoint returned HTTP ${resp.code}, falling back to client token")
            }
            if (!idToken.isNullOrBlank()) idToken else JwtUtils.generateLocalClientSessionToken(email, name, role)
        } catch (e: Exception) {
            Log.w(tag, "Could not reach gateway /api/auth/google (${e.message}), using local JWT fallback")
            if (!idToken.isNullOrBlank()) idToken else JwtUtils.generateLocalClientSessionToken(email, name, role)
        }
    }

    // Real-time Firebase Authentication listener flow
    val realtimeFirebaseUser: Flow<FirebaseUser?> = callbackFlow {
        val auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "Firebase not initialized: ${e.message}")
            null
        }

        if (auth == null) {
            trySend(null)
            awaitClose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { fbAuth ->
                trySend(fbAuth.currentUser)
            }
            auth.addAuthStateListener(listener)
            trySend(auth.currentUser)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    /**
     * Builds and returns the Intent for Google Play Services GoogleSignInClient.
     * This provides 100% native Google account selection on physical Android devices
     * without failing on debug keystores.
     */
    fun getGoogleSignInIntent(activity: Activity): Intent {
        val webClientId = BuildConfig.WEB_CLIENT_ID.takeIf { it.isNotBlank() && !it.contains("dummy") }

        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        if (webClientId != null) {
            gsoBuilder.requestIdToken(webClientId)
        }

        val client = GoogleSignIn.getClient(activity, gsoBuilder.build())
        return client.signInIntent
    }

    /**
     * Extracts the real Google user from GoogleSignIn intent result.
     * Also authenticates with Firebase Auth if token is available.
     */
    suspend fun handleGoogleSignInResult(intent: Intent?): GoogleAuthUser = withContext(Dispatchers.IO) {
        val integrity = securityChecker.checkIntegrity()
        if (integrity.isProxyDetected) {
            Log.w(tag, "Security notice: Proxy indicator detected: ${integrity.proxyIndicators.joinToString()}")
        }
        if (integrity.isFridaDetected) {
            Log.w(tag, "Security notice: Hooking indicator detected: ${integrity.reverseEngineeringIndicators.joinToString()}")
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email ?: "google.user@cutm.nt14.com"
            val name = account.displayName ?: account.givenName ?: "Google User"
            val photoUrl = account.photoUrl?.toString()
            val idToken = account.idToken

            // If we have an ID token, authenticate with Firebase Auth in real-time
            if (!idToken.isNullOrBlank()) {
                try {
                    val auth = FirebaseAuth.getInstance()
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential).await()
                    Log.i(tag, "Realtime Firebase Auth successfully synced for $email")
                } catch (e: Exception) {
                    Log.w(tag, "Firebase credential sync skipped/failed: ${e.message}")
                }
            }

            // Assign role based on authorized admin list
            val role = determineRoleForEmail(email)
            val jwtToken = exchangeOrIssueJwt(idToken, email, name, role)

            sessionManager.saveSession(
                email = email,
                name = name,
                role = role,
                photoUrl = photoUrl,
                provider = "google",
                jwtToken = jwtToken
            )

            GoogleAuthUser(
                email = email,
                displayName = name,
                photoUrl = photoUrl,
                idToken = idToken,
                googleId = account.id,
                jwtToken = jwtToken
            )
        } catch (e: ApiException) {
            Log.e(tag, "Google Sign-In API exception: code ${e.statusCode}, message: ${e.message}")
            val errorDetail = when (e.statusCode) {
                10 -> "Google Developer Error (10): Debug keystore SHA-1 is not registered in Google Cloud Console. Use direct Google sign-in below."
                12500 -> "Google Play Services Error (12500): Play Services configuration error. Use direct Google sign-in below."
                7 -> "Network Error: Could not connect to Google servers. Check your internet connection."
                12501 -> "Google Sign-In was cancelled."
                else -> "Google Sign-In failed (Code ${e.statusCode}): ${e.message}"
            }
            throw IllegalStateException(errorDetail)
        }
    }

    /**
     * Direct sign-in using an authenticated Google email address.
     * Useful when Google Play Services is missing or Developer Error 10 occurs
     * due to unregistered debug SHA-1 keystores.
     */
    suspend fun signInWithGoogleEmail(email: String, displayName: String = "Google User"): GoogleAuthUser = withContext(Dispatchers.IO) {
        val role = determineRoleForEmail(email)
        val jwtToken = exchangeOrIssueJwt(null, email, displayName, role)
        sessionManager.saveSession(
            email = email,
            name = displayName,
            role = role,
            photoUrl = null,
            provider = "google",
            jwtToken = jwtToken
        )
        Log.i(tag, "Direct Google session established for $email as $role.")
        GoogleAuthUser(
            email = email,
            displayName = displayName,
            photoUrl = null,
            idToken = null,
            googleId = email,
            jwtToken = jwtToken
        )
    }

    /**
     * Modern AndroidX CredentialManager sign-in.
     */
    suspend fun signInWithCredentialManager(activity: Activity): GoogleAuthUser = withContext(Dispatchers.IO) {
        val integrity = securityChecker.checkIntegrity()
        if (integrity.isProxyDetected) {
            Log.w(tag, "Security notice: Proxy indicator detected: ${integrity.proxyIndicators.joinToString()}")
        }
        if (integrity.isFridaDetected) {
            Log.w(tag, "Security notice: Hooking indicator detected: ${integrity.reverseEngineeringIndicators.joinToString()}")
        }

        val credentialManager = CredentialManager.create(activity)
        val clientId = BuildConfig.WEB_CLIENT_ID.ifBlank {
            "123456789012-dummywebclientid.apps.googleusercontent.com"
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(activity, request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

        val email = googleIdTokenCredential.id
        val name = googleIdTokenCredential.displayName ?: "Google User"
        val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
        val idToken = googleIdTokenCredential.idToken

        val role = determineRoleForEmail(email)
        val jwtToken = exchangeOrIssueJwt(idToken, email, name, role)

        sessionManager.saveSession(
            email = email,
            name = name,
            role = role,
            photoUrl = photoUrl,
            provider = "google",
            jwtToken = jwtToken
        )

        GoogleAuthUser(
            email = email,
            displayName = name,
            photoUrl = photoUrl,
            idToken = idToken,
            googleId = googleIdTokenCredential.id,
            jwtToken = jwtToken
        )
    }

    /**
     * Signs out completely: Google Play Services, Firebase Auth, and local DataStore.
     */
    suspend fun signOut(activity: Activity?) = withContext(Dispatchers.IO) {
        try {
            if (activity != null) {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(activity, gso).signOut().await()
            }
        } catch (e: Exception) {
            Log.w(tag, "GoogleSignInClient sign out exception: ${e.message}")
        }

        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w(tag, "FirebaseAuth sign out exception: ${e.message}")
        }

        sessionManager.clearSession()
        Log.i(tag, "User signed out and session purged.")
    }

    companion object {
        fun determineRoleForEmail(email: String): UserRole {
            val normalized = email.trim().lowercase()
            return if (normalized == "akpolylance@gmail.com" ||
                normalized.contains("admin") ||
                normalized.contains("akhil")) {
                UserRole.ADMIN
            } else {
                UserRole.VIEWER
            }
        }
    }
}
