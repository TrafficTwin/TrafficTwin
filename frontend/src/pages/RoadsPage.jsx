import React, { useEffect, useMemo, useState } from "react";
import EmptyState from "../components/common/EmptyState.jsx";
import RoadCard from "../components/roads/RoadCard.jsx";
import RoadFormDialog from "../components/roads/RoadFormDialog.jsx";
import RoadToolbar from "../components/roads/RoadToolbar.jsx";

import {
    clearRoadStates,
    getRoadStates,
    syncRoadStates
} from "../services/roadsApi.js";
import {
    addFavouriteRoad,
    getCurrentUserProfile,
    removeFavouriteRoad
} from "../services/userApi.js";

export default function RoadsPage() {
    const [roadStates, setRoadStates] = useState([]);
    const [search, setSearch] = useState("");
    const [sortMode, setSortMode] = useState("LOCATION");
    const [editingRoad, setEditingRoad] = useState(null);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [favouriteRoadIds, setFavouriteRoadIds] = useState([]);

    function createRoadClientId(road) {
        const slug = String(`${road.tip ?? ""}-${road.relacija ?? ""}`)
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .replace(/[^a-z0-9]+/g, "-")
            .replace(/^-+|-+$/g, "");
        return slug ? `road-${slug}` : `road-${Date.now()}`;
    }

    async function loadFavourites() {
        try {
            const profile = await getCurrentUserProfile();
            setFavouriteRoadIds(profile.favouriteRoadIds ?? []);
        } catch (err) {
            setError(err.message);
        }
    }

    async function loadRoadStates() {
        try {
            setLoading(true);
            const data = await getRoadStates();
            setRoadStates(data ?? []);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadRoadStates();
        loadFavourites();
    }, []);

    const visibleRoads = useMemo(() => {
        return [...roadStates]
            .filter((road) =>
                road.relacija?.toLowerCase().includes(search.toLowerCase())
            )
            .sort((a, b) => {
                if (sortMode === "STATE") return a.stanje.localeCompare(b.stanje);
                if (sortMode === "TYPE") return a.tip.localeCompare(b.tip);
                return a.relacija.localeCompare(b.relacija);
            });
    }, [roadStates, search, sortMode]);

    function handleAdd() {
        setEditingRoad(null);
        setIsDialogOpen(true);
    }

    function handleEdit(road) {
        setEditingRoad(road);
        setIsDialogOpen(true);
    }

    function handleSave(road) {
        const roadWithId = {
            ...road,
            id: road.id ?? createRoadClientId(road)
        };
        setRoadStates((current) => {
            if (editingRoad == null) {
                return [...current, roadWithId];
            } else {
                return current.map((item) => (item === editingRoad ? roadWithId : item));
            }
        });
        setIsDialogOpen(false);
        setEditingRoad(null);
    }

    function handleDelete(roadToDelete) {
        setRoadStates((current) => current.filter((r) => r !== roadToDelete));
    }

    async function handleSync() {
        try {
            setError("");
            await syncRoadStates(roadStates);
            await loadRoadStates();
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleClear() {
        const confirmed = window.confirm("Res želiš izbrisati vsa stanja cest?");
        if (!confirmed) return;
        try {
            setError("");
            await clearRoadStates();
            setRoadStates([]);
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleToggleFavourite(road) {
        try {
            setError("");
            if (!road.id) {
                setError("Cesta nima ID-ja. Najprej jo shrani oziroma sinhroniziraj.");
                return;
            }
            const isFavourite = favouriteRoadIds.includes(road.id);
            if (isFavourite) {
                await removeFavouriteRoad(road.id);
                setFavouriteRoadIds((current) => current.filter((id) => id !== road.id));
            } else {
                await addFavouriteRoad(road.id);
                setFavouriteRoadIds((current) => [...current, road.id]);
            }
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Stanje cest</p>
                    <h2>Pregled stanj cest</h2>
                </div>
            </div>

            <RoadToolbar
                search={search}
                onSearchChange={setSearch}
                sortMode={sortMode}
                onSortChange={setSortMode}
                onRefresh={loadRoadStates}
                onAdd={handleAdd}
                onSave={handleSync}
                onClear={handleClear}
            />

            {error && <div className="error-box">{error}</div>}
            {loading && <div className="info-box">Nalaganje podatkov ...</div>}

            {!loading && visibleRoads.length === 0 ? (
                <EmptyState
                    title="Ni podatkov"
                    description="Klikni Osveži ali dodaj stanje ceste."
                />
            ) : (
                <div className="list">
                    {visibleRoads.map((road, index) => (
                        <RoadCard
                            key={road.id ?? `road-${index}`}
                            road={road}
                            onEdit={() => handleEdit(road)}
                            onDelete={() => handleDelete(road)}
                            isFavourite={favouriteRoadIds.includes(road.id)}
                            onToggleFavourite={handleToggleFavourite}
                        />
                    ))}
                </div>
            )}

            {isDialogOpen && (
                <RoadFormDialog
                    road={editingRoad}
                    onClose={() => {
                        setIsDialogOpen(false);
                        setEditingRoad(null);
                    }}
                    onSave={handleSave}
                />
            )}
        </section>
    );
}