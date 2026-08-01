# Project Plan

Extend 'NoSense' to include user profile onboarding and support for various multimedia message types (images, video, files, audio) over the mesh network.

## Project Brief

NoSense is an offline-first mesh messaging application.
Current features: P2P mesh networking, anonymous identity, private/group chats, channels, and statuses.
New Requirements:
- Initial configuration screen for profile setup (username).
- Support for multimedia messages: images, videos, files, and audio.
- UI/UX refinements for these new features.

## Implementation Steps
**Total Duration:** 2h 2m 43s

### Task_1_Core_Setup: Configure project dependencies and core infrastructure. Add Hilt for DI and Google Nearby Connections. Setup Room database for persistent storage (messages, chats, statuses) and DataStore for anonymous identity management.
- **Status:** COMPLETED
- **Updates:** Infrastructure setup complete. Hilt, Room, and DataStore are configured. Identity generation (UUID + Username) is implemented. Adaptive icon created. Project builds successfully.
- **Acceptance Criteria:**
  - Project builds successfully with Hilt and Nearby Connections dependencies
  - Room database schema is defined for Chats, Messages, and Statuses
  - Anonymous identity (username + ID) is generated and stored in DataStore

### Task_2_Mesh_Networking: Implement the P2P networking layer using Google Nearby Connections. Create a manager to handle device discovery, connection lifecycle, and data transfer for the mesh network.
- **Status:** COMPLETED
- **Updates:** P2P networking layer implemented using Google Nearby Connections (P2P_CLUSTER strategy). MeshManager handles discovery, connection, and data transfer (MeshPacket). Permissions handler and Hilt integration for networking are ready. Project builds successfully.
- **Acceptance Criteria:**
  - Devices can discover each other via Wi-Fi/Bluetooth
  - Connections are established automatically or via prompt
  - Basic data packets can be sent and received between nodes
- **Duration:** 5m 59s

### Task_3_Messaging_Feature: Develop the messaging UI and logic. Implement Chat List and Chat Detail screens using Jetpack Compose M3. Handle private and group message routing and local storage.
- **Status:** COMPLETED
- **Updates:** Messaging UI and logic implemented. Chat List and Chat Detail screens are functional. Messages are persisted in Room and sent/received via MeshManager. Jetpack Navigation and M3 styling are integrated. Edge-to-edge support added.
- **Acceptance Criteria:**
  - UI follows Material Design 3 guidelines
  - Private and group chats are functional offline
  - Messages are persisted in Room and displayed correctly in the UI
- **Duration:** 7m 25s

### Task_4_Broadcasts_Statuses: Implement local broadcast channels and the Statuses (Stories) feature. Messages in broadcast channels should propagate to all nearby nodes, and statuses should sync across the local mesh.
- **Status:** COMPLETED
- **Updates:** Local broadcast channels and Statuses (Stories) features implemented. Messages in channels are broadcasted to all nearby nodes. Statuses are propagated through the mesh, stored in Room, and expire after 24 hours. Bottom navigation added for Chats, Status, and Channels. UI follows M3.
- **Acceptance Criteria:**
  - Local broadcast channel allows sending messages to all nearby users
  - Statuses can be created and are propagated to connected devices
  - UI for viewing and posting statuses is implemented
- **Duration:** 2m 20s

### Task_5_Polish_and_Verify: Apply final UI/UX refinements: Material 3 vibrant color scheme, Edge-to-Edge display, and adaptive app icon. Conduct a final run to verify mesh stability and application flow.
- **Status:** COMPLETED
- **Updates:** Vibrant M3 theme applied. Edge-to-Edge is fully implemented. Adaptive app icon created. The critic verified the app's stability, UI, and permissions flow. P2P discovery infrastructure is active. App is ready for physical device testing.
- **Acceptance Criteria:**
  - Vibrant M3 theme applied (light/dark)
  - App icon is adaptive and matches the core function
  - Edge-to-Edge is fully implemented
  - App builds, runs without crashes, and P2P communication is verified by the critic
- **Duration:** 1h 39m 48s

### Task_6_Onboarding_and_Multimedia_Logic: Implement the initial onboarding screen for user profile setup (username) using DataStore. Update the Room database schema and networking layer (Nearby Connections) to handle multimedia message types: images, videos, audio, and files.
- **Status:** COMPLETED
- **Updates:** Onboarding screen and multimedia data logic implemented. Onboarding saves username to DataStore and is shown on first launch. Room schema updated for files. MeshManager extended for Payload.fromFile transfers. Project builds successfully.
- **Acceptance Criteria:**
  - Onboarding screen appears on first launch and saves username to DataStore
  - Database schema is updated to support multimedia message metadata
  - Networking layer is extended to handle file payloads via Google Nearby Connections
- **Duration:** 7m 11s

### Task_7_Multimedia_UI_and_Run_Verify: Create UI components for multimedia selection and display in Chat screens. Implement image/video previews, audio playback, and file interaction. Perform a final verification of application stability and feature alignment.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Users can send and view images, videos, audio, and files in chats
  - Material 3 UI is maintained for new multimedia components
  - Make sure all existing tests pass
  - Build pass and app does not crash
  - Critic verifies multimedia transfer and onboarding flow
- **StartTime:** 2026-08-01 16:16:18 COT

