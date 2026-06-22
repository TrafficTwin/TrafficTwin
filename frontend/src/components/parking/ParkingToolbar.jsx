import React, { useContext } from "react";
import { AuthContext } from "../../context/AuthContext";

export default function ParkingToolbar({
    search,
    onSearchChange,
    sortMode,
    onSortChange,
    latitude,
    longitude,
    radius,
    onLatitudeChange,
    onLongitudeChange,
    onRadiusChange,
    onRefresh,
    onNearby,
    onAdd
}) {
    // Tukaj pridobimo informacijo, ali je uporabnik admin
    const { isAdmin } = useContext(AuthContext);

    return (
        <div className="toolbar">
            <input
                value={search}
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder="Išči lokacijo"
            />

            <select value={sortMode} onChange={(event) => onSortChange(event.target.value)}>
                <option value="LOCATION">Abecedno A-Ž</option>
                <option value="FREE">Najbolj prosto</option>
                <option value="NEAREST">Najbližje</option>
            </select>

           
            <button onClick={onRefresh}>Osveži</button>
            
            {/* Gumb se prikaže samo, če je isAdmin true */}
            {isAdmin && (
                <button className="primary" onClick={onAdd}>
                    Novo
                </button>
            )}
        </div>
    );
}