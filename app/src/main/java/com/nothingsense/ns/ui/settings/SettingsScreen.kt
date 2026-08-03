package com.nothingsense.ns.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingsense.ns.BuildConfig
import com.nothingsense.ns.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userId by viewModel.userId.collectAsState()
    val currentUsername by viewModel.username.collectAsState()
    var username by remember(currentUsername) { mutableStateOf(currentUsername ?: "") }
    val clipboardManager = LocalClipboardManager.current
    val isBiometricEnabled by viewModel.biometricEnabled.collectAsState()
    val isAutoDownloadEnabled by viewModel.autoDownload.collectAsState()
    val selectedTheme by viewModel.themeMode.collectAsState()

    val updateInfo by viewModel.updateInfoState.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.settings),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Ajustes, privacidad y apariencia",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Identity Section Header
            SettingsSectionHeader("TU IDENTIDAD MESH")
            
            // User ID Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ID de Nodo Criptográfico",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Miembro Anónimo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                userId ?: "...",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            userId?.let {
                                clipboardManager.setText(AnnotatedString(it))
                                Toast.makeText(context, "ID copiado", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copiar ID")
                        }
                    }
                }
            }

            // Quick Username Edit
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            if (username.isNotBlank() && username != currentUsername) {
                Button(
                    onClick = { viewModel.updateUsername(username) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }

            // Software Updates Section
            SettingsSectionHeader("ACTUALIZACIONES DE SOFTWARE")
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Autoactualizador NoSense",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Versión actual: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    AnimatedVisibility(visible = updateInfo != null) {
                        val info = updateInfo
                        if (info != null) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                if (info.isUpdateAvailable) {
                                    Text(
                                        text = "✨ ¡Nueva versión v${info.latestVersionName} disponible!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = info.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.downloadAndInstallUpdate(info) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Descargar e Instalar Ahora", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "✅ NoSense está actualizado a la última versión disponible.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF00B894),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.checkForUpdates() },
                        enabled = !isCheckingUpdate,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Buscando en GitHub Releases...")
                        } else {
                            Text("Buscar Actualizaciones")
                        }
                    }
                }
            }

            // Customization & Appearance Section
            SettingsSectionHeader("PERSONALIZACIÓN Y APARIENCIA")
            SettingsSwitchRow(
                icon = Icons.Rounded.Palette,
                title = "Tema de Interfaz",
                subtitle = selectedTheme,
                checked = selectedTheme == "NoSense Dark",
                onCheckedChange = {
                    val newTheme = if (it) "NoSense Dark" else "Modo Claro"
                    viewModel.setThemeMode(newTheme)
                }
            )

    val isFlagSecureEnabled by viewModel.flagSecureEnabled.collectAsState()
    var showWipeDialog by remember { mutableStateOf(false) }
    var showPassphraseDialog by remember { mutableStateOf(false) }
    var backupMode by remember { mutableStateOf("EXPORT") } // "EXPORT" or "IMPORT"
    var pendingBackupUri by remember { mutableStateOf<Uri?>(null) }
    var passphraseText by remember { mutableStateOf("") }

    val createBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            pendingBackupUri = it
            backupMode = "EXPORT"
            passphraseText = ""
            showPassphraseDialog = true
        }
    }

    val importBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingBackupUri = it
            backupMode = "IMPORT"
            passphraseText = ""
            showPassphraseDialog = true
        }
    }

    // Security Wipe Confirmation Dialog
    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("🚨 BORRADO DE SEGURIDAD DE EMERGENCIA", color = Color(0xFFD63031), fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción destruirá permanentemente tus conversaciones, mensajes, llaves criptográficas y preferencias locales. La app se reiniciará al estado de fábrica.\n\n¿Estás seguro de continuar?") },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeDialog = false
                        viewModel.executeEmergencyWipe {
                            Toast.makeText(context, "Información destruida correctamente", Toast.LENGTH_LONG).show()
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63031))
                ) {
                    Text("SÍ, BORRAR TODO", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Passphrase Dialog for .nsbak Container
    if (showPassphraseDialog) {
        AlertDialog(
            onDismissRequest = { showPassphraseDialog = false },
            title = { Text(if (backupMode == "EXPORT") "🔒 Cifrar Respaldo (.nsbak)" else "🔓 Descifrar Respaldo (.nsbak)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Ingresa una contraseña para proteger el contenedor binario de respaldo:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passphraseText,
                        onValueChange = { passphraseText = it },
                        label = { Text("Contraseña de Respaldo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = passphraseText.isNotBlank(),
                    onClick = {
                        val uri = pendingBackupUri
                        val pass = passphraseText
                        showPassphraseDialog = false
                        if (uri != null && pass.isNotBlank()) {
                            if (backupMode == "EXPORT") {
                                viewModel.exportBackup(uri, pass) { success ->
                                    val msg = if (success) "Respaldo .nsbak guardado exitosamente" else "Fallo al exportar respaldo"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.importBackup(uri, pass) { success ->
                                    val msg = if (success) "Respaldo .nsbak restaurado exitosamente" else "Contraseña incorrecta o archivo dañado"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text(if (backupMode == "EXPORT") "Exportar .nsbak" else "Restaurar .nsbak")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPassphraseDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Security & Privacy Section
    SettingsSectionHeader("SEGURIDAD Y PRIVACIDAD FÍSICA")
    SettingsSwitchRow(
        icon = Icons.Rounded.Fingerprint,
        title = "Bloqueo Biométrico",
        subtitle = "Requerir Huella/PIN para abrir la app",
        checked = isBiometricEnabled,
        onCheckedChange = { viewModel.setBiometricEnabled(it) }
    )

    SettingsSwitchRow(
        icon = Icons.Rounded.Download,
        title = "Protección Anti-Capturas (FLAG_SECURE)",
        subtitle = "Bloquear capturas de pantalla y ocultar en multitarea",
        checked = isFlagSecureEnabled,
        onCheckedChange = { viewModel.setFlagSecureEnabled(it) }
    )

    SettingsSwitchRow(
        icon = Icons.Rounded.Download,
        title = "Descarga Automática Mesh",
        subtitle = "Descargar archivos adjuntos automáticamente",
        checked = isAutoDownloadEnabled,
        onCheckedChange = { viewModel.setAutoDownloadEnabled(it) }
    )

    // Backup & Restore Section
    SettingsSectionHeader("PERMANENCIA DE DATOS Y RESPALDOS")
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Contenedor Cifrado (.nsbak)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Exporta o importa tus conversaciones e identidad en un formato binario propietario inmune a lectura externa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { createBackupLauncher.launch("nosense_backup_${System.currentTimeMillis()}.nsbak") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Exportar .nsbak", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { importBackupLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Importar .nsbak", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Emergency Wipe Section
    SettingsSectionHeader("ZONA DE EMERGENCIA")
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFD63031).copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Autodestrucción / Borrado Seguro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD63031)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Destruye permanentemente todos los chats, llaves criptográficas y datos del dispositivo sin posibilidad de recuperación.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showWipeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63031)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("💥 Borrar Toda la Información de Seguridad", fontWeight = FontWeight.Bold)
            }
        }
    }

            // App Version Footer
            Spacer(Modifier.height(12.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "NoSense v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) - Red Mesh Criptográfica",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
