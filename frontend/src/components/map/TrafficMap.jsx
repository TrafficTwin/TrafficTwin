import React, { useEffect, useMemo, useState, useRef } from "react";
import {
    CircleMarker,
    MapContainer,
    Polyline,
    Popup,
    TileLayer,
    Circle,
    useMapEvents
} from "react-leaflet";
import { getParking } from "../../services/parkingApi.js";
import { getRoadStates } from "../../services/roadsApi.js";

const MARIBOR_CENTER = [46.5547, 15.6459];

// Haversine formula za izračun razdalje med dvema točkama v metrih
function getDistanceInMeters(lat1, lon1, lat2, lon2) {
    const R = 6371e3; // Radij zemlje v metrih
    const phi1 = (lat1 * Math.PI) / 180;
    const phi2 = (lat2 * Math.PI) / 180;
    const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
    const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

    const a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
              Math.cos(phi1) * Math.cos(phi2) *
              Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c; // Razdalja v metrih
}

function isValidCoordinate(latitude, longitude) {
    return (
        Number.isFinite(Number(latitude)) &&
        Number.isFinite(Number(longitude)) &&
        Number(latitude) >= -90 &&
        Number(latitude) <= 90 &&
        Number(longitude) >= -180 &&
        Number(longitude) <= 180
    );
}

function getParkingColor(parking) {
    const capacity = Number(parking.capacity) || 0;
    const occupied = Number(parking.occupied) || 0;
    if (capacity <= 0) return "#64748b";
    const occupancyRate = occupied / capacity;
    if (occupancyRate >= 0.9) return "#dc2626";
    if (occupancyRate >= 0.6) return "#f59e0b";
    return "#16a34a";
}

function getRoadColor(road) {
    const stanje = String(road.stanje ?? "").toLowerCase();
    if (stanje.includes("zapr")) return "#dc2626";
    if (stanje.includes("ovir") || stanje.includes("delo")) return "#f59e0b";
    if (stanje.includes("normal") || stanje.includes("prevoz")) return "#16a34a";
    return "#2563eb";
}

// Komponenta za lovljenje klikov na zemljevidu
function MapClickHandler({ onMapClick }) {
    useMapEvents({
        click(e) {
            onMapClick([e.latlng.lat, e.latlng.lng]);
        },
    });
    return null;
}

