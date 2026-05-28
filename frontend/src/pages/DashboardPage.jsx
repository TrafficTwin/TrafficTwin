import React from "react";


export default function DashboardPage() {
    return (
        <section className="page">
            <div className="page-header">
                <div>
                    <p className="eyebrow">Pregled</p>
                    <h2>Nadzorna plošča</h2>
                </div>
            </div>

            <div className="grid">
                <article className="card">
                    <span className="card-label">Modul</span>
                    <strong>Parkirišča</strong>
                    <p>Pregled kapacitet, zasedenosti in lokacij parkirišč.</p>
                </article>

                <article className="card">
                    <span className="card-label">Modul</span>
                    <strong>Stanje cest</strong>
                    <p>Pregled relacij, tipov in trenutnega stanja cest.</p>
                </article>
            </div>
        </section>
    );
}