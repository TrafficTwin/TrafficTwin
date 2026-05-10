const express = require('express');
const { MongoClient } = require('mongodb');
const cors = require('cors');

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

app.get('/api/parking', async (req, res) => {
    try {
        const data = await col().find({}, { projection: { _id: 0 } }).toArray();
        res.json(data);
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.post('/api/parking', async (req, res) => {
    try {
        await col().insertOne(req.body);
        res.status(201).json({ message: "Dodano." });
    } catch (err) {
        if (err.code === 11000) res.status(409).json({ error: "ID ze obstaja." });
        else res.status(500).json({ error: err.message });
    }
});

app.post('/api/parking/sync', async (req, res) => {
    try {
        console.log("Sinhroniziram " + req.body.length + " zapisov...");
        await col().deleteMany({});
        const docs = req.body.map(item => ({ ...item, lastUpdated: new Date() }));
        await col().insertMany(docs);
        res.json({ message: "Sinhronizirano " + docs.length + " zapisov." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.put('/api/parking/:id', async (req, res) => {
    try {
        const id = parseInt(req.params.id);
        const result = await col().findOneAndUpdate(
            { id },
            { $set: { ...req.body, lastUpdated: new Date() } },
            { returnDocument: 'after' }
        );
        if (!result) return res.status(404).json({ error: "Ni najdeno." });
        res.json({ message: "Posodobljeno." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.delete('/api/parking/:id', async (req, res) => {
    try {
        const id = parseInt(req.params.id);
        const result = await col().findOneAndDelete({ id });
        if (!result) return res.status(404).json({ error: "Ni najdeno." });
        res.json({ message: "Izbrisano." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.delete('/api/parking', async (req, res) => {
    try {
        await col().deleteMany({});
        res.json({ message: "Baza izpraznjena." });
    } catch (err) { res.status(500).json({ error: err.message }); }
});

app.listen(3000, () => console.log("Streznik tece na http://localhost:3000"));