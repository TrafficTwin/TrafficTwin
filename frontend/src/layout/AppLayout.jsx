import { NavLink, Outlet } from "react-router-dom";
import React from "react";
import { useAuth } from "../context/AuthContext.jsx";

export default function AppLayout() {
    const { user, isAdmin, logout } = useAuth();

    return (
        <div className="shell">
            <aside className="sidebar">
                <div>
                    <p className="eyebrow">TrafficTwin</p>
                    <h1>Management</h1>
                </div>

                <nav className="nav">
                    <NavLink to="/">Pregled</NavLink>
                    <NavLink to="/parking">Parkirišča</NavLink>
                    <NavLink to="/stanje-cest">Stanje cest</NavLink>
                </nav>

                <div className="sidebar-user">
                    <div className="sidebar-user-info">
                        <span className="sidebar-user-name">{user?.name}</span>
                        <span className="sidebar-user-role">{isAdmin ? "Administrator" : "Uporabnik"}</span>
                    </div>
                    <button className="sidebar-logout" onClick={logout} aria-label="Odjava">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                            <polyline points="16,17 21,12 16,7"/>
                            <line x1="21" y1="12" x2="9" y2="12"/>
                        </svg>
                    </button>
                </div>
            </aside>

            <main className="content">
                <Outlet />
            </main>
        </div>
    );
}