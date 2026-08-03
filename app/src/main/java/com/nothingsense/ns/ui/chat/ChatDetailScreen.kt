package com.nothingsense.ns.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nothingsense.ns.R
import com.nothingsense.ns.data.local.entities.MessageEntity
import com.nothingsense.ns.data.local.entities.MessageType
import com.nothingsense.ns.ui.chat.components.NoSenseSticker
import com.nothingsense.ns.ui.chat.components.StickerPickerSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    chatName: String,
    viewModel: ChatViewModel,
    onNavigateToPeerProfile: (userId: String, username: String) -> Unit = { _, _ -> },
    onStartCall: (userId: String, username: String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.getMessages(chatId).collectAsState()
    val userId by viewModel.userId.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var isRecordingWalkieTalkie by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendFile(chatId, it)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (chatId != "PUBLIC_CHANNEL") {
                                    onNavigateToPeerProfile(chatId, chatName)
                                }
                            }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = chatName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(chatName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                stringResource(R.string.connected_nearby),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (chatId != "PUBLIC_CHANNEL") {
                            IconButton(onClick = { onStartCall(chatId, chatName) }) {
                                Icon(
                                    Icons.Rounded.Call,
                                    contentDescription = "Llamar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            Column {
                if (showAttachmentMenu) {
                    AttachmentMenu(
                        onTypeSelect = { type ->
                            showAttachmentMenu = false
                            filePicker.launch(type)
                        }
                    )
                }

                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.ime)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showAttachmentMenu = !showAttachmentMenu },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = if (showAttachmentMenu) Icons.Rounded.Close else Icons.Rounded.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = { showStickerPicker = true }) {
                            Icon(
                                Icons.Rounded.Mood,
                                contentDescription = "Stickers & Emojis",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.message_placeholder)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        Spacer(Modifier.width(6.dp))

                        if (text.isNotBlank()) {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.sendMessage(
                                        chatId = chatId,
                                        text = text,
                                        isChannel = chatId == "PUBLIC_CHANNEL"
                                    )
                                    text = ""
                                },
                                elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = stringResource(R.string.send),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            // Walkie-Talkie Push To Talk Button
                            Surface(
                                shape = CircleShape,
                                color = if (isRecordingWalkieTalkie) Color(0xFFD63031) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isRecordingWalkieTalkie = true
                                                viewModel.startWalkieTalkie(chatId)
                                                Toast.makeText(context, "📻 Transmitiendo en Walkie-Talkie P2P...", Toast.LENGTH_SHORT).show()
                                                tryAwaitRelease()
                                                isRecordingWalkieTalkie = false
                                                viewModel.stopWalkieTalkie(chatId)
                                                Toast.makeText(context, "Fin de transmisión", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isRecordingWalkieTalkie) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                        contentDescription = "Walkie Talkie P2P",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isMe = message.senderId == userId
                )
            }
        }
    }

    // Sticker Picker Sheet
    if (showStickerPicker) {
        StickerPickerSheet(
            onStickerSelected = { sticker ->
                viewModel.sendMessage(
                    chatId = chatId,
                    text = sticker.emoji,
                    isChannel = chatId == "PUBLIC_CHANNEL"
                )
            },
            onDismiss = { showStickerPicker = false }
        )
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isMe: Boolean
) {
    val context = LocalContext.current
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = bubbleShape,
            color = containerColor,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (!isMe) {
                    Text(
                        text = message.senderId.take(8),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }
                    MessageType.IMAGE -> {
                        AsyncImage(
                            model = message.fileUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { openFileIntent(context, message.fileUri, message.fileType ?: "image/*") }
                        )
                    }
                    MessageType.VIDEO -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable { openFileIntent(context, message.fileUri, message.fileType ?: "video/*") }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PlayCircle, contentDescription = "Reproducir Video", tint = Color.White, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                    MessageType.AUDIO -> {
                        AudioPlayerBubble(
                            fileUriString = message.fileUri,
                            fileName = message.fileName,
                            contentColor = contentColor
                        )
                    }
                    MessageType.FILE -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = contentColor.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openFileIntent(context, message.fileUri, message.fileType ?: "*/*") }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.InsertDriveFile,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName ?: stringResource(R.string.file),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = message.fileType?.substringAfter('/')?.uppercase() ?: "DOCUMENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor.copy(alpha = 0.7f)
                                    )
                                }
                                Icon(
                                    Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End).alpha(0.7f)
                )
            }
        }
    }
}

private fun openFileIntent(context: Context, fileUriString: String?, mimeType: String?) {
    if (fileUriString == null) return
    try {
        val uri = Uri.parse(fileUriString)
        val contentUri = if (uri.scheme == "file") {
            val file = java.io.File(uri.path ?: "")
            if (file.exists()) {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else uri
        } else uri

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("ChatDetailScreen", "Cannot open file intent", e)
    }
}

@Composable
fun AttachmentMenu(
    onTypeSelect: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(16.dp)
        ) {
            AttachmentOption(icon = Icons.Rounded.Image, label = "Foto", color = Color(0xFF00CEC9)) { onTypeSelect("image/*") }
            AttachmentOption(icon = Icons.Rounded.VideoFile, label = "Video", color = Color(0xFF6C5CE7)) { onTypeSelect("video/*") }
            AttachmentOption(icon = Icons.Rounded.AudioFile, label = "Audio", color = Color(0xFFE17055)) { onTypeSelect("audio/*") }
            AttachmentOption(icon = Icons.AutoMirrored.Rounded.InsertDriveFile, label = "Archivo", color = Color(0xFF00B894)) { onTypeSelect("*/*") }
        }
    }
}

@Composable
fun AttachmentOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = color)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun AudioPlayerBubble(
    fileUriString: String?,
    fileName: String?,
    contentColor: Color
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    DisposableEffect(fileUriString) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                try {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying && mp.duration > 0) {
                        progress = mp.currentPosition.toFloat() / mp.duration.toFloat()
                    }
                } catch (e: Exception) {
                    isPlaying = false
                }
                kotlinx.coroutines.delay(200)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        IconButton(
            onClick = {
                if (fileUriString == null) return@IconButton
                try {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayer == null) {
                            val uri = Uri.parse(fileUriString)
                            mediaPlayer = android.media.MediaPlayer().apply {
                                setDataSource(context, uri)
                                prepare()
                                setOnCompletionListener {
                                    isPlaying = false
                                    progress = 0f
                                }
                            }
                        }
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AudioPlayerBubble", "Error playing audio", e)
                    openFileIntent(context, fileUriString, "audio/*")
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = contentColor
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName ?: "Audio",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = contentColor,
                trackColor = contentColor.copy(alpha = 0.3f)
            )
        }
    }
}
