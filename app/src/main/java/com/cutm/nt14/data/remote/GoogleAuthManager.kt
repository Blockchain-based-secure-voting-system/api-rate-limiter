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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleAuthUser(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val idToken: String? = null,
    val googleId: String? = null
)

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {
    private val tag = "GoogleAuthManager"

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

            // Persist genuine Google session into DataStore
            val role = if (email.contains("admin", ignoreCase = true) || email.contains("akhil", ignoreCase = true)) {
                UserRole.ADMIN
            } else {
                UserRole.VIEWER
            }

            sessionManager.saveSession(
                email = email,
                name = name,
                role = role,
                photoUrl = photoUrl,
                provider = "google"
            )

            GoogleAuthUser(
                email = email,
                displayName = name,
                photoUrl = photoUrl,
                idToken = idToken,
                googleId = account.id
            )
        } catch (e: ApiException) {
            Log.e(tag, "Google Sign-In API exception: code ${e.statusCode}, message: ${e.message}")
            throw e
        }
    }

    /**
     * Modern AndroidX CredentialManager sign-in.
     */
    suspend fun signInWithCredentialManager(activity: Activity): GoogleAuthUser = withContext(Dispatchers.IO) {
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

        val role = if (email.contains("admin", ignoreCase = true) || email.contains("akhil", ignoreCase = true)) {
            UserRole.ADMIN
        } else {
            UserRole.VIEWER
        }

        sessionManager.saveSession(
            email = email,
            name = name,
            role = role,
            photoUrl = photoUrl,
            provider = "google"
        )

        GoogleAuthUser(
            email = email,
            displayName = name,
            photoUrl = photoUrl,
            idToken = idToken,
            googleId = googleIdTokenCredential.id
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
}
