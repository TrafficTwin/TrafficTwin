import React from "react";
import { useAuth } from "../../context/AuthContext"; // Uporabi raje useAuth
export default function RoadCard({ road, onEdit, onDelete }) {
    const { isAdmin } = useAuth();

    return (
        <article className="list-card">
            <div className="list-card-main">
                <h3>{road.relacija}</h3>
                <p>Tip: {road.tip}</p>
                <p>Stanje: {road.stanje}</p>
            </div>

            <div className="card-actions">
                {isAdmin && <button onClick={() => onEdit(road)}>Uredi</button>}
                {isAdmin && (
                <button className="danger" onClick={() => onDelete(road)}>Izbriši</button>
    )}
            </div>
        </article>
    );
}