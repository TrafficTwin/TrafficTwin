const express = require('express');
const { MongoClient } = require('mongodb');
const cors = require('cors');
const http = require('http');
const WebSocket = require('ws');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
require('dotenv').config();

const app = express();
app.use(cors({
    origin: "*",
    credentials: false
}));
app.use(express.json());

const mongoUri = process.env.MONGO_URI || 'mongodb://127.0.0.1:27017';
const client = new MongoClient(mongoUri);
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET;

if (!JWT_SECRET) {
    console.error("NAPAKA: JWT_SECRET ni nastavljen v .env!");
    process.exit(1);
}

const server = http.createServer(app);
const wss = new WebSocket.Server({ server, path: '/ws' });

let db;

// -------------------- KOLEKCIJE --------------------

const col = () => {
    if (!db) throw new Error("Baza podatkov še ni pripravljena.");
    return db.collection('parkings');
};

const roadCol = () => {
    if (!db) throw new Error("Baza podatkov še ni pripravljena.");
    return db.collection('road_states');
};

const userCol = () => {
    if (!db) throw new Error("Baza podatkov še ni pripravljena.");
    return db.collection('users');
};

// -------------------- POMOŽNE FUNKCIJE --------------------

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
    const doc = { ...raw, latitude, longitude, lastUpdated: new Date() };
    delete doc.locationGeo;
    delete doc.distanceMeters;
    if (hasValidCoordinates(latitude, longitude)) {
        doc.locationGeo = { type: 'Point', coordinates: [longitude, latitude] };
    }
    return doc;
}

