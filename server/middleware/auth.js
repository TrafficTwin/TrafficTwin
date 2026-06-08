const jwt = require('jsonwebtoken');

const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1]; // Format: "Bearer TOKEN"

    if (!token) return res.status(401).json({ msg: "Manjkajoč token" });

    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
        if (err) return res.status(403).json({ msg: "Neveljaven token" });
        req.user = user; // Tukaj so sedaj podatki o uporabniku (vloga, id...)
        next();
    });
};

const authorizeAdmin = (req, res, next) => {
    if (req.user && req.user.role === 'admin') {
        next();
    } else {
        res.status(403).json({ msg: "Dostop zavrnjen. Potrebne so admin pravice." });
    }
};

module.exports = { authenticateToken, authorizeAdmin };