package com.nothingsense.ns.ui.channel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nothingsense.ns.data.local.entities.ChatEntity
import com.nothingsense.ns.data.local.entities.ChatType
import com.nothingsense.ns.ui.chat.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    viewModel: ChatViewModel,
    onChannelClick: (ChatEntity) -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val channels = chats.filter { it.type == ChatType.CHANNEL || it.id == "PUBLIC_CHANNEL" }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Channels", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    onChannelClick(ChatEntity("PUBLIC_CHANNEL", "Public Channel", ChatType.CHANNEL, null, null))
                },
                icon = { Icon(Icons.Rounded.Campaign, null) },
                text = { Text("Join Public Channel") }
            )
        }
    ) { padding ->
        if (channels.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp))
                    Text("No channels joined", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding
            ) {
                items(channels) { channel ->
                    ListItem(
                        headlineContent = { Text(channel.name, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(channel.lastMessage ?: "Broadcast to everyone") },
                        leadingContent = {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Campaign, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        },
                        modifier = Modifier.clickable { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}
