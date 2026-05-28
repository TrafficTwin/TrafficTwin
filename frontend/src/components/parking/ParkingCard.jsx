import React from "react";


export default function ParkingCard({ parking, onEdit, onDelete }) {
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

                {parking.latitude != null && parking.longitude != null && (
                    <p>
                        Koordinate: {Number(parking.latitude).toFixed(5)},{" "}
                        {Number(parking.longitude).toFixed(5)}
                    </p>
                )}

                {parking.distanceMeters != null && (
                    <p>Oddaljenost: {Number(parking.distanceMeters).toFixed(0)} m</p>
                )}

                <div className="progress">
                    <div style={{ width: `${percentage}%` }} />
                </div>
            </div>

            <div className="card-actions">
                <button onClick={() => onEdit(parking)}>Uredi</button>
                <button className="danger" onClick={() => onDelete(parking.id)}>Izbriši</button>
            </div>
        </article>
    );
}