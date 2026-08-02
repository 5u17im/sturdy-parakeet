package com.nothingsense.ns.network

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.network.model.FileMetadata
import com.nothingsense.ns.network.model.MeshNode
import com.nothingsense.ns.network.model.MeshPacket
import com.nothingsense.ns.network.model.PacketType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MeshManager"
private const val SERVICE_ID = "com.nothingsense.ns.SERVICE_ID"

@Singleton
class MeshManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val identityManager: IdentityManager
) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMeshStarted = false

    private val _discoveredNodes = MutableStateFlow<Map<String, MeshNode>>(emptyMap())
    val discoveredNodes: StateFlow<Map<String, MeshNode>> = _discoveredNodes.asStateFlow()

    private val _connectedNodes = MutableStateFlow<Map<String, MeshNode>>(emptyMap())
    val connectedNodes: StateFlow<Map<String, MeshNode>> = _connectedNodes.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<MeshPacket> = _incomingPackets.asSharedFlow()

    private val _peerConnectedEvent = MutableSharedFlow<MeshNode>(extraBufferCapacity = 32)
    val peerConnectedEvent: SharedFlow<MeshNode> = _peerConnectedEvent.asSharedFlow()

    private val pendingFilePacketsQueue = ConcurrentLinkedQueue<MeshPacket>()
    private val pendingFilePayloads = ConcurrentHashMap<Long, Payload.File>()
    private val completedPayloadStatus = ConcurrentHashMap<Long, Boolean>()
    private val seenPacketIds = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun startMesh() {
        if (isMeshStarted) return
        isMeshStarted = true
        
        scope.launch {
            val userId = identityManager.getOrCreateUserId()
            val username = identityManager.getUsername()
            val endpointName = "$userId|$username"
            
            startAdvertising(endpointName)
            startDiscovery()
        }
    }

    private fun startAdvertising(endpointName: String) {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(
            endpointName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully for $endpointName")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Advertising failed", e)
            isMeshStarted = false
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started successfully")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Discovery failed", e)
            isMeshStarted = false
        }
    }

    fun stopMesh() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _discoveredNodes.value = emptyMap()
        _connectedNodes.value = emptyMap()
        isMeshStarted = false
    }

    fun connectToNode(endpointId: String) {
        scope.launch {
            val username = identityManager.getUsername()
            connectionsClient.requestConnection(
                username,
                endpointId,
                connectionLifecycleCallback
            ).addOnFailureListener { e ->
                Log.e(TAG, "Request connection failed to $endpointId", e)
            }
        }
    }

    fun sendPacket(packet: MeshPacket, targetEndpointId: String? = null) {
        val json = Json.encodeToString(packet)
        val payload = Payload.fromBytes(json.toByteArray())
        
        sendPayload(payload, targetEndpointId)
    }

    fun sendFile(fileUri: android.net.Uri, packet: MeshPacket, targetEndpointId: String? = null) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(fileUri, "r") ?: return
            val filePayload = Payload.fromFile(pfd)
            
            val updatedPacket = packet.copy(
                fileMetadata = packet.fileMetadata?.copy(fileId = filePayload.id)
            )
            val json = Json.encodeToString(updatedPacket)
            val metadataPayload = Payload.fromBytes(json.toByteArray())

            sendPayload(metadataPayload, targetEndpointId)
            sendPayload(filePayload, targetEndpointId)
            Log.d(TAG, "Sent file payload with ID: ${filePayload.id} and metadata")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file payload", e)
        }
    }

    private fun sendPayload(payload: Payload, targetEndpointId: String?) {
        if (targetEndpointId != null) {
            connectionsClient.sendPayload(targetEndpointId, payload)
        } else {
            val endpoints = _connectedNodes.value.keys.toList()
            if (endpoints.isNotEmpty()) {
                connectionsClient.sendPayload(endpoints, payload)
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId, name: ${info.endpointName}")
            val parts = info.endpointName.split("|")
            val userId = parts.getOrNull(0) ?: "unknown"
            val username = parts.getOrNull(1) ?: "Unknown"
            
            val node = MeshNode(endpointId, userId, username)
            _discoveredNodes.update { it + (endpointId to node) }
            
            // Auto-connect to discovered nodes
            connectToNode(endpointId)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            _discoveredNodes.update { it - endpointId }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated: $endpointId")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.d(TAG, "Connection successful: $endpointId")
                val node = _discoveredNodes.value[endpointId]
                if (node != null) {
                    val connectedNode = node.copy(isConnected = true)
                    _connectedNodes.update { it + (endpointId to connectedNode) }
                    _peerConnectedEvent.tryEmit(connectedNode)
                }
            } else {
                Log.e(TAG, "Connection failed: $endpointId, status: ${result.status}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected: $endpointId")
            _connectedNodes.update { it - endpointId }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    payload.asBytes()?.let { bytes ->
                        try {
                            val json = String(bytes)
                            val packet = Json.decodeFromString<MeshPacket>(json)
                            Log.d(TAG, "Packet received from $endpointId: $packet")
                            processReceivedPacket(packet, endpointId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding packet bytes", e)
                        }
                    }
                }
                Payload.Type.FILE -> {
                    val file = payload.asFile()
                    if (file != null) {
                        pendingFilePayloads[payload.id] = file
                        Log.d(TAG, "File payload received with ID: ${payload.id}")
                        tryProcessFileQueue()
                    }
                }
                else -> {}
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                Log.d(TAG, "Payload transfer success for ID: ${update.payloadId}")
                completedPayloadStatus[update.payloadId] = true
                tryProcessFileQueue()
            }
        }
    }

    private fun processReceivedPacket(packet: MeshPacket, endpointId: String) {
        if (!seenPacketIds.add(packet.packetId)) {
            Log.d(TAG, "Duplicate packet ${packet.packetId} ignored")
            return
        }

        scope.launch(Dispatchers.IO) {
            val currentUserId = identityManager.getOrCreateUserId()
            val isForMe = packet.recipientId == null || packet.recipientId == currentUserId

            if (isForMe) {
                if (packet.type == PacketType.FILE_TRANSFER && packet.fileMetadata != null) {
                    pendingFilePacketsQueue.add(packet)
                    tryProcessFileQueue()
                } else {
                    _incomingPackets.tryEmit(packet)
                }
            }

            // Multi-hop Routing Relay
            if (packet.ttl > 1 && packet.senderId != currentUserId) {
                val relayedPacket = packet.copy(
                    ttl = packet.ttl - 1,
                    hopCount = packet.hopCount + 1
                )
                val otherEndpoints = _connectedNodes.value.keys.filter { it != endpointId }
                if (otherEndpoints.isNotEmpty()) {
                    try {
                        val json = Json.encodeToString(relayedPacket)
                        val payload = Payload.fromBytes(json.toByteArray())
                        connectionsClient.sendPayload(otherEndpoints, payload)
                        Log.d(TAG, "Relayed packet ${packet.packetId} to ${otherEndpoints.size} nodes (Hops: ${relayedPacket.hopCount})")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error relaying packet", e)
                    }
                }
            }
        }
    }

    private fun tryProcessFileQueue() {
        scope.launch(Dispatchers.IO) {
            val completedId = completedPayloadStatus.keys.firstOrNull { completedPayloadStatus[it] == true }
            if (completedId != null) {
                val filePayload = pendingFilePayloads[completedId]
                val packet = pendingFilePacketsQueue.poll()
                if (filePayload != null && packet != null) {
                    completedPayloadStatus.remove(completedId)
                    pendingFilePayloads.remove(completedId)

                    try {
                        val receivedDir = java.io.File(context.filesDir, "received_files")
                        if (!receivedDir.exists()) receivedDir.mkdirs()

                        val fileName = packet.fileMetadata?.fileName ?: "file_${System.currentTimeMillis()}"
                        val destFile = java.io.File(receivedDir, "${System.currentTimeMillis()}_$fileName")

                        val javaFile = filePayload.asJavaFile()
                        if (javaFile != null && javaFile.exists()) {
                            javaFile.copyTo(destFile, overwrite = true)
                            javaFile.delete()
                        } else {
                            filePayload.asParcelFileDescriptor()?.let { pfd ->
                                java.io.FileInputStream(pfd.fileDescriptor).use { input ->
                                    java.io.FileOutputStream(destFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }

                        val savedUri = android.net.Uri.fromFile(destFile).toString()
                        val finalPacket = packet.copy(content = savedUri)
                        _incomingPackets.tryEmit(finalPacket)
                        Log.d(TAG, "Successfully processed incoming file to $savedUri")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed saving received file payload", e)
                    }
                }
            }
        }
    }
}
