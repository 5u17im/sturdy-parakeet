package com.nothingsense.ns.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.data.local.dao.StatusDao
import com.nothingsense.ns.data.local.entities.StatusEntity
import com.nothingsense.ns.network.MeshManager
import com.nothingsense.ns.network.model.MeshPacket
import com.nothingsense.ns.network.model.PacketType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statusDao: StatusDao,
    private val meshManager: MeshManager,
    private val identityManager: IdentityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        observeIncomingPackets()
    }

    private fun observeIncomingPackets() {
        scope.launch {
            meshManager.incomingPackets.collectLatest { packet ->
                if (packet?.type == PacketType.STATUS_UPDATE) {
                    handleIncomingStatus(packet)
                }
            }
        }
    }

    private suspend fun handleIncomingStatus(packet: MeshPacket) {
        // Content format: "TEXT_CONTENT" or "IMAGE_BASE64||TEXT_CONTENT"
        val parts = packet.content.split("||", limit = 2)
        var imageUri: String? = null
        val textContent: String

        if (parts.size == 2) {
            // Has image: first part is Base64, second is text
            try {
                val imageBytes = Base64.decode(parts[0], Base64.NO_WRAP)
                val receivedDir = File(context.filesDir, "received_statuses")
                if (!receivedDir.exists()) receivedDir.mkdirs()
                val destFile = File(receivedDir, "${System.currentTimeMillis()}_status.jpg")
                FileOutputStream(destFile).use { it.write(imageBytes) }
                imageUri = Uri.fromFile(destFile).toString()
            } catch (e: Exception) {
                android.util.Log.e("StatusRepository", "Failed to decode status image", e)
            }
            textContent = parts[1]
        } else {
            textContent = packet.content
        }

        val status = StatusEntity(
            id = UUID.randomUUID().toString(),
            userId = packet.senderId,
            username = packet.senderName,
            content = textContent,
            timestamp = packet.timestamp,
            expiresAt = packet.timestamp + (24 * 60 * 60 * 1000),
            imageUri = imageUri
        )
        statusDao.insertStatus(status)
    }

    fun getActiveStatuses(): Flow<List<StatusEntity>> = 
        statusDao.getActiveStatuses(System.currentTimeMillis())

    suspend fun postStatus(content: String, imageUri: Uri? = null) {
        val userId = identityManager.getOrCreateUserId()
        val username = identityManager.getUsername()
        val now = System.currentTimeMillis()

        // Compress and encode image if present
        var imageBase64: String? = null
        var localImageUri: String? = null

        if (imageUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream != null) {
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    if (bitmap != null) {
                        val maxWidth = 800
                        val scaledBitmap = if (bitmap.width > maxWidth) {
                            val ratio = maxWidth.toFloat() / bitmap.width
                            val newHeight = (bitmap.height * ratio).toInt()
                            Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true).also {
                                if (it != bitmap) bitmap.recycle()
                            }
                        } else {
                            bitmap
                        }

                        val outputStream = ByteArrayOutputStream()
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
                        scaledBitmap.recycle()

                        val imageBytes = outputStream.toByteArray()
                        imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                        // Save local copy
                        val statusDir = File(context.filesDir, "received_statuses")
                        if (!statusDir.exists()) statusDir.mkdirs()
                        val localFile = File(statusDir, "${now}_my_status.jpg")
                        FileOutputStream(localFile).use { it.write(imageBytes) }
                        localImageUri = Uri.fromFile(localFile).toString()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StatusRepository", "Error compressing status image", e)
            }
        }

        val status = StatusEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            username = username,
            content = content,
            timestamp = now,
            expiresAt = now + (24 * 60 * 60 * 1000),
            imageUri = localImageUri
        )
        
        statusDao.insertStatus(status)

        // Build packet content: "IMAGE_BASE64||TEXT" or just "TEXT"
        val packetContent = if (imageBase64 != null) {
            "$imageBase64||$content"
        } else {
            content
        }

        val packet = MeshPacket(
            senderId = userId,
            senderName = username,
            type = PacketType.STATUS_UPDATE,
            content = packetContent,
            timestamp = now
        )
        meshManager.sendPacket(packet) // Broadcast to all
    }
}
