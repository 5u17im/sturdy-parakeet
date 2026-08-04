package com.nothingsense.ns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nothingsense.ns.ui.navigation.NavGraph
import com.nothingsense.ns.ui.theme.NoSenseTheme
import dagger.hilt.android.AndroidEntryPoint

import android.view.WindowManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.nothingsense.ns.data.identity.IdentityManager
import javax.inject.Inject

import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nothingsense.ns.security.BiometricAuthManager
import com.nothingsense.ns.ui.security.BiometricLockOverlayScreen

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var identityManager: IdentityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isFlagSecureEnabled by identityManager.flagSecureEnabledFlow.collectAsState(initial = false)
            val isBiometricEnabled by identityManager.biometricEnabledFlow.collectAsState(initial = false)
            var isAppUnlocked by remember { mutableStateOf(false) }

            LaunchedEffect(isFlagSecureEnabled) {
                if (isFlagSecureEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && !isAppUnlocked) {
                    BiometricAuthManager.promptBiometricAuth(
                        activity = this@MainActivity,
                        title = "Desbloquear NoSense",
                        subtitle = "Escanea tu huella dactilar para acceder a tus chats",
                        onSuccess = { isAppUnlocked = true },
                        onError = { _ -> }
                    )
                }
            }

            NoSenseTheme {
                if (isBiometricEnabled && !isAppUnlocked) {
                    BiometricLockOverlayScreen(
                        onUnlockSuccess = { isAppUnlocked = true }
                    )
                } else {
                    NavGraph()
                }
            }
        }
    }
}
