const crypto = require('crypto');

const NAP_BASE_URL = process.env.NAP_BASE_URL || 'https://b2b.nap.si';

const NAP_CONTENTS = [
    {
        key: 'roadworks-en',
        code: 'b2b.roadworks.geojson.en_US',
        sourceName: 'Delo na cesti (angleški GeoJSON)',
        tip: 'Delo na cesti',
        language: 'eng',
        format: 'GeoJSON'
    },
    {
        key: 'roadworks-sl',
        code: 'b2b.roadworks.geojson.sl_SI',
        sourceName: 'Delo na cesti (slovenski GeoJSON)',
        tip: 'Delo na cesti',
        language: 'slv',
        format: 'GeoJSON'
    },
    {
        key: 'events-en',
        code: 'b2b.events.geojson.en_US',
        sourceName: 'Prometni dogodki (angleški GeoJSON)',
        tip: 'Prometni dogodek',
        language: 'eng',
        format: 'GeoJSON'
    },
    {
        key: 'events-sl',
        code: 'b2b.events.geojson.sl_SI',
        sourceName: 'Prometni dogodki (slovenski GeoJSON)',
        tip: 'Prometni dogodek',
        language: 'slv',
        format: 'GeoJSON'
    },
    {
        key: 'traffic-report',
        code: 'b2b.traffic-report.json',
        sourceName: 'Prometno poročilo (JSON)',
        tip: 'Prometno poročilo',
        language: 'slv',
        format: 'JSON'
    },
    {
        key: 'border-delays',
        code: 'b2b.borderdelays.geojson',
        sourceName: 'Zastoji na meji (GeoJSON)',
        tip: 'Zastoj na meji',
        language: 'slv/eng',
        format: 'GeoJSON'
    }
];

let tokenCache = null;

function sha1(value) {
    return crypto.createHash('sha1').update(String(value)).digest('hex').slice(0, 14);
}

function trimText(value, maxLength = 500) {
    const text = String(value ?? '').replace(/\s+/g, ' ').trim();
    if (text.length <= maxLength) return text;
    return text.slice(0, maxLength - 1).trim() + '…';
}

function isPlainObject(value) {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function toNumber(value) {
    const n = typeof value === 'number' ? value : Number(String(value ?? '').replace(',', '.'));
    return Number.isFinite(n) ? n : null;
}

function hasValidCoordinates(latitude, longitude) {
    return latitude !== null && longitude !== null &&
        latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
}

function stringifyValue(value) {
    if (value == null) return '';
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
        return trimText(value);
    }
    if (Array.isArray(value)) {
        return trimText(value.map(stringifyValue).filter(Boolean).join(', '));
    }
    return trimText(JSON.stringify(value));
}

function findValueByKey(obj, candidates, depth = 0) {
    if (!isPlainObject(obj) || depth > 5) return '';

    const normalizedCandidates = candidates.map((c) => c.toLowerCase());

    for (const [key, value] of Object.entries(obj)) {
        if (normalizedCandidates.includes(key.toLowerCase())) {
            const text = stringifyValue(value);
            if (text) return text;
        }
    }

    for (const value of Object.values(obj)) {
        if (isPlainObject(value)) {
            const found = findValueByKey(value, candidates, depth + 1);
            if (found) return found;
        }
    }

    return '';
}

function extractProperties(item) {
    if (isPlainObject(item?.properties)) return item.properties;
    if (isPlainObject(item?.attributes)) return item.attributes;
    if (isPlainObject(item)) return item;
    return {};
}

function extractGeometry(item) {
    if (isPlainObject(item?.geometry)) return item.geometry;
    if (isPlainObject(item) && typeof item.type === 'string' && Array.isArray(item.coordinates)) return item;
    return null;
}

function coordinatePairsFromGeometry(geometry) {
    if (!geometry || !Array.isArray(geometry.coordinates)) return [];

    const pairs = [];

    function walk(value) {
        if (!Array.isArray(value)) return;

        if (value.length >= 2 && typeof value[0] === 'number' && typeof value[1] === 'number') {
            const lon = toNumber(value[0]);
            const lat = toNumber(value[1]);
            if (hasValidCoordinates(lat, lon)) pairs.push([lat, lon]);
            return;
        }

        value.forEach(walk);
    }

    walk(geometry.coordinates);
    return pairs;
}

function centerOf(points) {
    if (!Array.isArray(points) || points.length === 0) return { latitude: null, longitude: null };

    const latitude = points.reduce((sum, point) => sum + point[0], 0) / points.length;
    const longitude = points.reduce((sum, point) => sum + point[1], 0) / points.length;

    return { latitude, longitude };
}

function extractRecordList(payload) {
    if (payload == null) return [];

    if (payload.type === 'FeatureCollection' && Array.isArray(payload.features)) {
        return payload.features;
    }

    if (Array.isArray(payload)) {
        return payload.flatMap(extractRecordList);
    }

    if (!isPlainObject(payload)) return [];

    if (payload.type === 'Feature' || payload.geometry || payload.properties) {
        return [payload];
    }

    const preferredKeys = [
        'features', 'items', 'data', 'records', 'messages', 'reports',
        'situations', 'events', 'roadworks', 'borderDelays', 'delays',
        'contents', 'content', 'trafficReport', 'traffic_reports'
    ];

    for (const key of preferredKeys) {
        if (Array.isArray(payload[key])) return payload[key].flatMap(extractRecordList);
        if (isPlainObject(payload[key])) {
            const nested = extractRecordList(payload[key]);
            if (nested.length > 0) return nested;
        }
    }

    const nestedArrays = Object.values(payload)
        .filter(Array.isArray)
        .flatMap(extractRecordList);

    if (nestedArrays.length > 0) return nestedArrays;

    return [payload];
}