function slugify(value) {
    return String(value ?? "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "");
}

function createRoadId(item, index = 0) {
    const slug = slugify(`${item.tip ?? ""}-${item.relacija ?? ""}`);
    return slug ? `road-${slug}` : `road-${index}`;
}

function normalizePoint(point) {
    if (Array.isArray(point)) {
        const latitude = toNumber(point[0]);
        const longitude = toNumber(point[1]);

        if (hasValidCoordinates(latitude, longitude)) {
            return [latitude, longitude];
        }

        return null;
    }

    const latitude = toNumber(point.latitude);
    const longitude = toNumber(point.longitude);

    if (hasValidCoordinates(latitude, longitude)) {
        return [latitude, longitude];
    }

    return null;
}

function normalizeRoadCoordinates(item) {
    const rawCoordinates = Array.isArray(item.coordinates)
        ? item.coordinates
        : Array.isArray(item.polyline)
            ? item.polyline
            : [];

    return rawCoordinates
        .map(normalizePoint)
        .filter(Boolean);
}

function normalizeRoadDoc(item, index = 0) {
    const latitude = toNumber(item.latitude);
    const longitude = toNumber(item.longitude);
    const hasPoint = hasValidCoordinates(latitude, longitude);

    return {
        id: item.id ?? createRoadId(item, index),
        tip: item.tip ?? "",
        relacija: item.relacija ?? "",
        stanje: item.stanje ?? "",
        latitude: hasPoint ? latitude : null,
        longitude: hasPoint ? longitude : null,
        coordinates: normalizeRoadCoordinates(item),
        lastUpdated: new Date()
    };
}

function normalizePoint(point) {
    if (Array.isArray(point)) {
        const latitude = toNumber(point[0]);
        const longitude = toNumber(point[1]);

        if (hasValidCoordinates(latitude, longitude)) {
            return [latitude, longitude];
        }

        return null;
    }

    const latitude = toNumber(point.latitude);
    const longitude = toNumber(point.longitude);

    if (hasValidCoordinates(latitude, longitude)) {
        return [latitude, longitude];
    }

    return null;
}

function normalizeRoadCoordinates(item) {
    const rawCoordinates = Array.isArray(item.coordinates)
        ? item.coordinates
        : Array.isArray(item.polyline)
            ? item.polyline
            : [];

    return rawCoordinates
        .map(normalizePoint)
        .filter(Boolean);
}

function normalizeRoadDoc(item) {
    const latitude = toNumber(item.latitude);
    const longitude = toNumber(item.longitude);
    const hasPoint = hasValidCoordinates(latitude, longitude);

    return {
        tip: item.tip ?? "",
        relacija: item.relacija ?? "",
        stanje: item.stanje ?? "",
        latitude: hasPoint ? latitude : null,
        longitude: hasPoint ? longitude : null,
        coordinates: normalizeRoadCoordinates(item),
        lastUpdated: new Date()
    };
}

// -------------------- JWT MIDDLEWARE --------------------

function requireAuth(req, res, next) {
    const authHeader = req.headers.authorization;
    if (!authHeader?.startsWith("Bearer ")) {
        return res.status(401).json({ error: "Manjka avtentikacijski token." });
    }
    try {
        req.user = jwt.verify(authHeader.slice(7), JWT_SECRET);
        next();
    } catch (err) {
        return res.status(401).json({ error: err.name === "TokenExpiredError" ? "Token je potekel." : "Neveljaven token." });
    }
}

function requireAdmin(req, res, next) {
    if (req.user?.role !== "admin") {
        return res.status(403).json({ error: "Dostop dovoljen samo administratorjem." });
    }
    next();
}

function getCurrentUserEmail(req) {
    return String(req.user?.sub ?? req.user?.email ?? "").toLowerCase();
}

async function ensureCurrentUserDoc(req) {
    const email = getCurrentUserEmail(req);

    if (!email) {
        throw new Error("Uporabnik nima e-pošte v tokenu.");
    }

    await userCol().updateOne(
        { email },
        {
            $setOnInsert: {
                email,
                name: req.user?.name ?? email,
                role: req.user?.role ?? "user",
                favouriteParkings: [],
                favouriteRoads: []
            }
        },
        { upsert: true }
    );

    return userCol().findOne({ email });
}

async function buildUserProfile(req) {
    const user = await ensureCurrentUserDoc(req);

    const favouriteParkingIds = Array.isArray(user.favouriteParkings)
        ? user.favouriteParkings.map(Number).filter(Number.isFinite)
        : [];

    const favouriteRoadIds = Array.isArray(user.favouriteRoads)
        ? user.favouriteRoads.map(String)
        : [];

    const [favouriteParkings, favouriteRoads] = await Promise.all([
        favouriteParkingIds.length > 0
            ? col().find(
                { id: { $in: favouriteParkingIds } },
                { projection: parkingProjection }
            ).toArray()
            : [],

        favouriteRoadIds.length > 0
            ? roadCol().find(
                { id: { $in: favouriteRoadIds } },
                { projection: { _id: 0 } }
            ).toArray()
            : []
    ]);

    return {
        id: user._id?.toString(),
        name: user.name ?? req.user?.name ?? "",
        email: user.email,
        role: user.role ?? req.user?.role ?? "user",
        favouriteParkingIds,
        favouriteRoadIds,
        favouriteParkings,
        favouriteRoads
    };
}

// -------------------- AUTH --------------------

app.post('/api/auth/register', async (req, res) => {
    const { email, password, name } = req.body ?? {};

    if (!email || !password || !name) {
        return res.status(400).json({ error: "Vsa polja so obvezna." });
    }

    try {
        const existing = await userCol().findOne({ email: email.toLowerCase() });
        if (existing) {
            return res.status(409).json({ error: "Uporabnik s tem e-poštnim naslovom že obstaja." });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        await userCol().insertOne({
            email: email.toLowerCase(),
            password: hashedPassword,
            role: "user",
            name,
            favouriteParkings: [],
            favouriteRoads: []
        });

        res.status(201).json({ message: "Registracija uspešna." });
    } catch (err) {
        res.status(500).json({ error: "Napaka pri registraciji: " + err.message });
    }
});

app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body ?? {};

    if (!email || !password) {
        return res.status(400).json({ error: "E-pošta in geslo sta obvezna." });
    }

    // ── ENV admin — neodvisen od baze ─────────────────────
    if (email.toLowerCase() === process.env.ADMIN_EMAIL?.toLowerCase() &&
        password === process.env.ADMIN_PASSWORD) {
        const token = jwt.sign(
            { sub: email, role: "admin", name: "Admin" },
            JWT_SECRET,
            { expiresIn: "8h" }
        );
        return res.json({ token, user: { email, role: "admin", name: "Admin" } });
    }

    // ── Navadni userji iz baze ─────────────────────────────
    try {
        const user = await userCol().findOne({ email: email.toLowerCase() });
        if (!user) return res.status(401).json({ error: "Napačen e-poštni naslov ali geslo." });

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) return res.status(401).json({ error: "Napačen e-poštni naslov ali geslo." });

        const token = jwt.sign(
            { sub: user.email, role: user.role, name: user.name },
            JWT_SECRET,
            { expiresIn: "8h" }
        );
        res.json({ token, user: { email: user.email, role: user.role, name: user.name } });
    } catch (err) {
        res.status(500).json({ error: "Napaka strežnika: " + err.message });
    }
});

