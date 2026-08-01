package com.nothingsense.ns.data.repository

import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.data.local.dao.StatusDao
import com.nothingsense.ns.data.local.entities.StatusEntity
import com.nothingsense.ns.network.MeshManager
import com.nothingsense.ns.network.model.MeshPacket
import com.nothingsense.ns.network.model.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
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
        val status = StatusEntity(
            id = UUID.randomUUID().toString(),
            userId = packet.senderId,
            username = packet.senderName,
            content = packet.content,
            timestamp = packet.timestamp,
            expiresAt = packet.timestamp + (24 * 60 * 60 * 1000) // 24 hours
        )
        statusDao.insertStatus(status)
    }

    fun getActiveStatuses(): Flow<List<StatusEntity>> = 
        statusDao.getActiveStatuses(System.currentTimeMillis())

    suspend fun postStatus(content: String) {
        val userId = identityManager.getOrCreateUserId()
        val username = identityManager.getUsername()
        val now = System.currentTimeMillis()

        val status = StatusEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            username = username,
            content = content,
            timestamp = now,
            expiresAt = now + (24 * 60 * 60 * 1000)
        )
        
        statusDao.insertStatus(status)

        val packet = MeshPacket(
            senderId = userId,
            senderName = username,
            type = PacketType.STATUS_UPDATE,
            content = content,
            timestamp = now
        )
        meshManager.sendPacket(packet) // Broadcast to all
    }
}
