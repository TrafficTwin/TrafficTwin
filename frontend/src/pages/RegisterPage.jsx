import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

export default function RegisterPage() {
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    async function handleRegister(e) {
        e.preventDefault();
        try {
            const res = await fetch('http://localhost:3000/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, email, password })
            });
            if (res.ok) {
                alert("Registracija uspešna!");
                navigate("/login");
            } else {
                const data = await res.json();
                setError(data.error);
            }
        } catch {
            setError("Napaka pri povezavi.");
        }
    }

    return (
        <div className="login-root">
            <div className="panel login-card">
                <p className="eyebrow">SmartCity</p>
                <h2 style={{ margin: "0 0 20px" }}>Registracija</h2>
                
                {error && <div className="error-box" style={{ marginBottom: "16px" }}>{error}</div>}

                <form onSubmit={handleRegister} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                    <label className="login-label">
                        Ime
                        <input className="login-input" value={name} onChange={e => setName(e.target.value)} required />
                    </label>
                    <label className="login-label">
                        E-pošta
                        <input className="login-input" type="email" value={email} onChange={e => setEmail(e.target.value)} required />
                    </label>
                    <label className="login-label">
                        Geslo
                        <input className="login-input" type="password" value={password} onChange={e => setPassword(e.target.value)} required />
                    </label>
                    <button type="submit" className="primary">Registriraj se</button>
                </form>

                <div style={{ marginTop: "20px", textAlign: "center" }}>
                    <button onClick={() => navigate("/login")} style={{ background: "none", border: "none", color: "#64748b", cursor: "pointer" }}>
                        Nazaj na prijavo
                    </button>
                </div>
            </div>
        </div>
    );
}