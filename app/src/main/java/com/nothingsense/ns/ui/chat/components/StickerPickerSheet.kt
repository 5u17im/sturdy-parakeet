package com.nothingsense.ns.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NoSenseSticker(
    val id: String,
    val emoji: String,
    val title: String
)

val NOSENSE_STICKER_PACK = listOf(
    NoSenseSticker("stk_mesh", "🕸️", "Red Mesh"),
    NoSenseSticker("stk_encrypted", "🔒", "Cifrado E2E"),
    NoSenseSticker("stk_zap", "⚡", "NoSense Pulse"),
    NoSenseSticker("stk_anon", "🤐", "Anónimo"),
    NoSenseSticker("stk_rocket", "🚀", "Despegue"),
    NoSenseSticker("stk_gem", "💎", "Gema Mesh"),
    NoSenseSticker("stk_fire", "🔥", "Fuego"),
    NoSenseSticker("stk_radio", "📻", "Radio P2P"),
    NoSenseSticker("stk_cloud", "☁️", "Nube Relay"),
    NoSenseSticker("stk_heart", "💜", "Corazón Violeta"),
    NoSenseSticker("stk_shield", "🛡️", "Escudo"),
    NoSenseSticker("stk_wave", "👋", "Hola Mesh")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerSheet(
    onStickerSelected: (NoSenseSticker) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Stickers & Emojis NoSense",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                items(NOSENSE_STICKER_PACK) { sticker ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                onStickerSelected(sticker)
                                onDismiss()
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = sticker.emoji,
                                fontSize = 32.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
