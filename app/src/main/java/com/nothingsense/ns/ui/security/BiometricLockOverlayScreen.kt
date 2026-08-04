package com.nothingsense.ns.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.nothingsense.ns.security.BiometricAuthManager

@Composable
fun BiometricLockOverlayScreen(
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E1F29), Color(0xFF0F1017))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF6C5CE7).copy(alpha = 0.2f),
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = "Bloqueado",
                        tint = Color(0xFF6C5CE7),
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "NoSense Bloqueado",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Autentícate con tu huella dactilar para desbloquear el acceso a tus chats criptográficos.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    activity?.let {
                        BiometricAuthManager.promptBiometricAuth(
                            activity = it,
                            title = "Desbloquear NoSense",
                            subtitle = "Confirma tu huella dactilar",
                            onSuccess = onUnlockSuccess,
                            onError = { _ -> }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
            ) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Desbloquear con Huella", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}
