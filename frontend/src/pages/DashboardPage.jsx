import React from "react";


import { useEffect, useState } from "react";
import StatCard from "../components/common/StatCard.jsx";
import { getParking } from "../services/parkingApi.js";
import { getRoadStates } from "../services/roadsApi.js";

export default function DashboardPage() {
    const [parkingCount, setParkingCount] = useState("-");
    const [roadCount, setRoadCount] = useState("-");

    useEffect(() => {
        async function loadStats() {
            try {
                const [parking, roads] = await Promise.all([
                    getParking(),
                    getRoadStates()
                ]);

                setParkingCount(parking?.length ?? 0);
                setRoadCount(roads?.length ?? 0);
            } catch {
                setParkingCount("Napaka");
                setRoadCount("Napaka");
            }
        }

        loadStats();
    }, []);

    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Pregled</p>
                    <h2>Pregledna stran</h2>
                </div>
            </div>

            <div className="grid">
                <StatCard
                    label="Parkirišča"
                    value={parkingCount}
                    description="Število parkirišč v bazi."
                />

                <StatCard
                    label="Stanje cest"
                    value={roadCount}
                    description="Število zapisov stanja cest."
                />
            </div>
        </section>
    );
}