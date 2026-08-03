package com.nothingsense.ns.security

import android.content.Context
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.nothingsense.ns.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SecurityWipeManager"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "identity_prefs")

@Singleton
class SecurityWipeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    suspend fun wipeAllAppData(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.w(TAG, "🚨 INICIANDO BORRADO DE SEGURIDAD DE EMERGENCIA EN EL DISPOSITIVO 🚨")

            // 1. Clear Room Database
            database.clearAllTables()
            Log.d(TAG, "Base de datos Room eliminada.")

            // 2. Clear KeyStore Keys
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (keyStore.containsAlias("nosense_identity_key")) {
                    keyStore.deleteEntry("nosense_identity_key")
                }
                if (keyStore.containsAlias("nosense_ecdh_key")) {
                    keyStore.deleteEntry("nosense_ecdh_key")
                }
                Log.d(TAG, "Llaves AndroidKeyStore destruidas.")
            } catch (e: Exception) {
                Log.e(TAG, "Error destruyendo KeyStore", e)
            }

            // 3. Clear Files & Cache
            val receivedDir = File(context.filesDir, "received_files")
            if (receivedDir.exists()) {
                receivedDir.deleteRecursively()
            }
            context.cacheDir?.deleteRecursively()
            Log.d(TAG, "Archivos locales y caché eliminados.")

            // 4. Reset DataStore Preferences
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Log.d(TAG, "Preferencias DataStore reseteadas.")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error durante el borrado de seguridad de emergencia", e)
            false
        }
    }
}