export default function TrafficMap() {
    const [allParkingLots, setAllParkingLots] = useState([]);
    const [roadStates, setRoadStates] = useState([]);
    const [showParking, setShowParking] = useState(true);
    const [showRoads, setShowRoads] = useState(true);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // Stanja za interaktivni polmer
    const [radiusMode, setRadiusMode] = useState(false);
    const [searchCenter, setSearchCenter] = useState(null); // Center postane aktivna točka ob kliku
    const [radiusMeters, setRadiusMeters] = useState(1000); // Privzeto 1km

    const markerRefs = useRef({});
    const mapRef = useRef(null);

    // Prvotni zajem vseh podatkov iz baze
    async function loadMapData() {
        try {
            setLoading(true);
            setError("");
            const [parkingData, roadData] = await Promise.all([
                getParking(),
                getRoadStates()
            ]);
            setAllParkingLots(parkingData ?? []);
            setRoadStates(roadData ?? []);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadMapData();
    }, []);

    // 1. Filtracija koordinat za ceste
    const trafficPoints = useMemo(() => {
        return roadStates.filter((road) => isValidCoordinate(road.latitude, road.longitude));
    }, [roadStates]);

    // 2. FILTRACIJA PARKIRIŠČ V REALNEM ČASU (Glede na slider in izbrani center)
    const filteredParking = useMemo(() => {
        // Najprej filtriramo samo tista, ki imajo veljavne koordinate
        const validParking = allParkingLots.filter((p) => isValidCoordinate(p.latitude, p.longitude));

        // Če način za polmer NI vklopljen ali pa center še ni izbran, prikaži vsa
        if (!radiusMode || !searchCenter) {
            return validParking;
        }

        // V živo filtriramo parkirišča, ki so znotraj izbranega polmera (v metrih)
        return validParking.filter((parking) => {
            const distance = getDistanceInMeters(
                searchCenter[0],
                searchCenter[1],
                Number(parking.latitude),
                Number(parking.longitude)
            );
            return distance <= radiusMeters;
        });
    }, [allParkingLots, radiusMode, searchCenter, radiusMeters]);

    const handleFocusParking = (parking) => {
        const latLng = [Number(parking.latitude), Number(parking.longitude)];
        if (mapRef.current) {
            mapRef.current.setView(latLng, 16);
        }
        const marker = markerRefs.current[parking.id];
        if (marker) {
            marker.openPopup();
        }
    };

    return (
        <div className="map-panel">
            <div className="map-toolbar">
                <div>
                    <strong>Zemljevid prometa</strong>
                    <p>Prikaz parkirišč in prometnih točk.</p>
                </div>

                <div className="map-actions">
                    <button 
                        className={`toggle-btn ${radiusMode ? "primary" : ""}`}
                        onClick={() => {
                            setRadiusMode(!radiusMode);
                            if (!searchCenter) setSearchCenter(MARIBOR_CENTER); // Nastavi privzeti center ob prvem vklopu
                        }}
                    >
                        {radiusMode ? "⚡ Iskanje v polmeru AKTIVNO" : "🔍 Vklopi iskanje v polmeru"}
                    </button>

                    {radiusMode && (
                        <div className="radius-controls">
                            <label>
                                Polmer: <strong>{radiusMeters}m</strong>
                                <input 
                                    type="range" 
                                    min="100" 
                                    max="1900" 
                                    step="50"
                                    value={radiusMeters} 
                                    onChange={(e) => setRadiusMeters(Number(e.target.value))} // Sprememba v živo!
                                />
                            </label>
                        </div>
                    )}

                    <label>
                        <input type="checkbox" checked={showParking} onChange={(e) => setShowParking(e.target.checked)} />
                        Parkirišča
                    </label>

                    <label>
                        <input type="checkbox" checked={showRoads} onChange={(e) => setShowRoads(e.target.checked)} />
                        Stanje cest
                    </label>

                    <button onClick={loadMapData}>Osveži podatke</button>
                </div>
            </div>

            {radiusMode && (
                <div className="info-box text-center">
                    📍 <strong>Navodilo:</strong> Klikni kamorkoli na zemljevid, da prestaviš center iskanja, nato premikaj slider za spreminjanje polmera v živo.
                </div>
            )}

            {error && <div className="error-box">{error}</div>}
            {loading && <div className="info-box">Nalaganje ...</div>}

            <MapContainer
                center={MARIBOR_CENTER}
                zoom={13}
                scrollWheelZoom={true}
                className="traffic-map"
                ref={mapRef}
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {/* Lovilec klikov, ki posodablja točko iskanja */}
                {radiusMode && (
                    <MapClickHandler onMapClick={(latlng) => setSearchCenter(latlng)} />
                )}

                {/* Dinamični krog, ki se v živo krči/širi glede na slider */}
                {radiusMode && searchCenter && (
                    <Circle 
                        center={searchCenter}
                        radius={radiusMeters}
                        pathOptions={{ 
                            color: '#2563eb', 
                            fillColor: '#2563eb', 
                            fillOpacity: 0.12, 
                            dashArray: '6, 6',
                            weight: 2
                        }}
                    />
                )}

                {/* Izris SAMO filtriranih parkirišč v živo */}
                {showParking && filteredParking.map((parking) => {
                    const capacity = Number(parking.capacity || 0);
                    const occupied = Number(parking.occupied || 0);
                    const freeSpaces = Math.max(capacity - occupied, 0);

                    return (
                        <CircleMarker
                            key={`parking-${parking.id}`}
                            ref={(el) => (markerRefs.current[parking.id] = el)}
                            center={[Number(parking.latitude), Number(parking.longitude)]}
                            radius={9}
                            pathOptions={{
                                color: getParkingColor(parking),
                                fillColor: getParkingColor(parking),
                                fillOpacity: 0.85
                            }}
                        >
                            <Popup>
                                <strong>{parking.location}</strong>
                                <br />
                                Kapaciteta: {parking.capacity} | Zasedeno: {parking.occupied}
                                <br />
                                <strong>Prosto: {freeSpaces}</strong>
                            </Popup>
                        </CircleMarker>
                    );
                })}

                {/* Izris stanja cest */}
                {showRoads && trafficPoints.map((road, index) => (
                    <CircleMarker
                        key={`traffic-${road.id ?? road.relacija ?? index}`}
                        center={[Number(road.latitude), Number(road.longitude)]}
                        radius={6}
                        pathOptions={{ color: getRoadColor(road), fillColor: getRoadColor(road), fillOpacity: 0.8 }}
                    >
                        <Popup>
                            <strong>{road.relacija}</strong><br />Stanje: {road.stanje}
                        </Popup>
                    </CircleMarker>
                ))}
            </MapContainer>

            {/* SPODNJI DINAMIČNI SEZNAM */}
            {showParking && (
                <div className="parking-list-section">
                    <h3>
                        {radiusMode && searchCenter 
                            ? `Najdena parkirišča v polmeru ${radiusMeters}m (${filteredParking.length})` 
                            : `Vsa parkirišča (${filteredParking.length})`}
                    </h3>
                    <div className="parking-horizontal-grid">
                        {filteredParking.length === 0 ? (
                            <p className="muted">V tem območju ni nobenega parkirišča. Poskusi povečati polmer na sliderju.</p>
                        ) : (
                            filteredParking.map((parking) => {
                                const capacity = Number(parking.capacity || 0);
                                const occupied = Number(parking.occupied || 0);
                                const freeSpaces = Math.max(capacity - occupied, 0);
                                const pct = capacity > 0 ? Math.min((occupied / capacity) * 100, 100) : 0;

                                return (
                                    <div 
                                        key={parking.id} 
                                        className="parking-mini-card"
                                        onClick={() => handleFocusParking(parking)}
                                    >
                                        <div className="card-header-info">
                                            <h4>{parking.location}</h4>
                                            <span className="payment-badge">{parking.typeOfPayment}</span>
                                        </div>
                                        <p>Prosto: <strong>{freeSpaces}</strong> / {capacity}</p>
                                        <div className="progress">
                                            <div style={{ 
                                                width: `${pct}%`, 
                                                backgroundColor: pct > 90 ? '#dc2626' : pct > 60 ? '#f59e0b' : '#16a34a' 
                                            }} />
                                        </div>
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}