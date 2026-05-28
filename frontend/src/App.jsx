import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./layout/AppLayout.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";
import ParkingPage from "./pages/ParkingPage.jsx";
import RoadsPage from "./pages/RoadsPage.jsx";
import React from "react";


export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route element={<AppLayout />}>
                    <Route path="/" element={<DashboardPage />} />
                    <Route path="/parking" element={<ParkingPage />} />
                    <Route path="/stanje-cest" element={<RoadsPage />} />
                    <Route path="*" element={<Navigate to="/" replace />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}