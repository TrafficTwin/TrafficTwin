import React, { useEffect, useState } from "react";
import EmptyState from "../components/common/EmptyState.jsx";
import {
    getCurrentUserProfile,
    removeFavouriteParking,
    removeFavouriteRoad
} from "../services/userApi.js";

function formatRole(role) {
    return role === "admin" ? "Administrator" : "Uporabnik";
}

function FavouriteParkingCard({ parking, onRemove }) {
    const capacity = Number(parking.capacity) || 0;
    const occupied = Number(parking.occupied) || 0;
    const free = Math.max(capacity - occupied, 0);

    return (
        <article className="list-card">
            <div>
                <h3>{parking.location}</h3>
                <p>Tip plačila: {parking.typeOfPayment}</p>
                <p>Kapaciteta: {capacity}</p>
                <p>Zasedeno: {occupied}</p>
                <p>Prosto: {free}</p>
            </div>

            <div className="card-actions">
                <button
                    className="danger"
                    onClick={() => onRemove(parking.id)}
                >
                    Odstrani
                </button>
            </div>
        </article>
    );
}

function FavouriteRoadCard({ road, onRemove }) {
    return (
        <article className="list-card">
            <div>
                <h3>{road.relacija}</h3>
                <p>Tip: {road.tip}</p>
                <p>Stanje: {road.stanje}</p>

                {road.latitude != null && road.longitude != null && (
                    <p>
                        Koordinate: {road.latitude}, {road.longitude}
                    </p>
                )}
            </div>

            <div className="card-actions">
                <button
                    className="danger"
                    onClick={() => onRemove(road.id)}
                >
                    Odstrani
                </button>
            </div>
        </article>
    );
}

export default function UserPage() {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function loadProfile() {
        try {
            setLoading(true);
            setError("");

            const data = await getCurrentUserProfile();
            setProfile(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadProfile();
    }, []);

    async function handleRemoveParking(parkingId) {
        try {
            setError("");
            const data = await removeFavouriteParking(parkingId);
            setProfile(data);
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleRemoveRoad(roadId) {
        try {
            setError("");
            const data = await removeFavouriteRoad(roadId);
            setProfile(data);
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Profil</p>
                    <h2>Uporabniška stran</h2>
                </div>
            </div>

            {error && <div className="error-box">{error}</div>}
            {loading && <div className="info-box">Nalaganje profila ...</div>}

            {profile && (
                <>
                    <div className="profile-grid">
                        <article className="card">
                            <span className="card-label">Ime</span>
                            <strong className="profile-value">{profile.name}</strong>
                        </article>

                        <article className="card">
                            <span className="card-label">E-pošta</span>
                            <strong className="profile-value">{profile.email}</strong>
                        </article>

                        <article className="card">
                            <span className="card-label">Vloga</span>
                            <strong className="profile-value">{formatRole(profile.role)}</strong>
                        </article>
                    </div>

                    <div className="profile-section">
                        <h3>Priljubljena parkirišča</h3>

                        {profile.favouriteParkings?.length > 0 ? (
                            <div className="list">
                                {profile.favouriteParkings.map((parking) => (
                                    <FavouriteParkingCard
                                        key={parking.id}
                                        parking={parking}
                                        onRemove={handleRemoveParking}
                                    />
                                ))}
                            </div>
                        ) : (
                            <EmptyState
                                title="Ni priljubljenih parkirišč"
                                description="Parkirišča lahko označiš kot priljubljena na strani Parkirišča."
                            />
                        )}
                    </div>

                    <div className="profile-section">
                        <h3>Priljubljene ceste</h3>

                        {profile.favouriteRoads?.length > 0 ? (
                            <div className="list">
                                {profile.favouriteRoads.map((road) => (
                                    <FavouriteRoadCard
                                        key={road.id}
                                        road={road}
                                        onRemove={handleRemoveRoad}
                                    />
                                ))}
                            </div>
                        ) : (
                            <EmptyState
                                title="Ni priljubljenih cest"
                                description="Ceste lahko označiš kot priljubljene na strani Stanje cest."
                            />
                        )}
                    </div>
                </>
            )}
        </section>
    );
}