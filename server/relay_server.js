/**
 * NoSense Anonymous Cloud Relay Server
 * 
 * Zero-logs, zero-db, in-memory WebSocket message relay for NoSense.
 * Forwards E2EE encrypted packets between connected nodes based on userId.
 */

const { WebSocketServer } = require('ws');
const http = require('http');

const PORT = process.env.PORT || 8080;
const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('NoSense Cloud Relay Operational\n');
});

const wss = new WebSocketServer({ server });

// Active connections map: userId -> Set of WebSocket clients
const clients = new Map();

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
    console.log(`[RELAY] Node connected: ${userId} (Active sockets for user: ${clients.get(userId).size})`);

    ws.on('message', (messageText) => {
        try {
            const packet = JSON.parse(messageText.toString());
            const recipientId = packet.recipientId;

            if (!recipientId || recipientId === 'PUBLIC_CHANNEL') {
                // Broadcast to all connected clients except sender
                wss.clients.forEach((client) => {
                    if (client !== ws && client.readyState === 1) {
                        client.send(messageText.toString());
                    }
                });
            } else {
                // Route to specific recipient userId
                const recipientSockets = clients.get(recipientId);
                if (recipientSockets && recipientSockets.size > 0) {
                    recipientSockets.forEach((client) => {
                        if (client.readyState === 1) {
                            client.send(messageText.toString());
                        }
                    });
                    console.log(`[RELAY] Delivered packet ${packet.packetId} from ${userId} to ${recipientId}`);
                } else {
                    console.log(`[RELAY] Recipient ${recipientId} offline on relay`);
                }
            }
        } catch (e) {
            console.error('[RELAY] Invalid packet received:', e.message);
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
    console.log(`🚀 NoSense Anonymous Relay Server running on port ${PORT}`);
    console.log(`====================================================`);
});
