/**
 * NoSense Anonymous Cloud Relay Server (v2.0.0)
 * 
 * Zero-logs, zero-db, in-memory WebSocket message relay for NoSense P2P Mesh network.
 * Features:
 * - Direct routing & offline store-and-forward buffer (24h TTL)
 * - Server-side packet deduplication (1-hour window)
 * - Multi-server cluster peer sync (PEER_SERVERS env var)
 * - REST Health & Diagnostic Endpoint (/health & /stats)
 */

const { WebSocketServer, WebSocket } = require('ws');
const http = require('http');

const PORT = process.env.PORT || 8080;
const MAX_BUFFER_PER_USER = 150;
const MESSAGE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours
const DEDUP_TTL_MS = 60 * 60 * 1000; // 1 hour

// Active connections map: userId -> Set of WebSocket clients
const clients = new Map();

// In-memory pending message buffer for offline users: userId -> Array of { packetJson, timestamp }
const offlineBuffer = new Map();

// Server-side deduplication set: packetId -> timestamp
const seenPackets = new Map();

// Sister relay servers for multi-server cluster
const peerServerUrls = process.env.PEER_SERVERS ? process.env.PEER_SERVERS.split(',').map(s => s.trim()) : [];
const peerSockets = new Map();

const server = http.createServer((req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    
    if (req.url === '/health' || req.url === '/stats') {
        let totalBuffered = 0;
        offlineBuffer.forEach(arr => totalBuffered += arr.length);
        
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            status: 'ok',
            version: '2.0.0',
            connectedNodes: clients.size,
            bufferedMessages: totalBuffered,
            dedupCacheSize: seenPackets.size,
            peerServers: peerServerUrls.length,
            uptimeSeconds: Math.floor(process.uptime())
        }, null, 2));
        return;
    }

    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('NoSense Cloud Relay Operational (v2.0.0)\n');
});

const wss = new WebSocketServer({ server });

// Automatic cleanup every 10 minutes
setInterval(() => {
    const now = Date.now();
    let cleanedBuffer = 0;
    
    offlineBuffer.forEach((messages, userId) => {
        const validMessages = messages.filter(msg => (now - msg.timestamp) < MESSAGE_TTL_MS);
        if (validMessages.length === 0) {
            offlineBuffer.delete(userId);
        } else {
            offlineBuffer.set(userId, validMessages);
        }
        cleanedBuffer += (messages.length - validMessages.length);
    });

    let cleanedDedup = 0;
    seenPackets.forEach((timestamp, packetId) => {
        if (now - timestamp > DEDUP_TTL_MS) {
            seenPackets.delete(packetId);
            cleanedDedup++;
        }
    });

    if (cleanedBuffer > 0 || cleanedDedup > 0) {
        console.log(`[RELAY CLEANUP] Purged ${cleanedBuffer} expired offline messages, ${cleanedDedup} dedup entries.`);
    }
}, 10 * 60 * 1000);

// Connect to sister relay servers for multi-server synchronization
function initPeerServers() {
    peerServerUrls.forEach(url => {
        if (!url) return;
        connectToPeerServer(url);
    });
}

function connectToPeerServer(url) {
    console.log(`[CLUSTER] Connecting to sister relay server: ${url}`);
    try {
        const ws = new WebSocket(url + '?userId=CLUSTER_RELAY_NODE');
        ws.on('open', () => {
            console.log(`[CLUSTER] Connected to sister relay server: ${url}`);
            peerSockets.set(url, ws);
        });
        ws.on('message', (data) => {
            handleRelayMessage(data.toString(), null, true);
        });
        ws.on('close', () => {
            peerSockets.delete(url);
            console.log(`[CLUSTER] Sister relay server disconnected (${url}). Retrying in 15s...`);
            setTimeout(() => connectToPeerServer(url), 15000);
        });
        ws.on('error', (err) => {
            console.error(`[CLUSTER] Error with sister server ${url}:`, err.message);
        });
    } catch (e) {
        console.error(`[CLUSTER] Exception connecting to ${url}:`, e.message);
    }
}

function handleRelayMessage(rawStr, senderWs = null, isFromClusterPeer = false) {
    let packet;
    try {
        packet = JSON.parse(rawStr);
    } catch (e) {
        return;
    }

    // Deduplication check
    if (packet.packetId) {
        if (seenPackets.has(packet.packetId)) {
            return; // Duplicate packet -> drop silently
        }
        seenPackets.set(packet.packetId, Date.now());
    }

    const recipientId = packet.recipientId;

    if (!recipientId || recipientId === 'PUBLIC_CHANNEL') {
        // Broadcast channel message
        wss.clients.forEach((client) => {
            if (client !== senderWs && client.readyState === 1) {
                client.send(rawStr);
            }
        });
    } else {
        // Route to specific recipient userId
        const recipientSockets = clients.get(recipientId);
        if (recipientSockets && recipientSockets.size > 0) {
            recipientSockets.forEach((client) => {
                if (client.readyState === 1) {
                    client.send(rawStr);
                }
            });
            console.log(`[RELAY] Direct packet ${packet.packetId} delivered to ${recipientId}`);
        } else {
            // Buffer offline message
            if (!offlineBuffer.has(recipientId)) {
                offlineBuffer.set(recipientId, []);
            }
            const userBuf = offlineBuffer.get(recipientId);
            if (userBuf.length < MAX_BUFFER_PER_USER) {
                userBuf.push({ packetJson: rawStr, timestamp: Date.now() });
                console.log(`[RELAY] Recipient ${recipientId} offline. Buffered packet ${packet.packetId}`);
            }
        }
    }

    // Forward to sister cluster relay servers if not coming from one
    if (!isFromClusterPeer && peerSockets.size > 0) {
        peerSockets.forEach((ws) => {
            if (ws.readyState === 1) {
                ws.send(rawStr);
            }
        });
    }
}

wss.on('connection', (ws, req) => {
    const urlParams = new URLSearchParams(req.url.replace(/^.*\?/, ''));
    const userId = urlParams.get('userId');

    if (!userId) {
        ws.close(4001, 'Missing userId parameter');
        return;
    }

    ws.userId = userId;
    if (!clients.has(userId)) {
        clients.set(userId, new Set());
    }
    clients.get(userId).add(ws);
    console.log(`[RELAY] Node connected: ${userId}`);

    // Deliver buffered offline messages if any exist
    if (offlineBuffer.has(userId)) {
        const pending = offlineBuffer.get(userId) || [];
        console.log(`[RELAY] Flushing ${pending.length} offline buffered messages to node ${userId}`);
        pending.forEach(item => {
            if (ws.readyState === 1) {
                ws.send(item.packetJson);
            }
        });
        offlineBuffer.delete(userId);
    }

    ws.on('message', (messageText) => {
        handleRelayMessage(messageText.toString(), ws, false);
    });

    ws.on('close', () => {
        const userSockets = clients.get(userId);
        if (userSockets) {
            userSockets.delete(ws);
            if (userSockets.size === 0) {
                clients.delete(userId);
            }
        }
        console.log(`[RELAY] Node disconnected: ${userId}`);
    });

    ws.on('error', (err) => {
        console.error(`[RELAY] Socket error for ${userId}:`, err.message);
    });
});

initPeerServers();

server.listen(PORT, () => {
    console.log(`====================================================`);
    console.log(`🚀 NoSense Anonymous Relay Server (v2.0.0) running on port ${PORT}`);
    console.log(`====================================================`);
});
