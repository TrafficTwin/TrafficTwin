import { apiRequest } from "./apiClient.js";

export function getRoadStates() {
    return apiRequest("/api/road-status");
}

export function getNapRoadSources() {
    return apiRequest("/api/road-status/nap/sources");
}

export function importNapRoadStates() {
    return apiRequest("/api/road-status/nap/import", {
        method: "POST"
    });
}

export function syncRoadStates(roads) {
    return apiRequest("/api/road-status/sync", {
        method: "POST",
        body: JSON.stringify(roads)
    });
}

export function clearRoadStates() {
    return apiRequest("/api/road-status", {
        method: "DELETE"
    });
}

export function updateRoadState(id, road) {
    return apiRequest(`/api/road-status/${encodeURIComponent(id)}`, {
        method: "PUT",
        body: JSON.stringify(road)
    });
}

export function deleteRoadState(id) {
    return apiRequest(`/api/road-status/${encodeURIComponent(id)}`, {
        method: "DELETE"
    });
}

export function createRoadState(road) {
    // To je nov, varen endpoint za dodajanje posamezne ceste
    return apiRequest("/api/road-status", {
        method: "POST",
        body: JSON.stringify(road)
    });
}