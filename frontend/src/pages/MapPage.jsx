import React from "react";
import TrafficMap from "../components/map/TrafficMap.jsx";

export default function MapPage() {
    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Zemljevid</p>
                    <h2>Parkirišča in stanje cest</h2>
                </div>
            </div>

            <TrafficMap />
        </section>
    );
}