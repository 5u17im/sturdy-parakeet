package com.nothingsense.ns.network

import android.util.Log
import com.nothingsense.ns.network.model.MeshNode
import com.nothingsense.ns.network.model.MeshPacket
import com.nothingsense.ns.network.model.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HybridTransportManager"

@Singleton
class HybridTransportManager @Inject constructor(
    val meshManager: MeshManager,
    val cloudRelayClient: CloudRelayClient,
    val connectivityObserver: NetworkConnectivityObserver
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 128)
    val incomingPackets: SharedFlow<MeshPacket> = _incomingPackets.asSharedFlow()

    val connectedLocalNodes: StateFlow<Map<String, MeshNode>> = meshManager.connectedNodes
    val isCloudConnected: StateFlow<Boolean> = cloudRelayClient.isConnected
    val peerConnectedEvent: SharedFlow<MeshNode> = meshManager.peerConnectedEvent

    private val relayedPacketIds: MutableSet<String> = java.util.Collections.synchronizedSet(
        java.util.Collections.newSetFromMap(
            object : java.util.LinkedHashMap<String, Boolean>(500, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                    return size > 3000
                }
            }
        )
    )

    init {
        startTransportEngine()
    }

    fun startTransportEngine() {
        meshManager.startMesh()

        // Observe network connectivity to manage CloudRelay
        scope.launch {
            connectivityObserver.isConnected.collect { isOnline ->
                Log.d(TAG, "Network state changed: isOnline=$isOnline")
                if (isOnline) {
                    cloudRelayClient.start()
                } else {
                    cloudRelayClient.stop()
                }
            }
        }

        // Collect incoming packets from Mesh (P2P Local)
        scope.launch {
            meshManager.incomingPackets.collect { packet ->
                handleLocalMeshPacket(packet)
            }
        }

        // Collect incoming packets from Cloud Relay (Internet)
        scope.launch {
            cloudRelayClient.incomingPackets.collect { packet ->
                handleCloudRelayPacket(packet)
            }
        }
    }

    private fun handleLocalMeshPacket(packet: MeshPacket) {
        if (relayedPacketIds.contains(packet.packetId)) return
        relayedPacketIds.add(packet.packetId)

        // Emit locally for our own app handling
        _incomingPackets.tryEmit(packet)

        // GATEWAY RELAY LOGIC (Local -> Cloud):
        // If this packet is targeted to someone not in our local mesh, and we have internet,
        // act as a Gateway and forward it to the Cloud Relay!
        val targetUserId = packet.recipientId
        val isTargetLocal = targetUserId == null || connectedLocalNodes.value.values.any { it.userId == targetUserId }

        if (!isTargetLocal && connectivityObserver.checkCurrentConnection()) {
            Log.d(TAG, "[GATEWAY RELAY] Forwarding local P2P packet to Cloud Relay for recipient: $targetUserId")
            cloudRelayClient.sendPacket(packet)
        }
    }

    private fun handleCloudRelayPacket(packet: MeshPacket) {
        if (relayedPacketIds.contains(packet.packetId)) return
        relayedPacketIds.add(packet.packetId)

        // Emit for our own app handling
        _incomingPackets.tryEmit(packet)

        // GATEWAY RELAY LOGIC (Cloud -> Local):
        // If a cloud packet arrived for a peer that is connected in our local P2P mesh,
        // bridge and transmit it over local Mesh to that peer!
        val targetUserId = packet.recipientId
        if (targetUserId != null) {
            val localPeerEndpoint = connectedLocalNodes.value.values.find { it.userId == targetUserId }?.endpointId
            if (localPeerEndpoint != null) {
                Log.d(TAG, "[GATEWAY RELAY] Bridging Cloud packet to local offline peer: $targetUserId")
                meshManager.sendPacket(packet, localPeerEndpoint)
            } else if (targetUserId == "PUBLIC_CHANNEL" || packet.type == PacketType.CHANNEL_MESSAGE || packet.type == PacketType.STATUS_UPDATE) {
                // Broadcast cloud channel messages or statuses to local mesh
                meshManager.sendPacket(packet, null)
            }
        }
    }

    suspend fun sendPacket(packet: MeshPacket, targetUserId: String? = null) {
        relayedPacketIds.add(packet.packetId)

        val localTargetEndpoint = if (targetUserId != null) {
            connectedLocalNodes.value.values.find { it.userId == targetUserId }?.endpointId
        } else null

        // 1. If target is in local Mesh, send via Mesh
        if (localTargetEndpoint != null) {
            Log.d(TAG, "Sending packet via Local P2P Mesh to $targetUserId")
            meshManager.sendPacket(packet, localTargetEndpoint)
        } 
        // 2. If it's a channel broadcast or status update, broadcast locally AND cloud
        else if (targetUserId == null) {
            Log.d(TAG, "Broadcasting packet via Local P2P Mesh & Cloud Relay")
            meshManager.sendPacket(packet, null)
            if (connectivityObserver.checkCurrentConnection()) {
                cloudRelayClient.sendPacket(packet)
            }
        } 
        // 3. Otherwise send via Cloud Relay if internet is available
        else if (connectivityObserver.checkCurrentConnection()) {
            Log.d(TAG, "Sending packet via Cloud Relay to $targetUserId")
            val sent = cloudRelayClient.sendPacket(packet)
            if (!sent) {
                // Fallback to local mesh broadcast
                meshManager.sendPacket(packet, null)
            }
        } 
        // 4. Fallback: Broadcast over local mesh so multi-hop relay can find it
        else {
            Log.d(TAG, "Internet unavailable. Fallback broadcast over Local Mesh for multi-hop: $targetUserId")
            meshManager.sendPacket(packet, null)
        }
    }
}
