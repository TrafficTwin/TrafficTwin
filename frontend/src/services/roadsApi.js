import { apiRequest } from "./apiClient.js";

export function getRoadStates() {
    return apiRequest("/api/stanje-cest");
}

export function getNapRoadSources() {
    return apiRequest("/api/stanje-cest/nap/sources");
}

export function importNapRoadStates() {
    return apiRequest("/api/stanje-cest/nap/import", {
        method: "POST"
    });
}

export function syncRoadStates(roads) {
    return apiRequest("/api/stanje-cest/sync", {
        method: "POST",
        body: JSON.stringify(roads)
    });
}

export function clearRoadStates() {
    return apiRequest("/api/stanje-cest", {
        method: "DELETE"
    });
}

export function updateRoadState(id, road) {
    return apiRequest(`/api/stanje-cest/${encodeURIComponent(id)}`, {
        method: "PUT",
        body: JSON.stringify(road)
    });
}
