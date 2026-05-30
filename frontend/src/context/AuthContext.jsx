import React, { createContext, useCallback, useContext, useEffect, useState } from "react";


export const AuthContext = createContext(null);
const TOKEN_KEY = "smart_city_token";

// Pomožna funkcija za dekodiranje JWT
function parseToken(token) {
    try {
        const payload = token.split(".")[1];
        const decoded = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
        if (decoded.exp < Math.floor(Date.now() / 1000)) return null;
        return decoded;
    } catch { return null; }
}

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem(TOKEN_KEY);
        if (token) {
            const decoded = parseToken(token);
            if (decoded) {
                setUser({ email: decoded.sub, role: decoded.role, name: decoded.name });
            } else {
                localStorage.removeItem(TOKEN_KEY);
            }
        }
        setLoading(false);
    }, []);

    const login = useCallback(async (email, password) => {
        const res = await fetch(`${import.meta.env.VITE_API_BASE_URL ?? "http://127.0.0.1:3000"}/api/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        });
        if (!res.ok) throw new Error((await res.json()).error);
        
        const { token, user: loggedIn } = await res.json();
        localStorage.setItem(TOKEN_KEY, token);
        setUser(loggedIn);
    }, []);

    const logout = useCallback(() => {
        localStorage.removeItem(TOKEN_KEY);
        setUser(null);
    }, []);

    const isAdmin = user?.role === "admin";

    return (
        <AuthContext.Provider value={{ user, isAdmin, loading, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth mora biti znotraj <AuthProvider>");
    return ctx;
}