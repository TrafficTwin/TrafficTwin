import React from "react";
import { useEffect, useState } from "react";

const emptyRoad = {
    tip: "",
    relacija: "",
    stanje: ""
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
            stanje: form.stanje.trim()
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

                <div className="dialog-actions">
                    <button type="button" onClick={onClose}>Prekliči</button>
                    <button className="primary" type="submit">Shrani</button>
                </div>
            </form>
        </div>
    );
}