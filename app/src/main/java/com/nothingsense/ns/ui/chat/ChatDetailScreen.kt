package com.nothingsense.ns.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nothingsense.ns.data.local.entities.MessageEntity
import com.nothingsense.ns.data.local.entities.MessageType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    chatName: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.getMessages(chatId).collectAsState()
    val userId by viewModel.userId.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            // Logic to send file will be implemented in ViewModel
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
                title = { Text(chatName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
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
                BottomAppBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.ime),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Attach", tint = MaterialTheme.colorScheme.primary)
                    }
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        maxLines = 4
                    )
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
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isMe = message.senderId == userId
                )
            }
        }
    }
}

@Composable
fun AttachmentMenu(onTypeSelect: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentOption(Icons.Rounded.Image, "Image", "image/*", onTypeSelect)
            AttachmentOption(Icons.Rounded.Videocam, "Video", "video/*", onTypeSelect)
            AttachmentOption(Icons.Rounded.Audiotrack, "Audio", "audio/*", onTypeSelect)
            AttachmentOption(Icons.AutoMirrored.Rounded.InsertDriveFile, "File", "*/*", onTypeSelect)
        }
    }
}

@Composable
fun AttachmentOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, mimeType: String, onTypeSelect: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onTypeSelect(mimeType) }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun MessageBubble(message: MessageEntity, isMe: Boolean) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    val shape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = color,
            contentColor = contentColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(text = message.text, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                    MessageType.IMAGE -> {
                        AsyncImage(
                            model = message.fileUri,
                            contentDescription = "Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (message.text.isNotBlank()) {
                            Text(text = message.text, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                        }
                    }
                    MessageType.VIDEO -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
                            Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(48.dp), tint = contentColor.copy(alpha = 0.6f))
                        }
                    }
                    MessageType.AUDIO -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.Rounded.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            LinearProgressIndicator(progress = { 0f }, modifier = Modifier.weight(1f))
                        }
                    }
                    MessageType.FILE -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.InsertDriveFile, null)
                            Spacer(Modifier.width(8.dp))
                            Text(message.fileName ?: "File", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End).alpha(0.7f)
                )
            }
        }
    }
}
