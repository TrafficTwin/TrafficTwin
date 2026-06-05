import React from "react";
import { useEffect, useState } from "react";

const emptyRoad = {
    tip: "",
    relacija: "",
    stanje: "",
    latitude: "",
    longitude: ""
};

export default function RoadFormDialog({ road, onClose, onSave }) {
    const [form, setForm] = useState(emptyRoad);

    useEffect(() => {
        setForm(road ?? emptyRoad);
    }, [road]);

    function updateField(field, value) {
        setForm((current) => ({
            ...current,
            [field]: value
        }));
    }

    function submit(event) {
        event.preventDefault();

        onSave({
            tip: form.tip.trim(),
            relacija: form.relacija.trim(),
            stanje: form.stanje.trim(),
            latitude: form.latitude === "" ? null : Number(form.latitude),
            longitude: form.longitude === "" ? null : Number(form.longitude)
        });
    }

    return (
        <div className="dialog-backdrop">
            <form className="dialog" onSubmit={submit}>
                <h3>{road ? "Uredi stanje ceste" : "Dodaj stanje ceste"}</h3>

                <label>
                    Tip
                    <input
                        value={form.tip}
                        onChange={(event) => updateField("tip", event.target.value)}
                        required
                    />
                </label>

                <label>
                    Relacija
                    <input
                        value={form.relacija}
                        onChange={(event) => updateField("relacija", event.target.value)}
                        required
                    />
                </label>

                <label>
                    Stanje
                    <input
                        value={form.stanje}
                        onChange={(event) => updateField("stanje", event.target.value)}
                        required
                    />
                </label>

                <label>
                    Latitude
                    <input
                        value={form.latitude ?? ""}
                        onChange={(event) => updateField("latitude", event.target.value)}
                        placeholder="46.5547"
                    />
                </label>

                <label>
                    Longitude
                    <input
                        value={form.longitude ?? ""}
                        onChange={(event) => updateField("longitude", event.target.value)}
                        placeholder="15.6459"
                    />
                </label>

                <div className="dialog-actions">
                    <button type="button" onClick={onClose}>Prekliči</button>
                    <button className="primary" type="submit">Shrani</button>
                </div>
            </form>
        </div>
    );
}