import React, { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom"; 

export default function LoginPage() {
    const navigate = useNavigate(); 
    const { login } = useAuth();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        if (!email || !password) { setError("Izpolni obe polji."); return; }
        try {
            setError("");
            setLoading(true);
            await login(email, password);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="login-root">
            <div className="panel login-card">
                <p className="eyebrow" style={{ marginBottom: "4px" }}>TrafficTwin</p>
                <h2 style={{ margin: "0 0 4px" }}>Prijava</h2>
                <p style={{ margin: "0 0 20px", color: "#64748b", fontSize: "14px" }}>
                    Dostop do uporabniškega vmesnika
                </p>

                {error && <div className="error-box" style={{ marginBottom: "16px" }}>{error}</div>}

                <form onSubmit={handleSubmit} noValidate style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                    <label className="login-label">
                        E-pošta
                        <input
                            type="email"
                            autoComplete="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="ime@podjetje.si"
                            disabled={loading}
                            className="login-input"
                        />
                    </label>

                    <label className="login-label">
                        Geslo
                        <div className="login-pw-wrap">
                            <input
                                type={showPassword ? "text" : "password"}
                                autoComplete="current-password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                disabled={loading}
                                className="login-input"
                                style={{ paddingRight: "2.5rem" }}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword((v) => !v)}
                                className="login-eye"
                                aria-label={showPassword ? "Skrij geslo" : "Pokaži geslo"}
                            >
                                {showPassword ? "✗" : "👁"}
                            </button>
                        </div>
                    </label>

                    <button type="submit" className="primary" disabled={loading} style={{ marginTop: "4px" }}>
                        {loading ? "Prijavljanje …" : "Prijava"}
                    </button>
                </form>
                <div style={{ marginTop: "20px", textAlign: "center", fontSize: "14px" }}>
                 <span style={{ color: "#64748b" }}></span>
                 <button 
                     onClick={() => navigate("/register")} 
                 style={{ 
                     background: "none", 
                        border: "none", 
                color: "#2563eb", 
                    cursor: "pointer", 
                    fontWeight: "bold",
                    padding: 0 
        }}
    >
        Registriracija
    </button>
</div>


            </div>
        </div>
    );
}