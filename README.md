# ⚡ NoSense

<p align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.png" width="128" height="128" alt="NoSense Logo" />
</p>

<p align="center">
  <b>Aplicación de Mensajería P2P Decentralizada, Anónima y de Transporte Híbrido (Mesh + Nube Relay)</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple.svg" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Security-E2EE%20AES--256--GCM-red.svg" alt="Security" />
  <img src="https://img.shields.io/badge/Architecture-Clean%20%2B%20Hilt-orange.svg" alt="Architecture" />
  <img src="https://img.shields.io/badge/Network-P2P%20Mesh%20%2B%20WebSockets-brightgreen.svg" alt="Network" />
  <a href="https://github.com/5u17im/sturdy-parakeet/releases/latest"><img src="https://img.shields.io/github/v/release/5u17im/sturdy-parakeet?color=blue&label=Ultima%20Release" alt="Latest Release" /></a>
</p>

<p align="center">
  <a href="https://github.com/5u17im/sturdy-parakeet/releases/latest"><b>📥 Descargar Última Versión APK (GitHub Releases)</b></a>
</p>

---

## 📖 Descripción General

**NoSense** es una plataforma de mensajería libre, anónima y resiliente diseñada para comunicarse bajo cualquier circunstancia: con o sin infraestructura de Internet. Utiliza un motor de **transporte híbrido inteligente** que combina redes P2P Mesh de corto alcance con relés anónimos en la nube.

Si no hay Internet, los dispositivos se descubren automáticamente por **Wi-Fi Direct / Bluetooth (Nearby Connections)** creando un enmallado dinámico. Si un dispositivo cercano se conecta a Internet, actúa automáticamente como **Gateway (Puente)** para los nodos fuera de línea, permitiéndoles enviar y recibir mensajes globales sin revelar su contenido.

---

## ✨ Características Principales

- 🌐 **Transporte Híbrido P2P & Cloud Relay**: Transmisión fluida sin interrupciones. Con conmuta automáticamente entre Nearby Connections (P2P local) y WebSockets (Internet).
- 🌉 **Nodo Gateway de Retransmisión**: Los nodos conectados a Internet sirven como puente transparente para entregar mensajes de dispositivos offline a la red global.
- 🔒 **Cifrado Extremo a Extremo (E2EE)**: Cifrado AES-256-GCM con claves generadas en el hardware seguro (`AndroidKeyStore`). Los relés y gateways intermedios jamás pueden leer los mensajes.
- 📸 **Historias y Estados (24h)**: Publicación de texto y fotografías en estados efímeros que se difunden por la red Mesh.
- 💬 **Canales Públicos y Mensajes Privados**: Chat global comunitario de canal público y chats directos entre pares.
- 📁 **Transferencia Eficiente de Archivos y Fotos**: Compresión inteligente JPEG antes del envío para entregas instantáneas.
- 🎨 **Diseño Moderno & Neomorfismo**: Interfaz construida 100% con Jetpack Compose, animaciones fluidas y modo oscuro nativo.
- 👤 **Identidad Criptográfica Anónima**: Sin números de teléfono, sin correos electrónicos, sin registro de datos personales.

---

## 🏗️ Arquitectura de Transporte Híbrido

```
                               ┌─────────────────────────────────┐
                               │   Servidor Relé Cloud Anónimo   │
                               │     (WebSockets - Zero DB)      │
                               └────────────────┬────────────────┘
                                                │ (Internet)
                     ┌──────────────────────────┴──────────────────────────┐
                     │                                                     │
            ┌────────▼────────┐                                   ┌────────▼────────┐
            │ Dispositivo A   │                                   │ Dispositivo B   │
            │ (Con Internet)  │                                   │ (Con Internet)  │
            └────────┬────────┘                                   └─────────────────┘
                     │ (Wi-Fi Direct / Bluetooth Mesh)
            ┌────────▼────────┐
            │ Dispositivo C   │
            │ (SIN Internet)  │
            └─────────────────┘
```

---

## 🛠️ Tecnologías Utilizadas

- **Lenguaje**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) con Material 3
- **Inyección de Dependencias**: [Hilt / Dagger](https://dagger.dev/hilt/)
- **Base de Datos Local**: [Room Database](https://developer.android.com/training/data-storage/room) con cifrado de entidad
- **Protocolo Mesh Local**: [Google Nearby Connections API](https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient) (Estrategia P2P Cluster)
- **Protocolo Cloud Relay**: WebSockets mediante [OkHttp](https://square.github.io/okhttp/)
- **Cifrado & Seguridad**: `AndroidKeyStore`, AES-256-GCM, SHA-256
- **Carga de Imágenes**: [Coil](https://coil-kt.github.io/coil/)

---

## 🚀 Compilación e Instalación

### Requisitos Previos
- Android Studio Ladybug (o superior) / JDK 17
- Android SDK 34 o superior
- Dispositivo Android con Android 13 (API 33) o superior

### Pasos para Compilar

1. **Clonar el repositorio**:
   ```bash
   git clone https://github.com/5u17im/NoSense.git
   cd NoSense
   ```

2. **Compilar el APK de depuración**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Instalar en un dispositivo vía ADB**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🌐 Servidor Relé Nube (Opcional)

NoSense incluye un servidor relé anónimo escrito en Node.js sin persistencia de datos (zero-logs).

Para desplegar tu propio servidor relé:

```bash
cd server
npm install
npm start
```

El servidor escuchará en el puerto `8080` ofreciendo la conexión WebSocket `/ws`.

---

## 🛡️ Privacidad & Anonimato

NoSense fue construido bajo el principio de **Privacidad por Diseño**:
- **Cero registros**: No almacena historial ni metadatos en servidores centralizados.
- **Identidad generada localmente**: Cada usuario posee un `UUID` y clave RSA/AES almacenada únicamente en su chip seguro.
- **Red Resiliente**: Funciona en situaciones de desastres naturales, censura o cortes de infraestructura de Internet.

---

## 📄 Licencia

Este proyecto está distribuido bajo la licencia MIT. Consulta el archivo `LICENSE` para más detalles.
