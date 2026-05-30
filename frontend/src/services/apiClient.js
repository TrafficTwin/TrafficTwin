const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://127.0.0.1:3000";

const TOKEN_KEY = "smart_city_token";

function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

export async function apiRequest(path, options = {}) {
    const token = getToken();

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...(options.headers ?? {}),
        },
    });

    // Token je potekel ali neveljaven → prisili logout
    if (response.status === 401) {
        localStorage.removeItem(TOKEN_KEY);
        window.location.href = "/login";
        throw new Error("Seja je potekla. Prosimo, prijavite se znova.");
    }

    // 403 — prijavljen, ampak nima dostopa (napačna vloga)
    if (response.status === 403) {
        throw new Error("Nimate dovoljenja za to dejanje.");
    }

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
        throw new Error(data?.error ?? data?.message ?? `API napaka: ${response.status}`);
    }

    return data;
}