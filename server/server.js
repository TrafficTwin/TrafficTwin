const express = require('express');
const { MongoClient } = require('mongodb');
const cors = require('cors');
const http = require('http');
const WebSocket = require('ws');

const app = express();
app.use(cors());
app.use(express.json());

const client = new MongoClient('mongodb://127.0.0.1:27017');
let db;

async function connect() {
    await client.connect();
    db = client.db('traffic_twin');
    console.log("Povezan z MongoDB");
}

connect().catch(err => console.error("Napaka pri povezavi:", err));

const col = () => db.collection('parkings');
const roadCol = () => db.collection('road_states');

const server = http.createServer(app);

const wss = new WebSocket.Server({
    server,
    path: '/ws'
});

function sendJson(ws, data) {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({
            ...data,
            sentAt: new Date().toISOString()
        }));
    }
}

async function getParkingSnapshot() {
    return await col()
        .find({}, { projection: { _id: 0 } })
        .toArray();
}

async function getRoadSnapshot() {
    return await roadCol()
        .find({}, { projection: { _id: 0 } })
        .toArray();
}

async function sendFullSnapshot(ws) {
    if (!db) {
        sendJson(ws, {
            type: "error",
            message: "Baza še ni pripravljena."
        });
        return;
    }

    const [parkings, roads] = await Promise.all([
        getParkingSnapshot(),
        getRoadSnapshot()
    ]);

    sendJson(ws, {
        type: "snapshot",
        parkings,
        roads
    });
}

wss.on('connection', async ws => {
    console.log("WebSocket klient povezan.");

    ws.isAlive = true;

    ws.on('pong', () => {
        ws.isAlive = true;
    });

    ws.on('message', async raw => {
        try {
            const message = JSON.parse(raw.toString());

            if (message.type === "ping") {
                sendJson(ws, { type: "pong" });
                return;
            }

            if (message.type === "snapshot:request") {
                await sendFullSnapshot(ws);
                return;
            }

            sendJson(ws, {
                type: "error",
                message: "Neznan WebSocket ukaz."
            });
        } catch (err) {
            sendJson(ws, {
                type: "error",
                message: "Neveljaven WebSocket JSON."
            });
        }
    });

    ws.on('close', () => {
        console.log("WebSocket klient odklopljen.");
    });

    await sendFullSnapshot(ws);
});

setInterval(() => {
    wss.clients.forEach(ws => {
        if (ws.isAlive === false) {
            return ws.terminate();
        }

        ws.isAlive = false;
        ws.ping();
    });
}, 30000);

// -------------------- PARKIRIŠČA --------------------

app.get('/api/parking', async (req, res) => {
    try {
        const data = await getParkingSnapshot();
        res.json(data);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/parking', async (req, res) => {
    try {
        await col().insertOne(req.body);
        res.status(201).json({ message: "Dodano." });
    } catch (err) {
        if (err.code === 11000) {
            res.status(409).json({ error: "ID že obstaja." });
        } else {
            res.status(500).json({ error: err.message });
        }
    }
});

app.post('/api/parking/sync', async (req, res) => {
    try {
        if (!Array.isArray(req.body)) {
            return res.status(400).json({ error: "Pričakovan je seznam parkirišč." });
        }

        console.log("Sinhroniziram " + req.body.length + " zapisov...");

        await col().deleteMany({});

        const docs = req.body.map(item => ({
            ...item,
            lastUpdated: new Date()
        }));

        if (docs.length > 0) {
            await col().insertMany(docs);
        }

        res.json({ message: "Sinhronizirano " + docs.length + " zapisov." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/parking/:id', async (req, res) => {
    try {
        const id = parseInt(req.params.id);

        const result = await col().findOneAndUpdate(
            { id },
            {
                $set: {
                    ...req.body,
                    id,
                    lastUpdated: new Date()
                }
            },
            { returnDocument: 'after' }
        );

        if (!result) {
            return res.status(404).json({ error: "Ni najdeno." });
        }

        res.json({ message: "Posodobljeno." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/parking/:id', async (req, res) => {
    try {
        const id = parseInt(req.params.id);

        const result = await col().findOneAndDelete({ id });

        if (!result) {
            return res.status(404).json({ error: "Ni najdeno." });
        }

        res.json({ message: "Izbrisano." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/parking', async (req, res) => {
    try {
        await col().deleteMany({});
        res.json({ message: "Baza izpraznjena." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// -------------------- STANJE CEST --------------------

app.get('/api/stanje-cest', async (req, res) => {
    try {
        const data = await getRoadSnapshot();
        res.json(data);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/stanje-cest/sync', async (req, res) => {
    try {
        if (!Array.isArray(req.body)) {
            return res.status(400).json({ error: "Pričakovan je seznam stanj cest." });
        }

        console.log("Sinhroniziram stanje cest: " + req.body.length + " zapisov...");

        await roadCol().deleteMany({});

        const docs = req.body.map(item => ({
            tip: item.tip ?? "",
            relacija: item.relacija ?? "",
            stanje: item.stanje ?? "",
            lastUpdated: new Date()
        }));

        if (docs.length > 0) {
            await roadCol().insertMany(docs);
        }

        res.json({ message: "Sinhronizirano " + docs.length + " zapisov stanja cest." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/stanje-cest', async (req, res) => {
    try {
        await roadCol().deleteMany({});
        res.json({ message: "Stanje cest izbrisano." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

server.listen(3000, () => {
    console.log("Strežnik teče na http://localhost:3000");
    console.log("WebSocket teče na ws://localhost:3000/ws");
});