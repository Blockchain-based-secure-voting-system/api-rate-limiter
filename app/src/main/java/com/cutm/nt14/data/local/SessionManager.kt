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

    val userEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL]
    }

    val userName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME]
    }

    val userRole: Flow<UserRole> = context.dataStore.data.map { prefs ->
        val roleStr = prefs[USER_ROLE] ?: UserRole.VIEWER.name
        UserRole.valueOf(roleStr)
    }

    suspend fun saveSession(email: String, name: String, role: UserRole = UserRole.ADMIN) {
        context.dataStore.edit { prefs ->
            prefs[USER_EMAIL] = email
            prefs[USER_NAME] = name
            prefs[USER_ROLE] = role.name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
