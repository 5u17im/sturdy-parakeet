# 🏛️ Documentación de Arquitectura Técnica - NoSense

Esta documentación describe la arquitectura interna de la red de transporte híbrida, el protocolo de paquetes `MeshPacket` y el modelo de cifrado de la aplicación **NoSense**.

---

## 1. Componentes del Núcleo de Red

### 1.1 `MeshManager` (P2P Local)
- Utiliza la API **Google Nearby Connections** configurada con la estrategia `Strategy.P2P_CLUSTER`.
- Los dispositivos emiten un anuncio (*Advertising*) y descubren (*Discovery*) pares en el mismo rango de radio (Wi-Fi Direct / Bluetooth).
- El handshake es automático para formar una topología en malla no estructurada.

### 1.2 `CloudRelayClient` (Relé WebSocket Nube)
- Mantiene un canal dúplex de **WebSockets** persistente hacia el servidor relé en la nube (`wss://relay.nothingsense.app/ws?userId=<ID>`).
- Utiliza pings periódicos (15 segundos) para mantener activa la conexión tras cortafuegos o NATs.

### 1.3 `HybridTransportManager` (Orquestador Híbrido)
- Actúa como fachada unificada para los repositorios (`MessagingRepository`, `StatusRepository`).
- Administra las políticas de enrutamiento:
  - **Local Direct**: Si el `recipientId` es un vecino visible en `MeshManager`, el paquete se entrega por el enlace P2P.
  - **Cloud Direct**: Si el dispositivo tiene Internet y el objetivo no está en el radio local, se envía por `CloudRelayClient`.
  - **Gateway Bridging**: Si un paquete ingresa vía `MeshManager` con destino a un nodo lejano, y el nodo actual dispone de Internet, se retransmite hacia `CloudRelayClient`.

---

## 2. Protocolo de Paquetes (`MeshPacket`)

Toda la comunicación en NoSense (mensajes privados, canales, estados y archivos) utiliza la estructura serializable `MeshPacket`:

```kotlin
@Serializable
data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val recipientId: String? = null, // null representa difusión pública (Broadcast)
    val type: PacketType,
    val content: String, // Texto o Base64 comprimido
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 3, // Time To Live para saltos mesh
    val hopCount: Int = 0,
    val fileMetadata: FileMetadata? = null
)
```

### Tipos de Paquetes (`PacketType`)
- `PRIVATE_MESSAGE`: Mensaje directo entre dos pares.
- `CHANNEL_MESSAGE`: Mensaje difundido en un canal público.
- `STATUS_UPDATE`: Estado efímero de 24 horas (Texto o Foto).
- `FILE_TRANSFER`: Archivo o imagen transmitida mediante carga embebida en Base64.
- `PEER_DISCOVERY`: Paquete de anuncio de identidad entre nodos.

---

## 3. Modelo de Cifrado y Seguridad

1. **Almacenamiento de Claves**:
   - Se utiliza `AndroidKeyStore` para crear y almacenar claves simétricas `AES-256-GCM` aisladas por hardware.
2. **Cifrado de Carga útil**:
   - `CryptoManager.kt` cifra las cargas útiles antes de empaquetarlas en `MeshPacket`.
3. **Privacidad de Retransmisión**:
   - Los nodos **Gateway** (relés intermedios P2P o servidores en la nube) sólo pueden leer los campos de enrutamiento (`recipientId`, `ttl`, `type`).
   - El campo `content` permanece cifrado punto a punto, impidiendo que intermediarios inspeccionen las conversaciones.
