import React from "react";


export default function StatCard({ label, value, description }) {
    return (
        <article className="card">
            <span className="card-label">{label}</span>
            <strong className="stat-value">{value}</strong>
            {description && <p>{description}</p>}
        </article>
    );
}