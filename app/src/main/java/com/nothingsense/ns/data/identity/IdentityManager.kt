package com.nothingsense.ns.data.identity

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "identity_prefs")

@Singleton
class IdentityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val USERNAME_KEY = stringPreferencesKey("username")
    private val BIO_KEY = stringPreferencesKey("bio")
    private val AVATAR_URI_KEY = stringPreferencesKey("avatar_uri")
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    private val AUTO_DOWNLOAD_KEY = booleanPreferencesKey("auto_download_enabled")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    val userIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    val usernameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    val bioFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BIO_KEY] ?: "Explorando la red mesh offline de NoSense"
    }

    val avatarUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AVATAR_URI_KEY]
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }

    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED_KEY] ?: false
    }

    val autoDownloadFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_DOWNLOAD_KEY] ?: true
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: "NoSense Dark"
    }

    suspend fun getOrCreateUserId(): String {
        val currentId = userIdFlow.first()
        if (currentId != null) return currentId

        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = newId
        }
        return newId
    }

    suspend fun setUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }

    suspend fun setBio(bio: String) {
        context.dataStore.edit { preferences ->
            preferences[BIO_KEY] = bio
        }
    }

    suspend fun setAvatarUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[AVATAR_URI_KEY] = uri
            } else {
                preferences.remove(AVATAR_URI_KEY)
            }
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    suspend fun setAutoDownloadEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_DOWNLOAD_KEY] = enabled
        }
    }

    suspend fun setThemeMode(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = theme
        }
    }

    suspend fun getUsername(): String {
        val currentUsername = usernameFlow.first()
        if (currentUsername != null) return currentUsername

        val defaultUsername = "User_${UUID.randomUUID().toString().take(4)}"
        setUsername(defaultUsername)
        return defaultUsername
    }
}
