import React from "react";
import { useEffect, useMemo, useState } from "react";
import EmptyState from "../components/common/EmptyState.jsx";
import ParkingCard from "../components/parking/ParkingCard.jsx";
import ParkingFormDialog from "../components/parking/ParkingFormDialog.jsx";
import ParkingToolbar from "../components/parking/ParkingToolbar.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import {
    createParking,
    deleteParking,
    getNearbyParking,
    getParking,
    updateParking
} from "../services/parkingApi.js";
import {
    addFavouriteParking,
    getCurrentUserProfile,
    removeFavouriteParking
} from "../services/userApi.js";

export default function ParkingPage() {
    const { isAdmin } = useAuth();
    const [parkingLots, setParkingLots] = useState([]);
    const [search, setSearch] = useState("");
    const [sortMode, setSortMode] = useState("LOCATION");
    const [latitude, setLatitude] = useState("46.5547");
    const [longitude, setLongitude] = useState("15.6459");
    const [radius, setRadius] = useState("1000");
    const [editingParking, setEditingParking] = useState(null);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [favouriteParkingIds, setFavouriteParkingIds] = useState([]);

    async function loadFavourites() {
        try {
            const profile = await getCurrentUserProfile();
            setFavouriteParkingIds(profile.favouriteParkingIds ?? []);
        } catch (err) {
            setError(err.message);
        }
    }

    async function loadParking() {
        try {
            setLoading(true);
            setError("");
            const data = await getParking();
            setParkingLots(data ?? []);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadParking();
        loadFavourites();
    }, []);

    const visibleParking = useMemo(() => {
        return parkingLots
            .filter((parking) =>
                parking.location?.toLowerCase().includes(search.toLowerCase())
            )
            .sort((a, b) => {
                if (sortMode === "FREE") {
                    return (b.capacity - b.occupied) - (a.capacity - a.occupied);
                }

                if (sortMode === "NEAREST") {
                    return (a.distanceMeters ?? Number.MAX_VALUE) - (b.distanceMeters ?? Number.MAX_VALUE);
                }

                return a.location.localeCompare(b.location);
            });
    }, [parkingLots, search, sortMode]);

    async function handleToggleFavourite(parking) {
        try {
            setError("");

            const parkingId = Number(parking.id);
            const isFavourite = favouriteParkingIds.includes(parkingId);

            if (isFavourite) {
                await removeFavouriteParking(parkingId);
                setFavouriteParkingIds((current) =>
                    current.filter((id) => id !== parkingId)
                );
            } else {
                await addFavouriteParking(parkingId);
                setFavouriteParkingIds((current) => [...current, parkingId]);
            }
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleNearby() {
        const lat = Number(latitude);
        const lon = Number(longitude);
        const radiusMeters = Number(radius) || 1000;

        if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
            setError("Vnesi veljavni koordinati lat/lon.");
            return;
        }

        try {
            setLoading(true);
            setError("");
            const data = await getNearbyParking(lat, lon, radiusMeters);
            setParkingLots(data ?? []);
            setSortMode("NEAREST");
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    function handleAdd() {
        setEditingParking(null);
        setIsDialogOpen(true);
    }

    function handleEdit(parking) {
        setEditingParking(parking);
        setIsDialogOpen(true);
    }

    async function handleSave(parking) {
        try {
            setError("");

            if (editingParking) {
                await updateParking(editingParking.id, parking);
            } else {
                await createParking(parking);
            }

            setIsDialogOpen(false);
            setEditingParking(null);
            await loadParking();
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleDelete(id) {
        const confirmed = window.confirm("Res želiš izbrisati parkirišče?");
        if (!confirmed) return;

        try {
            setError("");
            await deleteParking(id);
            await loadParking();
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Parkirišča</p>
                    <h2>Upravljanje parkirišč</h2>
                </div>
            </div>

            <ParkingToolbar
                search={search}
                onSearchChange={setSearch}
                sortMode={sortMode}
                onSortChange={setSortMode}
                latitude={latitude}
                longitude={longitude}
                radius={radius}
                onLatitudeChange={setLatitude}
                onLongitudeChange={setLongitude}
                onRadiusChange={setRadius}
                onRefresh={loadParking}
                onNearby={handleNearby}
                onAdd={isAdmin ? handleAdd : null}
            />

            {error && <div className="error-box">{error}</div>}
            {loading && <div className="info-box">Nalaganje podatkov ...</div>}

            {!loading && visibleParking.length === 0 ? (
                <EmptyState
                    title="Ni parkirišč"
                    description="Klikni Osveži ali dodaj novo parkirišče."
                />
            ) : (
                <div className="list">
                    {visibleParking.map((parking) => (
                        <ParkingCard
                            key={parking.id}
                            parking={parking}
                            onEdit={isAdmin ? handleEdit : null}
                            onDelete={isAdmin ? handleDelete : null}
                            isFavourite={favouriteParkingIds.includes(Number(parking.id))}
                            onToggleFavourite={handleToggleFavourite}
                        />
                    ))}
                </div>
            )}

            {isDialogOpen && (
                <ParkingFormDialog
                    parking={editingParking}
                    onClose={() => {
                        setIsDialogOpen(false);
                        setEditingParking(null);
                    }}
                    onSave={handleSave}
                />
            )}
        </section>
    );
}