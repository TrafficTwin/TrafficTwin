import React from "react";

export default function RoadCard({ road, onEdit, onDelete }) {
    return (
        <article className="list-card">
            <div className="list-card-main">
                <h3>{road.relacija}</h3>
                <p>Tip: {road.tip}</p>
                <p>Stanje: {road.stanje}</p>
            </div>

            <div className="card-actions">
                <button onClick={onEdit}>Uredi</button>
                <button className="danger" onClick={onDelete}>Izbriši</button>
            </div>
        </article>
    );
}