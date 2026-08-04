package com.nothingsense.ns.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingsense.ns.R
import com.nothingsense.ns.network.model.MeshNode
import com.nothingsense.ns.ui.qr.QRCodeGenerator
import com.nothingsense.ns.ui.qr.QRCodeScannerDialog

@Composable
fun AddUserScreen(
    discoveredNodes: List<MeshNode>,
    currentUserId: String = "",
    currentUsername: String = "",
    onAddUser: (userId: String, username: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var inputUserId by remember { mutableStateOf("") }
    var inputUsername by remember { mutableStateOf("") }
    var showQRScanner by remember { mutableStateOf(false) }

    val qrJsonPayload = remember(currentUserId, currentUsername) {
        "{\"v\":1,\"userId\":\"$currentUserId\",\"username\":\"$currentUsername\"}"
    }

    val qrImageBitmap = remember(qrJsonPayload) {
        QRCodeGenerator.generateQRCodeImageBitmap(qrJsonPayload, 512, 512)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Agregar Contacto Mesh",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Cercanos") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Buscar") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Mi QR") }
                    )
                }

                Spacer(Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // Nearby P2P Nodes
                        if (discoveredNodes.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Buscando pares en el radio Mesh...", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 240.dp)
                            ) {
                                items(discoveredNodes) { node ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onAddUser(node.userId, node.username)
                                                onDismiss()
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = node.username.take(1).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    node.username,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    "ID: ${node.userId.take(8)}...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                Icons.Rounded.Add,
                                                contentDescription = "Agregar",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Search by ID or Name
                        Column {
                            OutlinedTextField(
                                value = inputUsername,
                                onValueChange = { inputUsername = it },
                                label = { Text("Nombre de Usuario") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = inputUserId,
                                onValueChange = { inputUserId = it },
                                label = { Text("ID Criptográfica del Nodo") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (inputUserId.isNotBlank() || inputUsername.isNotBlank()) {
                                        val finalId = inputUserId.ifBlank { inputUsername }
                                        val finalName = inputUsername.ifBlank { "Usuario Mesh" }
                                        onAddUser(finalId, finalName)
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Agregar Contacto", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        // Real Dynamic QR Code View & Camera Scanner Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(180.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                                    if (qrImageBitmap != null) {
                                        Image(
                                            bitmap = qrImageBitmap,
                                            contentDescription = "Código QR NoSense",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Muestra este QR o escanea el de tu contacto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { showQRScanner = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Escanear QR con Cámara", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )

    if (showQRScanner) {
        QRCodeScannerDialog(
            onScanResult = { userId, username ->
                onAddUser(userId, username)
                onDismiss()
            },
            onDismiss = { showQRScanner = false }
        )
    }
}
