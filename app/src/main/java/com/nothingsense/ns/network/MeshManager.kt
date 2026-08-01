package com.nothingsense.ns.network

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.network.model.MeshNode
import com.nothingsense.ns.network.model.MeshPacket
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    fun sendFile(fileUri: android.net.Uri, packet: MeshPacket, targetEndpointId: String) {
        try {
            val filePayload = Payload.fromFile(context.contentResolver.openFileDescriptor(fileUri, "r")!!)
            val metadataPayload = Payload.fromBytes(Json.encodeToString(packet).toByteArray())
            
            connectionsClient.sendPayload(targetEndpointId, metadataPayload)
            connectionsClient.sendPayload(targetEndpointId, filePayload)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file", e)
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
            
            _discoveredNodes.update { it + (endpointId to MeshNode(endpointId, userId, username)) }
            
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
                    _connectedNodes.update { it + (endpointId to node.copy(isConnected = true)) }
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
            payload.asBytes()?.let { bytes ->
                try {
                    val json = String(bytes)
                    val packet = Json.decodeFromString<MeshPacket>(json)
                    _incomingPackets.tryEmit(packet)
                    Log.d(TAG, "Packet received from $endpointId: $packet")
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding packet", e)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
        }
    }
}
