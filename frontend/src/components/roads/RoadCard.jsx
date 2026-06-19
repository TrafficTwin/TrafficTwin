import React from "react";
import { useAuth } from "../../context/AuthContext.jsx";

export default function RoadCard({
                                     road,
                                     onEdit,
                                     onDelete,
                                     isFavourite = false,
                                     onToggleFavourite
                                 }) {
    const { isAdmin } = useAuth();

    const shortText = (text, maxLength = 120) => {
    if (!text) return "-";
    return text.length > maxLength ? text.slice(0, maxLength) + "..." : text;
};

    const title = road.title || road.relation || road.sourceName || "Prometni zapis";
    const description = road.description && road.description !== road.state
        ? road.description
        : "";

        console.log("Road ID:", road.id);

    return (
        <article className="list-card road-card">
            <div className="list-card-main">
                {/*<div className="road-card-header">
                    <h3>{title}</h3>
                    <span className="road-source-badge">{road.sourceName || road.type || "NAP"}</span>
                </div>*/}

                <p><strong>Tip:</strong> {road.type || road.recordType || road.tip || "-"}</p>
                <p><strong>Lokacija/relacija:</strong> {road.relation || road.relacija || "-"}</p>
                <p><strong>Stanje/opis:</strong> {shortText(road.state || road.stanje || "-")}</p>

                {description && (
                    <p><strong>Podrobnosti:</strong> {shortText(description)}</p>
                )}

                {/*<div className="road-meta">
                    {road.language && <span>Jezik: {road.language}</span>}
                    {road.format && <span>Format: {road.format}</span>}
                    {road.napCode && <span>Koda: {road.napCode}</span>}
                    {road.startTime && <span>Od: {formatDate(road.startTime)}</span>}
                    {road.endTime && <span>Do: {formatDate(road.endTime)}</span>}
                    {road.lastUpdated && <span>Uvoženo: {formatDate(road.lastUpdated)}</span>}
                </div>*/}

                {road.latitude != null && road.longitude != null && (
                    <p><strong>Koordinate:</strong> {Number(road.latitude).toFixed(5)}, {Number(road.longitude).toFixed(5)}</p>
                )}
            </div>

            <div className="card-actions">
                {onToggleFavourite && road.id && (
    <button
        className={isFavourite ? "favourite active" : "favourite"}
        onClick={() => {
            console.log("Kliknil si na gumb, ID je:", road.id); // <--- TUKAJ se izvede!
            onToggleFavourite(road);
        }}
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