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

    return (
        <article className="list-card">
            <div className="list-card-main">
                <h3>{road.relacija}</h3>
                <p>Tip: {road.tip}</p>
                <p>Stanje: {road.stanje}</p>

                {road.latitude != null && road.longitude != null && (
                    <p>Koordinate: {road.latitude}, {road.longitude}</p>
                )}
            </div>

            <div className="card-actions">
                {onToggleFavourite && (
                    <button
                        className={isFavourite ? "favourite active" : "favourite"}
                        onClick={() => onToggleFavourite(road)}
                    >
                        {isFavourite ? "★ Priljubljeno" : "☆ Priljubljeno"}
                    </button>
                )}

                {isAdmin && onEdit && (
                    <button onClick={() => onEdit(road)}>
                        Uredi
                    </button>
                )}

                {isAdmin && onDelete && (
                    <button
                        className="danger"
                        onClick={() => onDelete(road)}
                    >
                        Izbriši
                    </button>
                )}
            </div>
        </article>
    );
}