import { NavLink, Outlet, useNavigate } from "react-router-dom";
import React, { useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";

export default function AppLayout() {
    const { user, isAdmin, logout } = useAuth();
    const navigate = useNavigate();
    const [menuOpen, setMenuOpen] = useState(false);

    const navLinks = [
        { to: "/", label: "Pregled" },
        { to: "/parking", label: "Parkirišča" },
        { to: "/stanje-cest", label: "Stanje cest" },
        { to: "/zemljevid", label: "Zemljevid" },
    ];

    const linkStyle = (isActive) => ({
        color: isActive ? "#fff" : "#94a3b8",
        background: isActive ? "#2563eb" : "transparent",
        textDecoration: "none",
        fontSize: "14px",
        fontWeight: isActive ? 600 : 500,
        padding: "6px 14px",
        borderRadius: "8px",
        transition: "background 0.15s, color 0.15s",
        whiteSpace: "nowrap",
    });

    const iconBtnStyle = {
        width: 32, height: 32, borderRadius: "50%",
        background: "rgba(255,255,255,0.08)", border: "none",
        color: "#94a3b8", display: "flex", alignItems: "center",
        justifyContent: "center", padding: 0, cursor: "pointer",
        transition: "background 0.15s, color 0.15s",
        flexShrink: 0,
    };

    return (
        <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
            <style>{`
    .nav-desktop { display: flex; }
    .nav-hamburger { display: none !important; }
    .nav-mobile-menu { display: none; }
    
    @media (max-width: 700px) {
        .nav-desktop { display: none !important; }
        .nav-hamburger { 
            display: flex !important; 
            /* RESETIRANJE GLOBALNEGA GUMBA */
            background: transparent !important; 
            border: none !important;
            padding: 4px !important;
            margin: 0 !important;
            cursor: pointer;
        }
        .nav-mobile-menu { display: flex !important; flex-direction: column; }
    }
`}</style>

            <header style={{
                background: "#111827", height: "52px", display: "flex",
                alignItems: "center", padding: "0 24px", gap: "24px",
                position: "sticky", top: 0, zIndex: 100,
            }}>
                <span style={{ fontSize: "13px", fontWeight: 700, color: "#fff", textTransform: "uppercase" }}>
                    Traffic Twin
                </span>

                <nav className="nav-desktop" style={{ display: "flex", gap: "2px" }}>
                    {navLinks.map(({ to, label }) => (
                        <NavLink key={to} to={to} end={to === "/"} style={({ isActive }) => linkStyle(isActive)}>
                            {label}
                        </NavLink>
                    ))}
                </nav>

                <div style={{ flex: 1 }} />

                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <div style={{ textAlign: "right", marginRight: "8px" }}>
                        <div style={{ fontSize: "13px", fontWeight: 600, color: "#fff" }}>{user?.name}</div>
                        <div style={{ fontSize: "10px", color: "#64748b", textTransform: "uppercase" }}>
                            {isAdmin ? "Administrator" : "Uporabnik"}
                        </div>
                    </div>

                    <button onClick={() => navigate("/profil")} title="Profil" style={iconBtnStyle}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                    </button>

                    <button onClick={logout} title="Odjava" style={iconBtnStyle}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16,17 21,12 16,7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                    </button>

                    <button className="nav-hamburger" onClick={() => setMenuOpen(!menuOpen)}>
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#ffffff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        {menuOpen ? (
            <>
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
            </>
        ) : (
            <>
                {/* Tukaj so tri črtice */}
                <line x1="4" y1="6" x2="20" y2="6"/>
                <line x1="4" y1="12" x2="20" y2="12"/>
                <line x1="4" y1="18" x2="20" y2="18"/>
            </>
        )}
    </svg>
</button>

                    
                </div>
            </header>

            {menuOpen && (
                <div className="nav-mobile-menu" style={{ background: "#1f2937", padding: "8px 16px" }}>
                    {navLinks.map(({ to, label }) => (
                        <NavLink key={to} to={to} end={to === "/"} onClick={() => setMenuOpen(false)} style={({ isActive }) => ({ ...linkStyle(isActive), display: "block", marginBottom: "4px" })}>
                            {label}
                        </NavLink>
                    ))}
                </div>
            )}

            <main className="content" style={{ flex: 1 }}>
                <Outlet />
            </main>
        </div>
    );
}