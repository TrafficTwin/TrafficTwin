// authMiddleware.js  —  Express backend (Node.js)
// Namestitev: npm install jsonwebtoken

import jwt from "jsonwebtoken";

const JWT_SECRET = process.env.JWT_SECRET;

if (!JWT_SECRET) {
    throw new Error("JWT_SECRET ni nastavljen v .env!");
}

// ── Preveri token ─────────────────────────────────────────────────────────────
export function requireAuth(req, res, next) {
    const authHeader = req.headers.authorization;

    if (!authHeader?.startsWith("Bearer ")) {
        return res.status(401).json({ error: "Manjka avtentikacijski token." });
    }

    const token = authHeader.slice(7);

    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        req.user = decoded; // { sub, role, name, exp, ... }
        next();
    } catch (err) {
        if (err.name === "TokenExpiredError") {
            return res.status(401).json({ error: "Token je potekel." });
        }
        return res.status(401).json({ error: "Neveljaven token." });
    }
}

// ── Samo admin ────────────────────────────────────────────────────────────────
export function requireAdmin(req, res, next) {
    if (req.user?.role !== "admin") {
        return res.status(403).json({ error: "Dostop dovoljen samo administratorjem." });
    }
    next();
}

// ── Login endpoint (dodaj v server.js / routes) ───────────────────────────────
// POST /api/auth/login  →  { email, password }  →  { token, user }
export function createLoginHandler(findUser) {
    return async (req, res) => {
        const { email, password } = req.body ?? {};

        if (!email || !password) {
            return res.status(400).json({ error: "E-pošta in geslo sta obvezna." });
        }

        try {
            // findUser je tvoja funkcija iz baze — spodaj je primer
            const user = await findUser(email, password);

            if (!user) {
                return res.status(401).json({ error: "Napačen e-poštni naslov ali geslo." });
            }

            const token = jwt.sign(
                { sub: user.email, role: user.role, name: user.name },
                JWT_SECRET,
                { expiresIn: "8h" }
            );

            res.json({ token, user: { email: user.email, role: user.role, name: user.name } });
        } catch (err) {
            console.error("Login napaka:", err);
            res.status(500).json({ error: "Napaka strežnika." });
        }
    };
}