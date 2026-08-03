package com.nothingsense.ns.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.data.local.AppDatabase
import com.nothingsense.ns.data.local.entities.ChatEntity
import com.nothingsense.ns.data.local.entities.ChatType
import com.nothingsense.ns.data.local.entities.DeliveryStatus
import com.nothingsense.ns.data.local.entities.MessageEntity
import com.nothingsense.ns.data.local.entities.MessageType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BackupRestoreManager"

@Singleton
class BackupRestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val identityManager: IdentityManager,
    private val backupEngine: NoSenseBackupEngine
) {

    suspend fun exportBackupToUri(targetUri: Uri, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = identityManager.getOrCreateUserId()
            val username = identityManager.getUsername()
            val bio = identityManager.bioFlow.first()

            val chats = database.chatDao().getAllChats().first().map {
                BackupChatItem(
                    id = it.id,
                    name = it.name,
                    type = it.type.name,
                    lastMessage = it.lastMessage,
                    lastMessageTimestamp = it.lastMessageTimestamp
                )
            }

            val allMessages = mutableListOf<BackupMessageItem>()
            for (chat in chats) {
                val messages = database.messageDao().getMessagesForChat(chat.id).first()
                allMessages.addAll(messages.map {
                    BackupMessageItem(
                        id = it.id,
                        chatId = it.chatId,
                        senderId = it.senderId,
                        text = it.text,
                        timestamp = it.timestamp,
                        type = it.type.name,
                        fileUri = it.fileUri,
                        fileType = it.fileType,
                        fileName = it.fileName,
                        status = it.status.name
                    )
                })
            }

            val payload = BackupDataPayload(
                userId = userId,
                username = username,
                bio = bio,
                chats = chats,
                messages = allMessages
            )

            val containerBytes = backupEngine.packAndEncrypt(payload, passphrase)

            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                output.write(containerBytes)
            }
            Log.d(TAG, "Exported .nsbak backup container successfully (${containerBytes.size} bytes).")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export .nsbak backup", e)
            false
        }
    }

    suspend fun importBackupFromUri(sourceUri: Uri, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext false
            val containerBytes = inputStream.use { it.readBytes() }

            val payload = backupEngine.unpackAndDecrypt(containerBytes, passphrase)

            // Restore identity
            identityManager.setUsername(payload.username)
            identityManager.setBio(payload.bio)

            // Restore chats
            for (chat in payload.chats) {
                database.chatDao().insertChat(
                    ChatEntity(
                        id = chat.id,
                        name = chat.name,
                        type = try { ChatType.valueOf(chat.type) } catch (e: Exception) { ChatType.PRIVATE },
                        lastMessage = chat.lastMessage,
                        lastMessageTimestamp = chat.lastMessageTimestamp
                    )
                )
            }

            // Restore messages
            for (msg in payload.messages) {
                database.messageDao().insertMessage(
                    MessageEntity(
                        id = msg.id,
                        chatId = msg.chatId,
                        senderId = msg.senderId,
                        text = msg.text,
                        timestamp = msg.timestamp,
                        type = try { MessageType.valueOf(msg.type) } catch (e: Exception) { MessageType.TEXT },
                        fileUri = msg.fileUri,
                        fileType = msg.fileType,
                        fileName = msg.fileName,
                        status = try { DeliveryStatus.valueOf(msg.status) } catch (e: Exception) { DeliveryStatus.SENT }
                    )
                )
            }

            Log.d(TAG, "Imported .nsbak backup container successfully (${payload.chats.size} chats, ${payload.messages.size} messages).")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import .nsbak backup", e)
            false
        }
    }
}
