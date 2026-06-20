import React from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext.jsx";
import AppLayout from "./layout/AppLayout.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";
import ParkingPage from "./pages/ParkingPage.jsx";
import RoadsPage from "./pages/RoadsPage.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";
import MapPage from "./pages/MapPage.jsx";
import UserPage from "./pages/UserPage.jsx";

function PrivateRoute({ children }) {
    const { user, loading } = useAuth();
    if (loading) return <div>Nalaganje...</div>;
    return user ? children : <Navigate to="/login" replace />;
}

function AdminRoute({ children }) {
    const { user, isAdmin, loading } = useAuth();
    if (loading) return null;
    if (!user) return <Navigate to="/login" replace />;
    if (!isAdmin) return <Navigate to="/" replace />;
    return children;
}

function PublicRoute({ children }) {
    const { user, loading } = useAuth();
    if (loading) return null;
    return user ? <Navigate to="/" replace /> : children;
}

function AppRoutes() {
    return (
        <Routes>
            <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
            <Route path="/register" element={<RegisterPage />} />
            <Route element={<PrivateRoute><AppLayout /></PrivateRoute>}>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/profil" element={<UserPage />} />
                <Route path="/parking" element={<ParkingPage />} />
                <Route path="/road-status" element={<RoadsPage />} />
                <Route path="/map" element={<MapPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
        </Routes>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <AppRoutes />
            </BrowserRouter>
        </AuthProvider>
    );
}