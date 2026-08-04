/**
 * NoSense Anonymous Cloud Relay Server
 * 
 * Zero-logs, zero-db, in-memory WebSocket message relay for NoSense.
 * Forwards E2EE encrypted packets between connected nodes based on userId.
 * Includes in-memory store-and-forward buffer for offline cloud recipients (24h TTL).
 */

const { WebSocketServer } = require('ws');
const http = require('http');

const PORT = process.env.PORT || 8080;
const MAX_BUFFER_PER_USER = 100;
const MESSAGE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain', 'Access-Control-Allow-Origin': '*' });
    res.end('NoSense Cloud Relay Operational (v1.2.0)\n');
});

const wss = new WebSocketServer({ server });

// Active connections map: userId -> Set of WebSocket clients
const clients = new Map();

// In-memory pending message buffer for offline users: userId -> Array of { packetJson, timestamp }
const offlineBuffer = new Map();

// Clean up expired offline messages every 10 minutes
setInterval(() => {
    const now = Date.now();
    let cleaned = 0;
    offlineBuffer.forEach((messages, userId) => {
        const validMessages = messages.filter(msg => (now - msg.timestamp) < MESSAGE_TTL_MS);
        if (validMessages.length === 0) {
            offlineBuffer.delete(userId);
        } else {
            offlineBuffer.set(userId, validMessages);
        }
        cleaned += (messages.length - validMessages.length);
    });
    if (cleaned > 0) {
        console.log(`[RELAY CLEANUP] Purged ${cleaned} expired offline messages from memory.`);
    }
}, 10 * 60 * 1000);

wss.on('connection', (ws, req) => {
    const urlParams = new URLSearchParams(req.url.replace(/^.*\?/, ''));
    const userId = urlParams.get('userId');

    if (!userId) {
        console.log('[RELAY] Connection rejected: Missing userId');
        ws.close(4001, 'Missing userId parameter');
        return;
    }

    ws.userId = userId;
    if (!clients.has(userId)) {
        clients.set(userId, new Set());
    }
    clients.get(userId).add(ws);
    console.log(`[RELAY] Node connected: ${userId} (Sockets: ${clients.get(userId).size})`);

    // Deliver buffered offline messages if any exist for this user
    if (offlineBuffer.has(userId)) {
        const pending = offlineBuffer.get(userId) || [];
        console.log(`[RELAY] Flushing ${pending.length} offline buffered messages to reconnected node ${userId}`);
        pending.forEach(item => {
            if (ws.readyState === 1) {
                ws.send(item.packetJson);
            }
        });
        offlineBuffer.delete(userId);
    }

    ws.on('message', (messageText) => {
        try {
            const rawStr = messageText.toString();
            const packet = JSON.parse(rawStr);
            const recipientId = packet.recipientId;

            if (!recipientId || recipientId === 'PUBLIC_CHANNEL') {
                // Broadcast channel message to all connected clients except sender
                wss.clients.forEach((client) => {
                    if (client !== ws && client.readyState === 1) {
                        client.send(rawStr);
                    }
                });
            } else {
                // Route to specific recipient userId
                const recipientSockets = clients.get(recipientId);
                if (recipientSockets && recipientSockets.size > 0) {
                    let deliveredCount = 0;
                    recipientSockets.forEach((client) => {
                        if (client.readyState === 1) {
                            client.send(rawStr);
                            deliveredCount++;
                        }
                    });
                    console.log(`[RELAY] Direct delivered packet ${packet.packetId} to ${recipientId} (${deliveredCount} socket)`);
                } else {
                    // Recipient offline on relay -> buffer in-memory for when they connect
                    if (!offlineBuffer.has(recipientId)) {
                        offlineBuffer.set(recipientId, []);
                    }
                    const userBuf = offlineBuffer.get(recipientId);
                    if (userBuf.length < MAX_BUFFER_PER_USER) {
                        userBuf.push({ packetJson: rawStr, timestamp: Date.now() });
                        console.log(`[RELAY] Recipient ${recipientId} offline. Buffered packet ${packet.packetId} in memory (Queue size: ${userBuf.length})`);
                    } else {
                        console.log(`[RELAY] Recipient ${recipientId} buffer full (${MAX_BUFFER_PER_USER} max). Dropping packet.`);
                    }
                }
            }
        } catch (e) {
            console.error('[RELAY] Invalid packet error:', e.message);
        }
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

server.listen(PORT, () => {
    console.log(`====================================================`);
    console.log(`🚀 NoSense Anonymous Relay Server (v1.2.0) running on port ${PORT}`);
    console.log(`====================================================`);
});
