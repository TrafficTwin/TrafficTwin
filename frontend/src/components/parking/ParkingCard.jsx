import React, { useContext } from "react"; // Dodan useContext
import { AuthContext } from "../../context/AuthContext"; // Popravljen uvoz

export default function ParkingCard({ parking, onEdit, onDelete }) {
    const { isAdmin } = useContext(AuthContext); // Zdaj bo delovalo
    
    const free = parking.capacity - parking.occupied;
    const percentage = parking.capacity > 0
        ? Math.round((parking.occupied / parking.capacity) * 100)
        : 0;

    return (
        <article className="list-card">
            <div className="list-card-main">
                <h3>{parking.location}</h3>
                <p>Tip plačila: {parking.typeOfPayment}</p>
                <p>Kapaciteta: {parking.capacity}</p>
                <p>Zasedeno: {parking.occupied}</p>
                <p>Prosto: {free}</p>
                <div className="progress">
                    <div style={{ width: `${percentage}%` }} />
                </div>
            </div>

            <div className="card-actions">
                {isAdmin && <button onClick={() => onEdit(parking)}>Uredi</button>}
                {isAdmin && (
                <button className="danger" onClick={() => onDelete(parking.id)}>
                    Izbriši
                </button>
    )}
            </div>
        </article>
    );
}