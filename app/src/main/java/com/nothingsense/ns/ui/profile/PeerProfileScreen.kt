package com.nothingsense.ns.ui.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingsense.ns.R
import com.nothingsense.ns.data.repository.ReputationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerProfileScreen(
    userId: String,
    username: String,
    reputationManager: ReputationManager,
    transportManager: com.nothingsense.ns.network.HybridTransportManager? = null,
    onStartChat: () -> Unit,
    onStartCall: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val blockedUsers by reputationManager.blockedUsersState.collectAsState()
    val isBlocked = blockedUsers.contains(userId)
    var showReportDialog by remember { mutableStateOf(false) }

    val reportCount = remember(userId) { reputationManager.getReportCount(userId) }
    val isWarningRequired = remember(reportCount) { reputationManager.isWarningBadgeRequired(userId) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Perfil de Nodo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar Header
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(100.dp)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = username.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = username,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            // Reputation Warning Badge if 3+ reports
            AnimatedVisibility(visible = isWarningRequired) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFF7675).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD63031)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD63031),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Advertencia de la Comunidad",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD63031),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Este nodo ha recibido múltiples reportes de la comunidad P2P. Procede con precaución.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Quick Actions: Chat & Call
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartChat,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Chat", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onStartCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Llamar", fontWeight = FontWeight.Bold)
                }
            }

            // Node Details Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Identidad Criptográfica del Nodo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = userId,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(userId))
                            Toast.makeText(context, "ID copiada al portapapeles", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copiar ID")
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Moderation Actions: Block & Report
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        if (isBlocked) {
                            reputationManager.unblockUser(userId)
                            Toast.makeText(context, "Usuario desbloqueado", Toast.LENGTH_SHORT).show()
                        } else {
                            reputationManager.blockUser(userId)
                            Toast.makeText(context, "Usuario bloqueado. No recibirás más mensajes.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isBlocked) MaterialTheme.colorScheme.primary else Color(0xFFE17055)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isBlocked) "Desbloquear Usuario" else "Bloquear Usuario", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { showReportDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD63031)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Flag, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reportar Usuario", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Silent Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Reportar Usuario", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Tu reporte se enviará de forma anónima a la red de reputación Mesh para proteger a otros usuarios de spam o abuso.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        reputationManager.reportUser(userId)
                        showReportDialog = false
                        Toast.makeText(context, "Reporte registrado anónimamente.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63031)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirmar Reporte", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
