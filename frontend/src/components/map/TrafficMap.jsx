import React, { useEffect, useMemo, useState } from "react";
import {
    CircleMarker,
    MapContainer,
    Polyline,
    Popup,
    TileLayer
} from "react-leaflet";
import { getParking } from "../../services/parkingApi.js";
import { getRoadStates } from "../../services/roadsApi.js";

const MARIBOR_CENTER = [46.5547, 15.6459];

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

function normalizeRoadPolyline(road) {
    if (Array.isArray(road.coordinates)) {
        return road.coordinates
            .map((point) => {
                if (Array.isArray(point)) {
                    return [Number(point[0]), Number(point[1])];
                }

                return [Number(point.latitude), Number(point.longitude)];
            })
            .filter(([lat, lon]) => isValidCoordinate(lat, lon));
    }

    if (Array.isArray(road.polyline)) {
        return road.polyline
            .map((point) => [Number(point.latitude), Number(point.longitude)])
            .filter(([lat, lon]) => isValidCoordinate(lat, lon));
    }

    return [];
}

export default function TrafficMap() {
    const [parkingLots, setParkingLots] = useState([]);
    const [roadStates, setRoadStates] = useState([]);
    const [showParking, setShowParking] = useState(true);
    const [showRoads, setShowRoads] = useState(true);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function loadMapData() {
        try {
            setLoading(true);
            setError("");

            const [parkingData, roadData] = await Promise.all([
                getParking(),
                getRoadStates()
            ]);

            setParkingLots(parkingData ?? []);
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

    const parkingWithCoordinates = useMemo(() => {
        return parkingLots.filter((parking) =>
            isValidCoordinate(parking.latitude, parking.longitude)
        );
    }, [parkingLots]);

    const trafficPoints = useMemo(() => {
        return roadStates.filter((road) =>
            isValidCoordinate(road.latitude, road.longitude)
        );
    }, [roadStates]);

    const roadsWithPolyline = useMemo(() => {
        return roadStates
            .map((road) => ({
                road,
                points: normalizeRoadPolyline(road)
            }))
            .filter((item) => item.points.length >= 2);
    }, [roadStates]);

    const roadsWithoutCoordinates = roadStates.filter((road) => {
        const hasPoint = isValidCoordinate(road.latitude, road.longitude);
        const hasLine = normalizeRoadPolyline(road).length >= 2;
        return !hasPoint && !hasLine;
    }).length;

    return (
        <div className="map-panel">
            <div className="map-toolbar">
                <div>
                    <strong>Zemljevid prometa</strong>
                    <p>Prikaz parkirišč in prometnih točk.</p>
                </div>

                <div className="map-actions">
                    <label>
                        <input
                            type="checkbox"
                            checked={showParking}
                            onChange={(event) => setShowParking(event.target.checked)}
                        />
                        Parkirišča
                    </label>

                    <label>
                        <input
                            type="checkbox"
                            checked={showRoads}
                            onChange={(event) => setShowRoads(event.target.checked)}
                        />
                        Stanje cest
                    </label>

                    <button onClick={loadMapData}>
                        Osveži
                    </button>
                </div>
            </div>

            {error && <div className="error-box">{error}</div>}
            {loading && <div className="info-box">Nalaganje zemljevida ...</div>}

            <MapContainer
                center={MARIBOR_CENTER}
                zoom={13}
                scrollWheelZoom={true}
                className="traffic-map"
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {showParking && parkingWithCoordinates.map((parking) => {
                    const capacity = Number(parking.capacity || 0);
                    const occupied = Number(parking.occupied || 0);
                    const freeSpaces = Math.max(capacity - occupied, 0);

                    return (
                        <CircleMarker
                            key={`parking-${parking.id}`}
                            center={[
                                Number(parking.latitude),
                                Number(parking.longitude)
                            ]}
                            radius={9}
                            pathOptions={{
                                color: getParkingColor(parking),
                                fillColor: getParkingColor(parking),
                                fillOpacity: 0.8
                            }}
                        >
                            <Popup>
                                <strong>{parking.location}</strong>
                                <br />
                                Kapaciteta: {parking.capacity}
                                <br />
                                Zasedeno: {parking.occupied}
                                <br />
                                Prosto: {freeSpaces}
                                <br />
                                Plačilo: {parking.typeOfPayment}
                            </Popup>
                        </CircleMarker>
                    );
                })}

                {showRoads && trafficPoints.map((road, index) => (
                    <CircleMarker
                        key={`traffic-${road.id ?? road.relacija ?? index}`}
                        center={[
                            Number(road.latitude),
                            Number(road.longitude)
                        ]}
                        radius={7}
                        pathOptions={{
                            color: getRoadColor(road),
                            fillColor: getRoadColor(road),
                            fillOpacity: 0.85
                        }}
                    >
                        <Popup>
                            <strong>{road.relacija}</strong>
                            <br />
                            Tip: {road.tip}
                            <br />
                            Stanje: {road.stanje}
                        </Popup>
                    </CircleMarker>
                ))}

                {showRoads && roadsWithPolyline.map(({ road, points }, index) => (
                    <Polyline
                        key={`road-line-${road.id ?? road.relacija ?? index}`}
                        positions={points}
                        pathOptions={{
                            color: getRoadColor(road),
                            weight: 6,
                            opacity: 0.85
                        }}
                    >
                        <Popup>
                            <strong>{road.relacija}</strong>
                            <br />
                            Tip: {road.tip}
                            <br />
                            Stanje: {road.stanje}
                        </Popup>
                    </Polyline>
                ))}
            </MapContainer>

            <div className="map-legend">
                <span><i className="legend-dot legend-green" /> Prosto / normalno</span>
                <span><i className="legend-dot legend-orange" /> Delno zasedeno / ovire</span>
                <span><i className="legend-dot legend-red" /> Polno / zaprto</span>

                {roadsWithoutCoordinates > 0 && (
                    <span className="muted">
                        {roadsWithoutCoordinates} cestnih zapisov še nima koordinat.
                    </span>
                )}
            </div>
        </div>
    );
}