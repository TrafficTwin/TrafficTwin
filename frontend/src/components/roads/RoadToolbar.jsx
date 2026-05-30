import React from "react";
export default function RoadToolbar({
                                        search,
                                        onSearchChange,
                                        sortMode,
                                        onSortChange,
                                        onRefresh,
                                        onAdd,
                                        onSave,
                                        onClear
                                    }) {
    return (
        <div className="toolbar">
            <input
                value={search}
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder="Išči relacijo"
            />

            <select value={sortMode} onChange={(event) => onSortChange(event.target.value)}>
                <option value="LOCATION">Lokacija A-Ž</option>
                <option value="STATE">Stanje ceste A-Ž</option>
                <option value="TYPE">Tip ceste A-Ž</option>
            </select>

            <button onClick={onRefresh}>Osveži</button>
            <button onClick={onAdd}>Dodaj</button>
            <button className="primary" onClick={onSave}>Shrani</button>
            <button className="danger" onClick={onClear}>Počisti</button>
        </div>
    );
}