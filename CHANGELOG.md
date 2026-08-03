# 📜 NoSense — Registro de Cambios (Changelog)

Todas las novedades, correcciones de errores y mejoras de rendimiento de NoSense.

---

## 🚀 [v1.4.0] - 2026-08-03

### 🚨 Borrado de Seguridad de Emergencia (Autodestrucción)
- **`SecurityWipeManager`:** Destrucción completa de la base de datos Room (`chats`, `messages`, `statuses`, `channels`).
- **Destrucción de Llaves Criptográficas:** Eliminación de los alias `nosense_identity_key` y `nosense_ecdh_key` en el `AndroidKeyStore`.
- **Limpieza Físico/Local:** Vaciado recursivo de la carpeta `received_files`, caché y preferencias `DataStore`.
- **Zona de Emergencia:** Botón dedicado con diálogo de advertencia e información irreversible en la interfaz de Ajustes.

### 🔒 Formato Binario Cifrado Propietario `.nsbak`
- **`NoSenseBackupEngine`:** Desarrollado un empaquetador binario exclusivo con encabezado mágico `NSBK` (`0x4E 0x53 0x42 0x4B`).
- **Seguridad Criptográfica:** Derivación de clave `PBKDF2WithHmacSHA256` (100,000 iteraciones) + Salt aleatorio de 16 bytes y cifrado `AES-256-GCM` con IV de 12 bytes.
- **Permanencia de Datos:** Exportación e importación de conversaciones e identidad en un contenedor inmune a lectura o edición fuera de NoSense.

### 🚫 Protección Anti-Capturas (`FLAG_SECURE`)
- **Bloqueo Multitarea:** Aplicación dinámica de `FLAG_SECURE` en la ventana de `MainActivity` para bloquear capturas de pantalla, grabación de video y vista oculta en la lista de aplicaciones recientes de Android.

---

## 🔐 [v1.3.0] - 2026-08-03

### Cifrado E2EE & Intercambio de Claves ECDH
- **Diffie-Hellman en Curva Elíptica:** Generación de pares de claves `secp256r1` en el `AndroidKeyStore`.
- **Paquetes HANDSHAKE:** Transmisión P2P de claves públicas y derivación de clave compartida secreta única por par de nodos.
- **DeliveryStatus & Cola Offline:** Introducido `DeliveryStatus` (`PENDING`, `SENT`, `DELIVERED`) en `MessageEntity` con retransmisión automática de mensajes guardados cuando un peer reconecta.
- **Persistencia en DataStore:** Almacenamiento de preferencias de bloqueo biométrico, tema y descargas automáticas.
- **Fix de Firma en Instalador:** Configuración de firma en `build.gradle.kts` e integración de `canRequestPackageInstalls()` para actualizaciones transparentes sin error de conflicto de paquetes.

---

## 🛠️ [v1.2.5] - 2026-08-03

### Estabilidad de Audio & Servicio de Fondo
- **Fix Mute/Altavoz:** `P2PAudioEngine` mantiene activo el stream de captura sin destruir `AudioRecord` durante el silenciamiento. Altavoz configurado en `MODE_IN_COMMUNICATION`.
- **Prevención de Leaks:** Reemplazados los conjuntos de deduplicación infinitos por colecciones acotadas `LinkedHashMap` (límite 3,000 elementos).
- **MeshService:** `ForegroundService` persistente registrado en el manifiesto para mantener la red mesh offline activa en segundo plano.
- **Estado de Red Híbrida:** Barra de estado visual en tiempo real (`Mesh: X peers | ☁️ Relay OK`).

---

## 📞 [v1.2.0] - 2026-08-03

### Llamadas P2P & Walkie-Talkie
- **LlamadasBidireccionales:** Transmisión de voz P2P en tiempo real usando `AudioRecord` (PCM 16kHz) y `AudioTrack`.
- **Modo Walkie-Talkie (PTT):** Botón de pulsar para hablar en chats privados y canales públicos.
- **Señalización P2P:** Transmisión de señales de control `OFFER`, `ACCEPT`, `REJECT`, `END` a través de redes Mesh y Cloud Relay.
