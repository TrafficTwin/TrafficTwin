import React from "react";
import { useAuth } from "../../context/AuthContext.jsx";

function formatDate(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return date.toLocaleString("sl-SI");
}

function shortText(value, maxLength = 260) {
    const text = String(value ?? "").trim();
    if (text.length <= maxLength) return text;
    return `${text.slice(0, maxLength).trim()}…`;
}

export default function RoadCard({
                                     road,
                                     onEdit,
                                     onDelete,
                                     isFavourite = false,
                                     onToggleFavourite
                                 }) {
    const { isAdmin } = useAuth();

    const title = road.title || road.relacija || road.sourceName || "Prometni zapis";
    const description = road.description && road.description !== road.stanje
        ? road.description
        : "";

    return (
        <article className="list-card road-card">
            <div className="list-card-main">
                <div className="road-card-header">
                    <h3>{title}</h3>
                    <span className="road-source-badge">{road.sourceName || road.tip || "NAP"}</span>
                </div>

                <p><strong>Tip:</strong> {road.tip || road.recordType || "-"}</p>
                <p><strong>Lokacija/relacija:</strong> {road.relacija || "-"}</p>
                <p><strong>Stanje/opis:</strong> {shortText(road.stanje || "-")}</p>

                {description && (
                    <p><strong>Podrobnosti:</strong> {shortText(description)}</p>
                )}

                <div className="road-meta">
                    {road.language && <span>Jezik: {road.language}</span>}
                    {road.format && <span>Format: {road.format}</span>}
                    {road.napCode && <span>Koda: {road.napCode}</span>}
                    {road.startTime && <span>Od: {formatDate(road.startTime)}</span>}
                    {road.endTime && <span>Do: {formatDate(road.endTime)}</span>}
                    {road.lastUpdated && <span>Uvoženo: {formatDate(road.lastUpdated)}</span>}
                </div>

                {road.latitude != null && road.longitude != null && (
                    <p><strong>Koordinate:</strong> {Number(road.latitude).toFixed(5)}, {Number(road.longitude).toFixed(5)}</p>
                )}
            </div>

            <div className="card-actions">
                {onToggleFavourite && road.id && (
                    <button
                        className={isFavourite ? "favourite active" : "favourite"}
                        onClick={() => onToggleFavourite(road)}
                    >
                        {isFavourite ? "★ Priljubljeno" : "☆ Priljubljeno"}
                    </button>
                )}

                {isAdmin && onEdit && (
                    <button onClick={() => onEdit(road)}>Uredi</button>
                )}

                {isAdmin && onDelete && (
                    <button className="danger" onClick={() => onDelete(road)}>Izbriši</button>
                )}
            </div>
        </article>
    );
}
