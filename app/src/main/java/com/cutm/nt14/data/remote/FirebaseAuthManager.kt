package com.cutm.nt14.data.remote

import android.content.Context
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class FirebaseAuthResult {
    data class Success(val email: String, val name: String, val role: UserRole, val token: String) : FirebaseAuthResult()
    data class Error(val message: String) : FirebaseAuthResult()
}

@Singleton
class FirebaseAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /**
     * Authenticate using Email & Password via Firebase Auth
     */
    suspend fun signInWithEmail(email: String, pass: String): FirebaseAuthResult {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: return FirebaseAuthResult.Error("User authentication failed.")
            processFirebaseUser(user)
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e.localizedMessage ?: "Email sign-in failed.")
        }
    }

    /**
     * Create a new User with Email & Password via Firebase Auth
     */
    suspend fun signUpWithEmail(email: String, pass: String, displayName: String): FirebaseAuthResult {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: return FirebaseAuthResult.Error("User creation failed.")

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdate).await()

            processFirebaseUser(user)
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e.localizedMessage ?: "Sign-up failed.")
        }
    }

    /**
     * Authenticate using a Google ID Token with Firebase
     */
    suspend fun signInWithGoogleIdToken(idToken: String): FirebaseAuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: return FirebaseAuthResult.Error("Google Firebase Auth failed.")
            processFirebaseUser(user)
        } catch (e: Exception) {
            FirebaseAuthResult.Error(e.localizedMessage ?: "Google Firebase Auth failed.")
        }
    }

    /**
     * Send Password Reset Email
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign Out
     */
    suspend fun signOut() {
        firebaseAuth.signOut()
        sessionManager.clearSession()
    }

    /**
     * Helper to extract JWT token, assign role (ADMIN for admin emails, VIEWER otherwise),
     * and save to Encrypted SessionManager.
     */
    private suspend fun processFirebaseUser(user: FirebaseUser): FirebaseAuthResult {
        val email = user.email ?: "user@cutm.nt14.com"
        val name = user.displayName ?: email.substringBefore("@")
        val tokenResult = user.getIdToken(true).await()
        val jwtToken = tokenResult.token ?: ""

        val role = if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.VIEWER

        sessionManager.saveSession(
            email = email,
            name = name,
            role = role,
            provider = "firebase",
            jwtToken = jwtToken
        )

        return FirebaseAuthResult.Success(email = email, name = name, role = role, token = jwtToken)
    }
}
