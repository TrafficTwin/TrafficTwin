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

export function removeFavouriteRoad(roadId) {
    return apiRequest(`/api/users/me/favourites/road/${encodeURIComponent(roadId)}`, {
        method: "DELETE"
    });
}

export function getAllUsers() {
    return apiRequest("/api/users");
}

export function updateUserRole(email, role) {
    return apiRequest(`/api/users/${encodeURIComponent(email)}/role`, {
        method: "PATCH",
        body: JSON.stringify({ role })
    });
}

export function deleteUser(email) {
    return apiRequest(`/api/users/${encodeURIComponent(email)}`, {
        method: "DELETE"
    });
}

export function addFavouriteRoad(roadId) {
    return apiRequest(`/api/users/me/favourites/road/${encodeURIComponent(roadId)}`, {
        method: "POST"
    });
}