package com.nothingsense.ns.ui.call

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import androidx.compose.material.icons.rounded.Call
import com.nothingsense.ns.ui.chat.ChatViewModel

@Composable
fun CallScreen(
    peerUsername: String,
    peerUserId: String,
    isIncoming: Boolean = false,
    viewModel: ChatViewModel,
    onEndCall: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isCallConnected by remember { mutableStateOf(false) }
    var callDurationSeconds by remember { mutableStateOf(0) }
    var callState by remember { mutableStateOf(if (isIncoming) "Llamada Entrante P2P..." else "Llamando P2P...") }

    val pulseAnim = remember { Animatable(1f) }

    // Observe incoming call signals (ACCEPT, END, REJECT)
    LaunchedEffect(peerUserId) {
        if (!isIncoming) {
            // Outgoing call: Send OFFER signal to recipient
            viewModel.sendCallSignal(peerUserId, "OFFER")
        }

        viewModel.incomingCallEvents.collect { event ->
            if (event.senderId == peerUserId) {
                when (event.signal) {
                    "ACCEPT" -> {
                        callState = "Conectado P2P Cifrado"
                        isCallConnected = true
                        viewModel.startCallAudio(peerUserId)
                    }
                    "REJECT", "END" -> {
                        viewModel.stopCallAudio()
                        onEndCall()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            pulseAnim.animateTo(1.2f, animationSpec = tween(1000))
            pulseAnim.animateTo(1.0f, animationSpec = tween(1000))
        }
    }

    LaunchedEffect(isCallConnected) {
        if (isCallConnected) {
            while (true) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    val minutes = callDurationSeconds / 60
    val seconds = callDurationSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF231165), Color(0xFF1E2030), Color(0xFF0F101A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Peer Avatar with Pulsing Halo
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .size((120 * pulseAnim.value).dp)
                        .border(2.dp, Color(0xFF6C5CE7).copy(alpha = 0.4f), CircleShape)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF6C5CE7),
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = peerUsername.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = peerUsername,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (callDurationSeconds > 0) formattedTime else callState,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.weight(1f))

            // Controls
            if (isIncoming && !isCallConnected) {
                // Incoming call buttons: Accept or Reject
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Reject Call
                    IconButton(
                        onClick = {
                            viewModel.sendCallSignal(peerUserId, "REJECT")
                            onEndCall()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFD63031), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.CallEnd,
                            contentDescription = "Rechazar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Accept Call
                    IconButton(
                        onClick = {
                            viewModel.sendCallSignal(peerUserId, "ACCEPT")
                            callState = "Conectado P2P Cifrado"
                            isCallConnected = true
                            viewModel.startCallAudio(peerUserId)
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF00B894), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = "Contestar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else {
                // Active / Outgoing call controls
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Mute Mic
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            viewModel.setCallMuted(isMuted)
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                if (isMuted) Color.White else Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = "Silenciar",
                            tint = if (isMuted) Color.Black else Color.White
                        )
                    }

                    // End Call
                    IconButton(
                        onClick = {
                            viewModel.sendCallSignal(peerUserId, "END")
                            viewModel.stopCallAudio()
                            onEndCall()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFD63031), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.CallEnd,
                            contentDescription = "Colgar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Speakerphone
                    IconButton(
                        onClick = {
                            isSpeakerOn = !isSpeakerOn
                            viewModel.setSpeakerphoneOn(isSpeakerOn)
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                if (isSpeakerOn) Color(0xFF00B894) else Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = "Altavoz",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
