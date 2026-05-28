import { NavLink, Outlet } from "react-router-dom";
import React from "react";


export default function AppLayout() {
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
            </aside>

            <main className="content">
                <Outlet />
            </main>
        </div>
    );
}