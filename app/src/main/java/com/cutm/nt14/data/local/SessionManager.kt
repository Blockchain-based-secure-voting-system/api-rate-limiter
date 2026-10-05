package com.cutm.nt14.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cutm.nt14.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val USER_EMAIL = stringPreferencesKey("user_email")
    private val USER_NAME = stringPreferencesKey("user_name")
    private val USER_ROLE = stringPreferencesKey("user_role")
    private val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
    private val AUTH_PROVIDER = stringPreferencesKey("auth_provider")

    val userEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL]
    }

    val userName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME]
    }

    val userPhotoUrl: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_PHOTO_URL]
    }

    val authProvider: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[AUTH_PROVIDER]
    }

    val userRole: Flow<UserRole> = context.dataStore.data.map { prefs ->
        val roleStr = prefs[USER_ROLE] ?: UserRole.VIEWER.name
        UserRole.valueOf(roleStr)
    }

    private val GATEWAY_HOST = stringPreferencesKey("gateway_host")

    val gatewayHost: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GATEWAY_HOST] ?: "10.0.2.2:8000"
    }

    suspend fun saveGatewayHost(host: String) {
        context.dataStore.edit { prefs ->
            prefs[GATEWAY_HOST] = host
        }
    }

    suspend fun saveSession(
        email: String,
        name: String,
        role: UserRole = UserRole.ADMIN,
        photoUrl: String? = null,
        provider: String = "google"
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_EMAIL] = email
            prefs[USER_NAME] = name
            prefs[USER_ROLE] = role.name
            if (photoUrl != null) {
                prefs[USER_PHOTO_URL] = photoUrl
            } else {
                prefs.remove(USER_PHOTO_URL)
            }
            prefs[AUTH_PROVIDER] = provider
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
