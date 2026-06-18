import React from "react";
import { useAuth } from "../../context/AuthContext.jsx";

export default function RoadToolbar({
                                        search,
                                        onSearchChange,
                                        sortMode,
                                        onSortChange,
                                        onRefresh,
                                        onImportNap,
                                        onAdd,
                                        onSave,
                                        onClear
                                    }) {
    const { isAdmin } = useAuth();

    return (
        <div className="toolbar road-toolbar">
            <input
                value={search}
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder="Išči po lokaciji, viru, opisu ..."
            />

            <select value={sortMode} onChange={(event) => onSortChange(event.target.value)}>
                <option value="LOCATION">Lokacija A-Ž</option>
                <option value="STATE">Stanje A-Ž</option>
                <option value="TYPE">Tip A-Ž</option>
                <option value="SOURCE">Vir A-Ž</option>
                <option value="UPDATED">Najnovejše</option>
            </select>

            <button onClick={onRefresh}>Osveži</button>

            {isAdmin && (
                <>
                    <button className="primary" onClick={onImportNap}>Uvozi NAP</button>
                    <button onClick={onAdd}>Dodaj ročno</button>
                    <button onClick={onSave}>Shrani ročne spremembe</button>
                    <button className="danger" onClick={onClear}>Počisti</button>
                </>
            )}
        </div>
    );
}
