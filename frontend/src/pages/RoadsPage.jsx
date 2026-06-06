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
import { AuthContext } from "../context/AuthContext";

export default function RoadsPage() {
    const [roadStates, setRoadStates] = useState([]);
    const [search, setSearch] = useState("");
    const [sortMode, setSortMode] = useState("LOCATION");
    const [editingRoad, setEditingRoad] = useState(null);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

  async function loadRoadStates() {
    try {
        setLoading(true);
        console.log("Kličem API za ceste..."); // DODAJ TOLE
        const data = await getRoadStates();
        console.log("Podatki iz API-ja:", data); // DODAJ TOLE
        setRoadStates(data ?? []);
    } catch (err) {
        console.error("Napaka pri loadRoadStates:", err); // DODAJ TOLE
        setError(err.message);
    } finally {
        setLoading(false);
    }
}

    useEffect(() => {
        loadRoadStates();
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
        setRoadStates((current) => {
            if (editingRoad == null) {
                // Novo dodano
                return [...current, road];
            } else {
                // Posodobljeno
                return current.map((item) => (item === editingRoad ? road : item));
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

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Stanje cest</p>
                    <h2>Upravljanje stanja cest</h2>
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
<<<<<<< Updated upstream
                            key={road.relacija || Math.random()} 
=======
                            key={road.id ?? `road-${index}`}
>>>>>>> Stashed changes
                            road={road}
                            onEdit={() => handleEdit(road)}
                            onDelete={() => handleDelete(road)}
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