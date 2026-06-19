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

// DOMNEVA (RESTful konvencija) - ni 100% potrjeno z backend kodo.
// Testiraj najprej na testnem/nepomembnem vnosu, ne na pravih podatkih!
export function deleteRoadState(id) {
    return apiRequest(`/api/stanje-cest/${encodeURIComponent(id)}`, {
        method: "DELETE"
    });
}

export function createRoadState(road) {
    // To je nov, varen endpoint za dodajanje posamezne ceste
    return apiRequest("/api/stanje-cest", {
        method: "POST",
        body: JSON.stringify(road)
    });
}