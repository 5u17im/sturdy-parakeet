package com.nothingsense.ns.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nothingsense.ns.data.local.entities.ChatEntity
import com.nothingsense.ns.network.model.MeshNode
import com.nothingsense.ns.ui.NearbyPermissionsHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    onChatClick: (ChatEntity) -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val nodes by viewModel.discoveredNodes.collectAsState()
    var showDiscoverDialog by remember { mutableStateOf(false) }

    NearbyPermissionsHandler {
        viewModel.startMesh()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text("NoSense", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDiscoverDialog = true }) {
                Icon(Icons.Rounded.Wifi, contentDescription = "Discover")
            }
        }
    ) { padding ->
        if (chats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp))
                    Text("No chats yet", style = MaterialTheme.typography.bodyLarge)
                    Text("Connect to nearby nodes to start chatting", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding
            ) {
                items(chats) { chat ->
                    ChatItem(chat = chat, onClick = { onChatClick(chat) })
                }
            }
        }
    }

    if (showDiscoverDialog) {
        AlertDialog(
            onDismissRequest = { showDiscoverDialog = false },
            title = { Text("Discovered Nodes") },
            text = {
                if (nodes.isEmpty()) {
                    Text("Searching for nearby users...")
                } else {
                    LazyColumn {
                        items(nodes) { node ->
                            ListItem(
                                headlineContent = { Text(node.username) },
                                supportingContent = { Text(node.userId.take(8)) },
                                leadingContent = { Icon(Icons.Rounded.Person, null) },
                                modifier = Modifier.clickable {
                                    viewModel.createChat(node)
                                    showDiscoverDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiscoverDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ChatItem(chat: ChatEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(chat.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text(
                chat.lastMessage ?: "Start a conversation",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
