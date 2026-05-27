const express = require('express');
const { MongoClient } = require('mongodb');
const cors = require('cors');
const http = require('http');
const WebSocket = require('ws');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

const mongoUri = process.env.MONGO_URI || 'mongodb://127.0.0.1:27017';
const client = new MongoClient(mongoUri);

const PORT = process.env.PORT || 3000;

const server = http.createServer(app);

const wss = new WebSocket.Server({
    server,
    path: '/ws'
});

let db;

const col = () => {
    if (!db) throw new Error("Baza podatkov še ni pripravljena.");
    return db.collection('parkings');
};

const roadCol = () => {
    if (!db) throw new Error("Baza podatkov še ni pripravljena.");
    return db.collection('road_states');

    const parkingProjection = { _id: 0, locationGeo: 0 };

    function toNumber(value) {
        const number = typeof value === 'number' ? value : Number(value);
        return Number.isFinite(number) ? number : null;
    }

    function hasValidCoordinates(latitude, longitude) {
        return latitude !== null && longitude !== null &&
            latitude >= -90 && latitude <= 90 &&
            longitude >= -180 && longitude <= 180;
    }

    function normalizeParkingDoc(raw) {
        const latitude = toNumber(raw.latitude);
        const longitude = toNumber(raw.longitude);

        const doc = {
            ...raw,
            latitude,
            longitude,
            lastUpdated: new Date()
        };

        delete doc.locationGeo;
        delete doc.distanceMeters;

        if (hasValidCoordinates(latitude, longitude)) {
            doc.locationGeo = {
                type: 'Point',
                coordinates: [longitude, latitude]
            };
        }

        return doc;
    }
};

function createWsPayload(data) {
    return JSON.stringify({
        ...data,
        sentAt: new Date().toISOString()
    });
}

function sendJson(ws, data) {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(createWsPayload(data));
    }
}

function broadcastJson(data) {
    const payload = createWsPayload(data);
    wss.clients.forEach(ws => {
        if (ws.readyState === WebSocket.OPEN) {
            ws.send(payload);
        }
    });
}

async function getParkingSnapshot() {
    return await col().find({}, { projection: parkingProjection }).toArray();
}

async function getRoadSnapshot() {
    return await roadCol().find({}, { projection: { _id: 0 } }).toArray();
}

async function getFullSnapshotPayload() {
    const [parkings, roads] = await Promise.all([
        getParkingSnapshot(),
        getRoadSnapshot()
    ]);
    return {
        type: "snapshot",
        parkings,
        roads
    };
}

async function sendFullSnapshot(ws) {
    if (!db) {
        sendJson(ws, { type: "error", message: "Baza še ni pripravljena." });
        return;
    }
    const payload = await getFullSnapshotPayload();
    sendJson(ws, payload);
}

async function broadcastFullSnapshot(reason) {
    if (!db) return;
    const payload = await getFullSnapshotPayload();
    broadcastJson({
        ...payload,
        reason
    });
}

async function broadcastParkingEvent(type, data = {}) {
    broadcastJson({
        type,
        resource: "parking",
        ...data
    });
    await broadcastFullSnapshot(type);
}

async function broadcastTrafficEvent(type, data = {}) {
    broadcastJson({
        type,
        resource: "traffic",
        ...data
    });
    await broadcastFullSnapshot(type);
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
            sendJson(ws, { type: "error", message: "Neznan WebSocket ukaz." });
        } catch (err) {
            sendJson(ws, { type: "error", message: "Neveljaven WebSocket JSON." });
        }
    });

    ws.on('close', () => {
        console.log("WebSocket klient odklopljen.");
    });

    await sendFullSnapshot(ws);
});

setInterval(() => {
    wss.clients.forEach(ws => {
        if (ws.isAlive === false) return ws.terminate();
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
        const doc = normalizeParkingDoc(req.body);
        await col().insertOne(doc);
        await broadcastParkingEvent("parking:created", {
            parking: { ...doc, _id: undefined }
        });
        res.status(201).json({ message: "Dodano." });
    } catch (err) {
        if (err.code === 11000) res.status(409).json({ error: "ID že obstaja." });
        else res.status(500).json({ error: err.message });
    }
});

app.post('/api/parking/sync', async (req, res) => {
    try {
        if (!Array.isArray(req.body)) {
            return res.status(400).json({ error: "Pričakovan je seznam parkirišč." });
        }
        console.log("Sinhroniziram " + req.body.length + " zapisov...");
        await col().deleteMany({});
        const docs = req.body.map(item => normalizeParkingDoc(item));
        if (docs.length > 0) await col().insertMany(docs);
        await broadcastParkingEvent("parking:synced", { count: docs.length });
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
            { $set: normalizeParkingDoc({ ...req.body, id }) },
            { returnDocument: 'after', projection: parkingProjection }
        );
        if (!result) return res.status(404).json({ error: "Ni najdeno." });
        await broadcastParkingEvent("parking:updated", { id, parking: result });
        res.json({ message: "Posodobljeno." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/parking/:id', async (req, res) => {
    try {
        const id = parseInt(req.params.id);
        const result = await col().findOneAndDelete({ id }, { projection: { _id: 0 } });
        if (!result) return res.status(404).json({ error: "Ni najdeno." });
        await broadcastParkingEvent("parking:deleted", { id, parking: result });
        res.json({ message: "Izbrisano." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/parking', async (req, res) => {
    try {
        const result = await col().deleteMany({});
        await broadcastParkingEvent("parking:cleared", { deletedCount: result.deletedCount });
        res.json({ message: "Baza izpraznjena." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// -------------------- STANJE CEST / PROMETNI UPDATE --------------------

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
        if (docs.length > 0) await roadCol().insertMany(docs);
        await broadcastTrafficEvent("traffic:synced", {
            count: docs.length,
            roads: docs.map(item => ({
                tip: item.tip,
                relacija: item.relacija,
                stanje: item.stanje,
                lastUpdated: item.lastUpdated
            }))
        });
        res.json({ message: "Sinhronizirano " + docs.length + " zapisov stanja cest." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/stanje-cest', async (req, res) => {
    try {
        const result = await roadCol().deleteMany({});
        await broadcastTrafficEvent("traffic:cleared", { deletedCount: result.deletedCount });
        res.json({ message: "Stanje cest izbrisano." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

async function startServer() {
    try {
        await client.connect();
        db = client.db('traffic_twin');
        await col().createIndex({ locationGeo: '2dsphere' });
        console.log("Uspešno povezan z MongoDB Atlas.");
        
        const publicUrl = process.env.PUBLIC_URL || "localhost";
        
        server.listen(PORT, () => {
            console.log("Strežnik teče na portu " + PORT);
            console.log("WebSocket teče na ws://" + publicUrl + ":" + PORT + "/ws");
        });
    } catch (err) {
        console.error("Kritična napaka pri povezavi z bazo:", err);
        process.exit(1);
    }
}

startServer();