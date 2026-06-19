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

    if (response.status === 401) {
        localStorage.removeItem(TOKEN_KEY);
        window.location.href = "/login";
        throw new Error("Seja je potekla. Prosimo, prijavite se znova.");
    }

    if (response.status === 403) {
        throw new Error("Nimate dovoljenja za to dejanje.");
    }

    const contentType = response.headers.get("content-type") ?? "";
    const text = await response.text();
    let data = null;

    if (text && contentType.includes("application/json")) {
        try {
            data = JSON.parse(text);
        } catch {
            // Strežnik je vrnil neveljaven JSON (npr. HTML error stran) -
            // ne podremo se na JSON.parse, samo data ostane null.
            data = null;
        }
    }

    if (!response.ok) {
        const fallbackMessage =
            response.status === 404
                ? `Pot ${path} ne obstaja na strežniku (404).`
                : `API napaka: ${response.status}`;
        throw new Error(data?.error ?? data?.message ?? fallbackMessage);
    }

    return data;
}