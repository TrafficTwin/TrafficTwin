import React, { useEffect, useMemo, useState } from "react";
import EmptyState from "../components/common/EmptyState.jsx";
import RoadCard from "../components/roads/RoadCard.jsx";
import RoadFormDialog from "../components/roads/RoadFormDialog.jsx";
import RoadToolbar from "../components/roads/RoadToolbar.jsx";
import {
    clearRoadStates,
    deleteRoadState,
    getRoadStates,
    updateRoadState,
    createRoadState 
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
const [modifiedRoads, setModifiedRoads] = useState(new Set());

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


async function handleSave(road) {
    try {
        setError("");

        if (editingRoad == null) {
            // TUKAJ KLICEMO NOVO FUNKCIJO ZA USTVARJANJE
            const newRoad = await createRoadState(road);
            setRoadStates((current) => [...current, newRoad]);
        } else {
            // UREJANJE obstoječe
            const updatedRoad = await updateRoadState(editingRoad.id, road);
            setRoadStates((current) => 
                current.map((item) => (item.id === editingRoad.id ? updatedRoad : item))
            );
        }

        setIsDialogOpen(false);
        setEditingRoad(null);
    } catch (err) {
        setError("Napaka pri shranjevanju: " + err.message);
    }
}

// Popravi handleSync, da pošlje le spremenjene ceste
async function handleSync() {
    try {
        setError("");
        // Poišči ceste, ki so v modifiedRoads setu
        const roadsToSync = roadStates.filter(r => modifiedRoads.has(r.id));
        
        if (roadsToSync.length === 0) {
            alert("Ni novih sprememb za sinhronizacijo.");
            return;
        }

        // Sinhroniziraj samo tiste ceste, ki so se spremenile
        // Če tvoj backend ne podpira arraya v sync, uporabi zanko
        for (const road of roadsToSync) {
            await updateRoadState(road.id, road);
        }
        
        setModifiedRoads(new Set()); // Počisti seznam sprememb
        await loadRoadStates();
        alert("Spremembe uspešno sinhronizirane!");
    } catch (err) {
        setError("Napaka pri sinhronizaciji: " + err.message);
    }
}

    async function handleDelete(roadToDelete) {
        const confirmed = window.confirm("Res želiš izbrisati stanje ceste?");
        if (!confirmed) return;
        try {
            setError("");
            if (roadToDelete.id) {
                await deleteRoadState(roadToDelete.id);
            }
            setRoadStates((current) => current.filter((r) => r !== roadToDelete));
        } catch (err) {
            setError("Napaka pri brisanju: " + err.message);
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