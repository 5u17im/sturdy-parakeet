package com.nothingsense.ns.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.data.local.entities.ChatEntity
import com.nothingsense.ns.data.local.entities.MessageEntity
import com.nothingsense.ns.data.repository.MessagingRepository
import com.nothingsense.ns.network.MeshManager
import com.nothingsense.ns.network.model.MeshNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: MessagingRepository,
    private val meshManager: MeshManager,
    private val identityManager: IdentityManager
) : ViewModel() {

    val chats: StateFlow<List<ChatEntity>> = repository.getAllChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredNodes: StateFlow<List<MeshNode>> = meshManager.discoveredNodes
        .combine(meshManager.connectedNodes) { discovered, connected ->
            discovered.values.toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userId: StateFlow<String?> = identityManager.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val onboardingCompleted: StateFlow<Boolean?> = identityManager.onboardingCompletedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val messageFlows = mutableMapOf<String, StateFlow<List<MessageEntity>>>()

    fun startMesh() {
        meshManager.startMesh()
    }

    fun getMessages(chatId: String): StateFlow<List<MessageEntity>> {
        return messageFlows.getOrPut(chatId) {
            repository.getMessagesForChat(chatId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun sendMessage(chatId: String, text: String, isChannel: Boolean = false) {
        viewModelScope.launch {
            repository.sendMessage(chatId, text, isChannel)
        }
    }

    fun createChat(node: MeshNode) {
        viewModelScope.launch {
            repository.createPrivateChat(node.userId, node.username)
        }
    }

    fun sendFile(chatId: String, uri: android.net.Uri) {
        viewModelScope.launch {
            repository.sendFile(chatId, uri)
        }
    }
}
