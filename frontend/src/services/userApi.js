import { apiRequest } from "./apiClient.js";

export function getCurrentUserProfile() {
    return apiRequest("/api/users/me");
}

export function addFavouriteParking(parkingId) {
    return apiRequest(`/api/users/me/favourites/parking/${parkingId}`, {
        method: "POST"
    });
}

export function removeFavouriteParking(parkingId) {
    return apiRequest(`/api/users/me/favourites/parking/${parkingId}`, {
        method: "DELETE"
    });
}

export function addFavouriteRoad(roadId) {
    return apiRequest(`/api/users/me/favourites/road/${encodeURIComponent(roadId)}`, {
        method: "POST"
    });
}

export function removeFavouriteRoad(roadId) {
    return apiRequest(`/api/users/me/favourites/road/${encodeURIComponent(roadId)}`, {
        method: "DELETE"
    });
}