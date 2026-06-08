import React from "react";
import { useAuth } from "../../context/AuthContext.jsx";

export default function ParkingCard({
                                        parking,
                                        onEdit,
                                        onDelete,
                                        isFavourite = false,
                                        onToggleFavourite
                                    }) {
    const { isAdmin } = useAuth();

    const capacity = Number(parking.capacity) || 0;
    const occupied = Number(parking.occupied) || 0;
    const free = Math.max(capacity - occupied, 0);

    const percentage = capacity > 0
        ? Math.round((occupied / capacity) * 100)
        : 0;

    return (
        <article className="list-card">
            <div className="list-card-main">
                <h3>{parking.location}</h3>
                <p>Tip plačila: {parking.typeOfPayment}</p>
                <p>Kapaciteta: {capacity}</p>
                <p>Zasedeno: {occupied}</p>
                <p>Prosto: {free}</p>

                <div className="progress">
                    <div style={{ width: `${percentage}%` }} />
                </div>
            </div>

            <div className="card-actions">
                {onToggleFavourite && (
                    <button
                        className={isFavourite ? "favourite active" : "favourite"}
                        onClick={() => onToggleFavourite(parking)}
                    >
                        {isFavourite ? "★ Priljubljeno" : "☆ Priljubljeno"}
                    </button>
                )}

                {isAdmin && onEdit && (
                    <button onClick={() => onEdit(parking)}>
                        Uredi
                    </button>
                )}

                {isAdmin && onDelete && (
                    <button
                        className="danger"
                        onClick={() => onDelete(parking.id)}
                    >
                        Izbriši
                    </button>
                )}
            </div>
        </article>
    );
}