import React, { useEffect, useMemo, useState, useRef } from "react";
import {
    CircleMarker,
    MapContainer,
    Popup,
    TileLayer,
    Circle,
    useMapEvents
} from "react-leaflet";
import { getParking } from "../../services/parkingApi.js";
import { getRoadStates } from "../../services/roadsApi.js";

const MARIBOR_CENTER = [46.5547, 15.6459];

function getDistanceInMeters(lat1, lon1, lat2, lon2) {
    const R = 6371e3;
    const phi1 = (lat1 * Math.PI) / 180;
    const phi2 = (lat2 * Math.PI) / 180;
    const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
    const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;
    const a = Math.sin(deltaPhi / 2) ** 2 + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function isValidCoordinate(lat, lng) {
    return Number.isFinite(Number(lat)) && Number.isFinite(Number(lng)) &&
        Number(lat) >= -90 && Number(lat) <= 90 && Number(lng) >= -180 && Number(lng) <= 180;
}

function getParkingColor(parking) {
    const capacity = Number(parking.capacity) || 0;
    const occupied = Number(parking.occupied) || 0;
    if (capacity <= 0) return "#64748b";
    const r = occupied / capacity;
    return r >= 0.9 ? "#dc2626" : r >= 0.6 ? "#f59e0b" : "#16a34a";
}

function getRoadColor(road) {
    const s = String(road.stanje ?? "").toLowerCase();
    if (s.includes("zapr")) return "#dc2626";
    if (s.includes("ovir") || s.includes("delo")) return "#f59e0b";
    if (s.includes("normal") || s.includes("prevoz")) return "#16a34a";
    return "#2563eb";
}

function MapClickHandler({ onMapClick }) {
    useMapEvents({ click(e) { onMapClick([e.latlng.lat, e.latlng.lng]); } });
    return null;
}

export default function TrafficMap() {
    const [allParkingLots, setAllParkingLots] = useState([]);
    const [roadStates, setRoadStates] = useState([]);
    const [showParking, setShowParking] = useState(true);
    const [showRoads, setShowRoads] = useState(true);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [radiusMode, setRadiusMode] = useState(false);
    const [searchCenter, setSearchCenter] = useState(null);
    const [radiusMeters, setRadiusMeters] = useState(1000);
    const [hoveredId, setHoveredId] = useState(null);

    const markerRefs = useRef({});
    const mapRef = useRef(null);

    async function loadMapData() {
        try {
            setLoading(true);
            setError("");
            const [parkingData, roadData] = await Promise.all([getParking(), getRoadStates()]);
            setAllParkingLots(parkingData ?? []);
            setRoadStates(roadData ?? []);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => { loadMapData(); }, []);

    const trafficPoints = useMemo(() =>
        roadStates.filter((r) => isValidCoordinate(r.latitude, r.longitude)), [roadStates]);

    const filteredParking = useMemo(() => {
        const valid = allParkingLots.filter((p) => isValidCoordinate(p.latitude, p.longitude));
        if (!radiusMode || !searchCenter) return valid;
        return valid.filter((p) =>
            getDistanceInMeters(searchCenter[0], searchCenter[1], Number(p.latitude), Number(p.longitude)) <= radiusMeters
        );
    }, [allParkingLots, radiusMode, searchCenter, radiusMeters]);

    const handleFocusParking = (parking) => {
        const latLng = [Number(parking.latitude), Number(parking.longitude)];
        if (mapRef.current) mapRef.current.setView(latLng, 16);
        markerRefs.current[parking.id]?.openPopup();
    };

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "12px", height: "calc(100vh - 52px - 64px)", minHeight: 0 }}>

            {/* Toolbar */}
            <div style={{
                display: "flex", justifyContent: "space-between", alignItems: "center",
                flexWrap: "wrap", gap: "12px", flexShrink: 0,
            }}>
                <div>
                    <strong style={{ fontSize: "16px" }}>Zemljevid prometa</strong>
                    <p style={{ margin: "2px 0 0", color: "#64748b", fontSize: "13px" }}>Prikaz parkirišč in prometnih točk.</p>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
                    <button
                        className={`toggle-btn ${radiusMode ? "primary" : ""}`}
                        onClick={() => {
                            setRadiusMode(!radiusMode);
                            if (!searchCenter) setSearchCenter(MARIBOR_CENTER);
                        }}
                    >
                        {radiusMode ? "⚡ Iskanje v polmeru AKTIVNO" : "🔍 Vklopi iskanje v polmeru"}
                    </button>
                    {radiusMode && (
                        <div className="radius-controls">
                            <label>
                                Polmer: <strong>{radiusMeters}m</strong>
                                <input type="range" min="100" max="1900" step="50"
                                    value={radiusMeters}
                                    onChange={(e) => setRadiusMeters(Number(e.target.value))} />
                            </label>
                        </div>
                    )}
                    <label style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "14px" }}>
                        <input type="checkbox" checked={showParking} onChange={(e) => setShowParking(e.target.checked)} />
                        Parkirišča
                    </label>
                    <label style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "14px" }}>
                        <input type="checkbox" checked={showRoads} onChange={(e) => setShowRoads(e.target.checked)} />
                        Stanje cest
                    </label>
                    <button onClick={loadMapData}>Osveži podatke</button>
                </div>
            </div>

            {radiusMode && (
                <div className="info-box" style={{ flexShrink: 0 }}>
                    📍 <strong>Navodilo:</strong> Klikni na zemljevid, da prestaviš center iskanja.
                </div>
            )}
            {error && <div className="error-box" style={{ flexShrink: 0 }}>{error}</div>}
            {loading && <div className="info-box" style={{ flexShrink: 0 }}>Nalaganje ...</div>}

            {/* Zemljevid + seznam */}
            <div style={{ display: "flex", gap: "16px", flex: 1, minHeight: 0 }}>

                {/* ZEMLJEVID */}
                <div style={{ flex: "1 1 0", minWidth: 0, minHeight: 0 }}>
                    <MapContainer
                        center={MARIBOR_CENTER}
                        zoom={13}
                        scrollWheelZoom={true}
                        style={{ width: "100%", height: "100%", borderRadius: "16px" }}
                        ref={mapRef}
                    >
                        <TileLayer
                            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />
                        {radiusMode && <MapClickHandler onMapClick={setSearchCenter} />}
                        {radiusMode && searchCenter && (
                            <Circle center={searchCenter} radius={radiusMeters}
                                pathOptions={{ color: '#2563eb', fillColor: '#2563eb', fillOpacity: 0.12, dashArray: '6,6', weight: 2 }} />
                        )}
                        {showParking && filteredParking.map((parking) => (
                            <CircleMarker
                                key={`parking-${parking.id}`}
                                ref={(el) => (markerRefs.current[parking.id] = el)}
                                center={[Number(parking.latitude), Number(parking.longitude)]}
                                radius={hoveredId === parking.id ? 12 : 9}
                                pathOptions={{
                                    color: getParkingColor(parking),
                                    fillColor: getParkingColor(parking),
                                    fillOpacity: 0.85,
                                    weight: hoveredId === parking.id ? 3 : 1,
                                }}
                            >
                                <Popup>
                                    <strong>{parking.location}</strong><br />
                                    Kapaciteta: {parking.capacity} | Zasedeno: {parking.occupied}<br />
                                    <strong>Prosto: {Math.max((Number(parking.capacity) || 0) - (Number(parking.occupied) || 0), 0)}</strong>
                                </Popup>
                            </CircleMarker>
                        ))}
                        {showRoads && trafficPoints.map((road, i) => (
                            <CircleMarker
                                key={`traffic-${road.id ?? i}`}
                                center={[Number(road.latitude), Number(road.longitude)]}
                                radius={6}
                                pathOptions={{ color: getRoadColor(road), fillColor: getRoadColor(road), fillOpacity: 0.8 }}
                            >
                                <Popup><strong>{road.relacija}</strong><br />Stanje: {road.stanje}</Popup>
                            </CircleMarker>
                        ))}
                    </MapContainer>
                </div>

                {/* SEZNAM PARKIRIŠČ */}
                {showParking && (
                    <div style={{
                        width: "280px",
                        flexShrink: 0,
                        display: "flex",
                        flexDirection: "column",
                        minHeight: 0,
                    }}>
                        <div style={{ marginBottom: "8px", flexShrink: 0 }}>
                            <strong style={{ fontSize: "14px" }}>
                                {radiusMode && searchCenter
                                    ? `V polmeru ${radiusMeters}m (${filteredParking.length})`
                                    : `Parkirišča (${filteredParking.length})`}
                            </strong>
                        </div>

                        <div style={{
                            flex: 1,
                            overflowY: "auto",
                            display: "flex",
                            flexDirection: "column",
                            gap: "8px",
                            paddingRight: "4px",
                        }}>
                            {filteredParking.length === 0 ? (
                                <p style={{ color: "#64748b" }}>Ni parkirišč. Poskusi povečati polmer.</p>
                            ) : (
                                filteredParking.map((parking) => {
                                    const capacity = Number(parking.capacity || 0);
                                    const occupied = Number(parking.occupied || 0);
                                    const free = Math.max(capacity - occupied, 0);
                                    const pct = capacity > 0 ? Math.min((occupied / capacity) * 100, 100) : 0;
                                    const barColor = pct > 90 ? '#dc2626' : pct > 60 ? '#f59e0b' : '#16a34a';
                                    const isHovered = hoveredId === parking.id;

                                    return (
                                        <div
                                            key={parking.id}
                                            onClick={() => handleFocusParking(parking)}
                                            onMouseEnter={() => setHoveredId(parking.id)}
                                            onMouseLeave={() => setHoveredId(null)}
                                            style={{
                                                background: isHovered ? "#f0f7ff" : "#fff",
                                                border: `1px solid ${isHovered ? "#2563eb" : "#e2e8f0"}`,
                                                borderRadius: "12px",
                                                padding: "12px 14px",
                                                cursor: "pointer",
                                                transition: "border-color 0.15s, background 0.15s",
                                                flexShrink: 0,
                                            }}
                                        >
                                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "4px" }}>
                                                <span style={{ fontSize: "13px", fontWeight: 600, color: "#111827", lineHeight: 1.3 }}>
                                                    {parking.location}
                                                </span>
                                                <span style={{
                                                    fontSize: "10px", background: "#f1f5f9", color: "#475569",
                                                    padding: "2px 6px", borderRadius: "6px", whiteSpace: "nowrap", marginLeft: "6px", flexShrink: 0,
                                                }}>
                                                    {parking.typeOfPayment}
                                                </span>
                                            </div>
                                            <p style={{ margin: "0 0 6px", fontSize: "12px", color: "#475569" }}>
                                                Prosto: <strong style={{ color: "#111827" }}>{free}</strong> / {capacity}
                                            </p>
                                            <div style={{ height: "5px", background: "#e2e8f0", borderRadius: "999px", overflow: "hidden" }}>
                                                <div style={{ width: `${pct}%`, height: "100%", backgroundColor: barColor, borderRadius: "999px" }} />
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}