function normalizeNapRecord(source, record, index) {
    const properties = extractProperties(record);
    const geometry = extractGeometry(record);
    const coordinates = coordinatePairsFromGeometry(geometry);
    const { latitude, longitude } = centerOf(coordinates);

    const title = findValueByKey(properties, [
        'title', 'name', 'naziv', 'heading', 'headline', 'eventName', 'roadName', 'road', 'cesta', 'borderCrossing'
    ]);

    const location = findValueByKey(properties, [
        'location', 'lokacija', 'relacija', 'route', 'section', 'roadSection', 'direction', 'smer', 'area', 'obmocje'
    ]);

    const description = findValueByKey(properties, [
        'description', 'opis', 'message', 'text', 'comment', 'summary', 'details', 'content'
    ]);

    const status = findValueByKey(properties, [
        'status', 'stanje', 'state', 'eventType', 'workType', 'delay', 'severity', 'urgency', 'type', 'category'
    ]);

    const startTime = findValueByKey(properties, ['startTime', 'validFrom', 'from', 'start', 'zacetek', 'begin']);
    const endTime = findValueByKey(properties, ['endTime', 'validTo', 'to', 'end', 'konec']);

    const relacija = trimText(location || title || description || source.sourceName, 220);
    const stanje = trimText(status || description || source.sourceName, 500);

    const identitySource = JSON.stringify({
        source: source.key,
        featureId: record?.id ?? properties?.id ?? properties?.identifier ?? null,
        title,
        location,
        status,
        coordinates: coordinates.slice(0, 3)
    });

    return {
        id: `nap-${source.key}-${sha1(identitySource)}-${index}`,
        tip: source.tip,
        relacija,
        stanje,
        title: trimText(title || relacija, 220),
        description: trimText(description || stanje, 1000),
        sourceKey: source.key,
        sourceName: source.sourceName,
        napCode: source.code,
        language: source.language,
        format: source.format,
        category: 'NAP B2B',
        recordType: source.tip,
        startTime: startTime || null,
        endTime: endTime || null,
        latitude,
        longitude,
        coordinates: coordinates.slice(0, 500),
        geometryType: geometry?.type ?? null,
        rawProperties: properties,
        importedAt: new Date(),
        lastUpdated: new Date()
    };
}

async function getAccessToken() {
    if (tokenCache && tokenCache.accessToken && tokenCache.expiresAt > Date.now() + 60_000) {
        return tokenCache.accessToken;
    }

    const username = process.env.NAP_USERNAME;
    const password = process.env.NAP_PASSWORD;

    if (!username || !password) {
        throw new Error('Manjkata NAP_USERNAME in/ali NAP_PASSWORD v .env.');
    }

    const body = new URLSearchParams({
        grant_type: 'password',
        username,
        password
    });

    const response = await fetch(`${NAP_BASE_URL}/uc/user/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body
    });

    const text = await response.text();
    let json = null;
    try { json = JSON.parse(text); } catch { /* ignore */ }

    if (!response.ok) {
        throw new Error(`NAP prijava ni uspela (${response.status}): ${text}`);
    }

    if (!json?.access_token) {
        throw new Error('NAP ni vrnil access_token.');
    }

    tokenCache = {
        accessToken: json.access_token,
        refreshToken: json.refresh_token,
        expiresAt: Date.now() + ((Number(json.expires_in) || 3600) * 1000)
    };

    return tokenCache.accessToken;
}

async function fetchNapContent(source, accessToken) {
    const response = await fetch(`${NAP_BASE_URL}/data/${source.code}`, {
        method: 'GET',
        headers: { Authorization: `bearer ${accessToken}` }
    });

    const text = await response.text();

    if (!response.ok) {
        throw new Error(`NAP vsebina ${source.code} ni dosegljiva (${response.status}): ${text.slice(0, 300)}`);
    }

    if (!text.trim()) return null;

    try {
        return JSON.parse(text);
    } catch (err) {
        throw new Error(`NAP vsebina ${source.code} ni veljaven JSON: ${err.message}`);
    }
}

async function importNapRoadContents() {
    const accessToken = await getAccessToken();
    const items = [];
    const sources = [];
    const errors = [];

    for (const source of NAP_CONTENTS) {
        try {
            const payload = await fetchNapContent(source, accessToken);
            const records = extractRecordList(payload);
            const normalized = records.map((record, index) => normalizeNapRecord(source, record, index));
            items.push(...normalized);
            sources.push({ ...source, count: normalized.length, ok: true });
        } catch (err) {
            sources.push({ ...source, count: 0, ok: false, error: err.message });
            errors.push({ source: source.sourceName, code: source.code, error: err.message });
        }
    }

    return {
        importedAt: new Date().toISOString(),
        count: items.length,
        sources,
        errors,
        items
    };
}

module.exports = {
    NAP_CONTENTS,
    importNapRoadContents,
    normalizeNapRecord,
    extractRecordList
};