app.get('/api/users/me', requireAuth, async (req, res) => {
    try {
        res.json(await buildUserProfile(req));
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/users/me/favourites/parking/:parkingId', requireAuth, async (req, res) => {
    try {
        const parkingId = Number(req.params.parkingId);

        if (!Number.isFinite(parkingId)) {
            return res.status(400).json({ error: "Neveljaven ID parkirišča." });
        }

        const parking = await col().findOne({ id: parkingId });

        if (!parking) {
            return res.status(404).json({ error: "Parkirišče ne obstaja." });
        }

        const email = getCurrentUserEmail(req);
        await ensureCurrentUserDoc(req);

        await userCol().updateOne(
            { email },
            { $addToSet: { favouriteParkings: parkingId } }
        );

        res.json(await buildUserProfile(req));
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/users/me/favourites/parking/:parkingId', requireAuth, async (req, res) => {
    try {
        const parkingId = Number(req.params.parkingId);

        if (!Number.isFinite(parkingId)) {
            return res.status(400).json({ error: "Neveljaven ID parkirišča." });
        }

        const email = getCurrentUserEmail(req);
        await ensureCurrentUserDoc(req);

        await userCol().updateOne(
            { email },
            { $pull: { favouriteParkings: parkingId } }
        );

        res.json(await buildUserProfile(req));
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/users/me/favourites/road/:roadId', requireAuth, async (req, res) => {
    try {
        const roadId = String(req.params.roadId);

        const road = await roadCol().findOne({ id: roadId });

        if (!road) {
            return res.status(404).json({ error: "Cesta ne obstaja." });
        }

        const email = getCurrentUserEmail(req);
        await ensureCurrentUserDoc(req);

        await userCol().updateOne(
            { email },
            { $addToSet: { favouriteRoads: roadId } }
        );

        res.json(await buildUserProfile(req));
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/users/me/favourites/road/:roadId', requireAuth, async (req, res) => {
    try {
        const roadId = String(req.params.roadId);

        const email = getCurrentUserEmail(req);
        await ensureCurrentUserDoc(req);

        await userCol().updateOne(
            { email },
            { $pull: { favouriteRoads: roadId } }
        );

        res.json(await buildUserProfile(req));
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// -------------------- WEBSOCKET --------------------

function createWsPayload(data) {
    return JSON.stringify({ ...data, sentAt: new Date().toISOString() });
}

function sendJson(ws, data) {
    if (ws.readyState === WebSocket.OPEN) ws.send(createWsPayload(data));
}

function broadcastJson(data) {
    const payload = createWsPayload(data);
    wss.clients.forEach(ws => { if (ws.readyState === WebSocket.OPEN) ws.send(payload); });
}

async function getParkingSnapshot() {
    return await col().find({}, { projection: parkingProjection }).toArray();
}

async function getRoadSnapshot() {
    return await roadCol().find({}, { projection: { _id: 0 } }).toArray();
}

async function getFullSnapshotPayload() {
    const [parkings, roads] = await Promise.all([getParkingSnapshot(), getRoadSnapshot()]);
    return { type: "snapshot", parkings, roads };
}

async function sendFullSnapshot(ws) {
    if (!db) { sendJson(ws, { type: "error", message: "Baza še ni pripravljena." }); return; }
    sendJson(ws, await getFullSnapshotPayload());
}

async function broadcastFullSnapshot(reason) {
    if (!db) return;
    broadcastJson({ ...await getFullSnapshotPayload(), reason });
}

async function broadcastParkingEvent(type, data = {}) {
    broadcastJson({ type, resource: "parking", ...data });
    await broadcastFullSnapshot(type);
}

async function broadcastTrafficEvent(type, data = {}) {
    broadcastJson({ type, resource: "traffic", ...data });
    await broadcastFullSnapshot(type);
}

wss.on('connection', async ws => {
    console.log("WebSocket klient povezan.");
    ws.isAlive = true;
    ws.on('pong', () => { ws.isAlive = true; });
    ws.on('message', async raw => {
        try {
            const message = JSON.parse(raw.toString());
            if (message.type === "ping") { sendJson(ws, { type: "pong" }); return; }
            if (message.type === "snapshot:request") { await sendFullSnapshot(ws); return; }
            sendJson(ws, { type: "error", message: "Neznan WebSocket ukaz." });
        } catch {
            sendJson(ws, { type: "error", message: "Neveljaven WebSocket JSON." });
        }
    });
    ws.on('close', () => console.log("WebSocket klient odklopljen."));
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

app.get('/api/parking',          requireAuth,              async (req, res) => {
    try { res.json(await getParkingSnapshot()); }
    catch (err) { res.status(500).json({ error: err.message }); }
});

app.get('/api/parking/nearby',   requireAuth,              async (req, res) => {
    try {
        const latitude = toNumber(req.query.lat);
        const longitude = toNumber(req.query.lon);
        const radiusMeters = Math.min(toNumber(req.query.radius) ?? 1000, 20000);
        if (!hasValidCoordinates(latitude, longitude)) {
            return res.status(400).json({ error: "Manjkajo ali niso veljavne koordinate lat/lon." });
        }
        const data = await col().aggregate([
            { $geoNear: { near: { type: 'Point', coordinates: [longitude, latitude] }, distanceField: 'distanceMeters', maxDistance: Math.max(radiusMeters, 1), spherical: true, query: { locationGeo: { $exists: true } } } },
            { $project: parkingProjection }
        ]).toArray();
        res.json(data);
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.post('/api/parking',         requireAuth, requireAdmin, async (req, res) => {
    try {
        const doc = normalizeParkingDoc(req.body);
        await col().insertOne(doc);
        await broadcastParkingEvent("parking:created", { parking: { ...doc, _id: undefined } });
        res.status(201).json({ message: "Dodano." });
    } catch (err) {
        if (err.code === 11000) res.status(409).json({ error: "ID že obstaja." });
        else res.status(500).json({ error: err.message });
    }
});

app.post('/api/parking/sync',    requireAuth, requireAdmin, async (req, res) => {
    try {
        if (!Array.isArray(req.body)) return res.status(400).json({ error: "Pričakovan je seznam parkirišč." });
        await col().deleteMany({});
        const docs = req.body.map(normalizeParkingDoc);
        if (docs.length > 0) await col().insertMany(docs);
        await broadcastParkingEvent("parking:synced", { count: docs.length });
        res.json({ message: "Sinhronizirano " + docs.length + " zapisov." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.put('/api/parking/:id',      requireAuth, requireAdmin, async (req, res) => {
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
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.delete('/api/parking/:id',   requireAuth, requireAdmin, async (req, res) => {
    try {
        const id = parseInt(req.params.id);
        const result = await col().findOneAndDelete({ id }, { projection: { _id: 0 } });
        if (!result) return res.status(404).json({ error: "Ni najdeno." });
        await broadcastParkingEvent("parking:deleted", { id, parking: result });
        res.json({ message: "Izbrisano." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.delete('/api/parking',       requireAuth, requireAdmin, async (req, res) => {
    try {
        const result = await col().deleteMany({});
        await broadcastParkingEvent("parking:cleared", { deletedCount: result.deletedCount });
        res.json({ message: "Baza izpraznjena." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

// -------------------- STANJE CEST --------------------

app.get('/api/stanje-cest',           requireAuth,              async (req, res) => {
    try { res.json(await getRoadSnapshot()); }
    catch (err) { res.status(500).json({ error: err.message }); }
});

app.post('/api/stanje-cest/sync',     requireAuth, requireAdmin, async (req, res) => {
    try {
        if (!Array.isArray(req.body)) return res.status(400).json({ error: "Pričakovan je seznam stanj cest." });
        await roadCol().deleteMany({});
        const docs = req.body.map((item, index) => normalizeRoadDoc(item, index));
        if (docs.length > 0) await roadCol().insertMany(docs);
        await broadcastTrafficEvent("traffic:synced", { count: docs.length });
        res.json({ message: "Sinhronizirano " + docs.length + " zapisov stanja cest." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.delete('/api/stanje-cest',        requireAuth, requireAdmin, async (req, res) => {
    try {
        const result = await roadCol().deleteMany({});
        await broadcastTrafficEvent("traffic:cleared", { deletedCount: result.deletedCount });
        res.json({ message: "Stanje cest izbrisano." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.put('/api/stanje-cest/:id', requireAuth, requireAdmin, async (req, res) => {
    try {
        const id = String(req.params.id);
        const result = await roadCol().findOneAndUpdate(
            { id },
            { $set: normalizeRoadDoc({ ...req.body, id }) },
            { returnDocument: 'after', projection: { _id: 0 } }
        );
        if (!result) return res.status(404).json({ error: "Ni najdeno." });
        await broadcastTrafficEvent("traffic:updated", { id, road: result });
        res.json({ message: "Posodobljeno." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});
// -------------------- START --------------------

async function startServer() {
    try {
        await client.connect();
        db = client.db('traffic_twin');
        await col().createIndex({ locationGeo: '2dsphere' });
        await roadCol().createIndex({ id: 1 });
        await userCol().createIndex({ email: 1 }, { unique: true });
        console.log("Uspešno povezan z MongoDB.");

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
// -------------------- UPRAVLJANJE USERJEV (ADMIN) --------------------

app.get('/api/users', requireAuth, requireAdmin, async (req, res) => {
    try {
        const users = await userCol().find(
            {},
            { projection: { _id: 0, password: 0 } }
        ).toArray();
        res.json(users);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.patch('/api/users/:email/role', requireAuth, requireAdmin, async (req, res) => {
    try {
        const email = decodeURIComponent(req.params.email).toLowerCase();
        const { role } = req.body ?? {};

        if (!["user", "admin"].includes(role)) {
            return res.status(400).json({ error: "Vloga mora biti 'user' ali 'admin'." });
        }

        const result = await userCol().updateOne({ email }, { $set: { role } });

        if (result.matchedCount === 0) {
            return res.status(404).json({ error: "Uporabnik ne obstaja." });
        }

        res.json({ message: "Vloga posodobljena." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/users/:email', requireAuth, requireAdmin, async (req, res) => {
    try {
        const email = decodeURIComponent(req.params.email).toLowerCase();
        const currentEmail = getCurrentUserEmail(req);

        if (email === currentEmail) {
            return res.status(400).json({ error: "Ne moreš izbrisati svojega računa." });
        }

        const result = await userCol().deleteOne({ email });

        if (result.deletedCount === 0) {
            return res.status(404).json({ error: "Uporabnik ne obstaja." });
        }

        res.json({ message: "Uporabnik izbrisan." });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});