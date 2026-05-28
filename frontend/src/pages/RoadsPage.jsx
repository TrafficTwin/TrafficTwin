import React from "react";


import { useEffect, useMemo, useState } from "react";
import EmptyState from "../components/common/EmptyState.jsx";
import RoadCard from "../components/roads/RoadCard.jsx";
import RoadFormDialog from "../components/roads/RoadFormDialog.jsx";
import RoadToolbar from "../components/roads/RoadToolbar.jsx";
import {
    clearRoadStates,
    getRoadStates,
    syncRoadStates
} from "../services/roadsApi.js";

export default function RoadsPage() {
    const [roadStates, setRoadStates] = useState([]);
    const [search, setSearch] = useState("");
    const [sortMode, setSortMode] = useState("LOCATION");
    const [editingIndex, setEditingIndex] = useState(null);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    async function loadRoadStates() {
        try {
            setLoading(true);
            setError("");
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
    }, []);

    const visibleRoads = useMemo(() => {
        return roadStates
            .map((road, index) => ({ road, index }))
            .filter(({ road }) =>
                road.relacija?.toLowerCase().includes(search.toLowerCase())
            )
            .sort((a, b) => {
                if (sortMode === "STATE") {
                    return a.road.stanje.localeCompare(b.road.stanje);
                }

                if (sortMode === "TYPE") {
                    return a.road.tip.localeCompare(b.road.tip);
                }

                return a.road.relacija.localeCompare(b.road.relacija);
            });
    }, [roadStates, search, sortMode]);

    function handleAdd() {
        setEditingIndex(null);
        setIsDialogOpen(true);
    }

    function handleEdit(index) {
        setEditingIndex(index);
        setIsDialogOpen(true);
    }

    function handleSave(road) {
        if (editingIndex == null) {
            setRoadStates((current) => [...current, road]);
        } else {
            setRoadStates((current) =>
                current.map((item, index) => index === editingIndex ? road : item)
            );
        }

        setIsDialogOpen(false);
        setEditingIndex(null);
    }

    function handleDelete(index) {
        setRoadStates((current) => current.filter((_, i) => i !== index));
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
                    {visibleRoads.map(({ road, index }) => (
                        <RoadCard
                            key={`${road.relacija}-${index}`}
                            road={road}
                            onEdit={() => handleEdit(index)}
                            onDelete={() => handleDelete(index)}
                        />
                    ))}
                </div>
            )}

            {isDialogOpen && (
                <RoadFormDialog
                    road={editingIndex == null ? null : roadStates[editingIndex]}
                    onClose={() => {
                        setIsDialogOpen(false);
                        setEditingIndex(null);
                    }}
                    onSave={handleSave}
                />
            )}
        </section>
    );
}