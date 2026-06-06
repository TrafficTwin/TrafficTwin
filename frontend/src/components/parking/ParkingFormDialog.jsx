import React, { useState, useEffect } from "react";
const emptyParking = {
    id: "",
    location: "",
    typeOfPayment: "FREE",
    capacity: "",
    occupied: "",
    latitude: "",
    longitude: ""
};

export default function ParkingFormDialog({ parking, onClose, onSave }) {
    const [form, setForm] = useState(emptyParking);

    useEffect(() => {
        if (parking) {
            setForm({
                id: parking.id ?? "",
                location: parking.location ?? "",
                typeOfPayment: parking.typeOfPayment ?? "FREE",
                capacity: parking.capacity ?? "",
                occupied: parking.occupied ?? "",
                latitude: parking.latitude ?? "",
                longitude: parking.longitude ?? ""
            });
        } else {
            setForm({
                ...emptyParking,
                id: Date.now()
            });
        }
    }, [parking]);

    function updateField(field, value) {
        setForm((current) => ({
            ...current,
            [field]: value
        }));
    }

    function submit(event) {
        event.preventDefault();

        const capacity = Number(form.capacity);
        const occupied = Number(form.occupied);
        const location = form.location.trim();

        if (!location) {
            alert("Lokacija ne sme biti prazna!");
        return;
        

        if (capacity < 0) {
            alert("Kapaciteta ne sme biti manjša od 0!");
            return;
        }

        if (occupied > capacity) {
            alert("Zasedenost ne sme presegati kapacitete!");
            return;
        }


        onSave({
            id: Number(form.id),
            location: form.location.trim(),
            typeOfPayment: form.typeOfPayment.trim(),
            capacity: Number(form.capacity),
            occupied: Number(form.occupied),
            latitude: form.latitude === "" ? null : Number(form.latitude),
            longitude: form.longitude === "" ? null : Number(form.longitude)
        });
    }

    return (
        <div className="dialog-backdrop">
            <form className="dialog" onSubmit={submit}>
                <h3>{parking ? "Uredi parkirišče" : "Novo parkirišče"}</h3>

                <label>
                    ID
                    <input
                        value={form.id}
                        onChange={(event) => updateField("id", event.target.value)}
                        disabled={Boolean(parking)}
                    />
                </label>

                <label>
                    Lokacija
                    <input
                        value={form.location}
                        onChange={(event) => updateField("location", event.target.value)}
                        required
                    />
                </label>

                <label>
                    Tip plačila
                    <input
                        value={form.typeOfPayment}
                        onChange={(event) => updateField("typeOfPayment", event.target.value)}
                        required
                    />
                </label>

                <label>
                    Kapaciteta
                    <input
                        type="number"
                        value={form.capacity}
                        onChange={(event) => updateField("capacity", event.target.value)}
                        required
                    />
                </label>

                <label>
                    Zasedeno
                    <input
                        type="number"
                        value={form.occupied}
                        onChange={(event) => updateField("occupied", event.target.value)}
                        required
                    />
                </label>

                <label>
                    Latitude
                    <input
                        value={form.latitude}
                        onChange={(event) => updateField("latitude", event.target.value)}
                    />
                </label>

                <label>
                    Longitude
                    <input
                        value={form.longitude}
                        onChange={(event) => updateField("longitude", event.target.value)}
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
