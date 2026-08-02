package com.nothingsense.ns.network

import android.util.Log
import com.nothingsense.ns.data.identity.IdentityManager
import com.nothingsense.ns.network.model.MeshPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CloudRelayClient"
private const val DEFAULT_RELAY_URL = "wss://relay.nothingsense.app/ws"

@Singleton
class CloudRelayClient @Inject constructor(
    private val identityManager: IdentityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<MeshPacket> = _incomingPackets.asSharedFlow()

    private var currentRelayUrl: String = DEFAULT_RELAY_URL
    private var isExplicitlyStopped = false

    fun start(serverUrl: String = DEFAULT_RELAY_URL) {
        currentRelayUrl = serverUrl
        isExplicitlyStopped = false

        if (client == null) {
            client = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(15, TimeUnit.SECONDS)
                .build()
        }

        connectWebSocket()
    }

    private fun connectWebSocket() {
        if (isExplicitlyStopped) return

        scope.launch {
            try {
                val userId = identityManager.getOrCreateUserId()
                val requestUrl = "$currentRelayUrl?userId=$userId"
                
                val request = Request.Builder()
                    .url(requestUrl)
                    .build()

                webSocket = client?.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d(TAG, "Cloud Relay WebSocket Connected to $currentRelayUrl")
                        _isConnected.value = true
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val packet = Json.decodeFromString<MeshPacket>(text)
                            Log.d(TAG, "Received Cloud Packet: type=${packet.type}, sender=${packet.senderName}")
                            _incomingPackets.tryEmit(packet)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing cloud relay message", e)
                        }
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "Cloud Relay WebSocket Closing: $reason")
                        _isConnected.value = false
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "Cloud Relay WebSocket Closed: $reason")
                        _isConnected.value = false
                        scheduleReconnect()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e(TAG, "Cloud Relay WebSocket Error: ${t.message}")
                        _isConnected.value = false
                        scheduleReconnect()
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate WebSocket connection", e)
                _isConnected.value = false
                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect() {
        if (isExplicitlyStopped) return
        scope.launch {
            delay(5000)
            if (!isExplicitlyStopped && !_isConnected.value) {
                Log.d(TAG, "Attempting Cloud Relay reconnection...")
                connectWebSocket()
            }
        }
    }

    fun sendPacket(packet: MeshPacket): Boolean {
        val ws = webSocket
        if (ws == null || !_isConnected.value) {
            Log.w(TAG, "Cannot send cloud packet: WebSocket not connected")
            return false
        }
        return try {
            val json = Json.encodeToString(packet)
            ws.send(json)
            Log.d(TAG, "Sent Cloud Packet: type=${packet.type}, recipient=${packet.recipientId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending packet over cloud relay", e)
            false
        }
    }

    fun stop() {
        isExplicitlyStopped = true
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _isConnected.value = false
    }
}
