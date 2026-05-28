import { apiRequest } from "./apiClient.js";

export function getParking() {
    return apiRequest("/api/parking");
}

export function getNearbyParking(latitude, longitude, radiusMeters) {
    const params = new URLSearchParams({
        lat: latitude,
        lon: longitude,
        radius: radiusMeters
    });

    return apiRequest(`/api/parking/nearby?${params.toString()}`);
}

export function createParking(parking) {
    return apiRequest("/api/parking", {
        method: "POST",
        body: JSON.stringify(parking)
    });
}

export function updateParking(id, parking) {
    return apiRequest(`/api/parking/${id}`, {
        method: "PUT",
        body: JSON.stringify(parking)
    });
}

export function deleteParking(id) {
    return apiRequest(`/api/parking/${id}`, {
        method: "DELETE"
    });
}