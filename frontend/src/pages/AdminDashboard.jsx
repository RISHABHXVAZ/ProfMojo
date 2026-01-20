import { useEffect, useState } from "react";
import axios from "axios";
import "./AdminDashboard.css";

export default function AdminDashboard() {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [showStaffModal, setShowStaffModal] = useState(false);
    const [selectedRequestId, setSelectedRequestId] = useState(null);

    const [staffList, setStaffList] = useState([]);
    const [staffLoading, setStaffLoading] = useState(false);

    const [allStaff, setAllStaff] = useState([]);
    const [staffStatsLoading, setStaffStatsLoading] = useState(true);

    const token = localStorage.getItem("token");

    /* ================= FETCH PENDING REQUESTS ================= */
    const fetchPendingRequests = async () => {
        try {
            const res = await axios.get(
                "http://localhost:8080/api/admin/amenities/pending",
                {
                    headers: { Authorization: `Bearer ${token}` }
                }
            );
            setRequests(res.data);
        } catch (err) {
            setError("Failed to load pending requests");
        } finally {
            setLoading(false);
        }
    };

    /* ================= FETCH ALL STAFF (RIGHT PANEL) ================= */
    const fetchAllStaff = async () => {
        try {
            const res = await axios.get(
                "http://localhost:8080/api/admin/amenities/staff/all",
                {
                    headers: { Authorization: `Bearer ${token}` }
                }
            );
            setAllStaff(res.data);
        } catch (err) {
            console.error("Failed to load staff list");
        } finally {
            setStaffStatsLoading(false);
        }
    };

    /* ================= FETCH AVAILABLE STAFF (MODAL) ================= */
    const fetchStaff = async () => {
        setStaffLoading(true);
        try {
            const res = await axios.get(
                "http://localhost:8080/api/admin/amenities/staff/available",
                {
                    headers: { Authorization: `Bearer ${token}` }
                }
            );
            setStaffList(res.data);
        } catch {
            alert("Failed to load staff");
        } finally {
            setStaffLoading(false);
        }
    };

    /* ================= ASSIGN STAFF ================= */
    const assignStaff = async (staffId) => {
        try {
            await axios.put(
                `http://localhost:8080/api/admin/amenities/${selectedRequestId}/assign/${staffId}`,
                {},
                {
                    headers: { Authorization: `Bearer ${token}` }
                }
            );
            setShowStaffModal(false);
            fetchPendingRequests();
            fetchAllStaff();
        } catch {
            alert("Assignment failed");
        }
    };

    /* ================= INITIAL LOAD ================= */
    useEffect(() => {
        fetchPendingRequests();
        fetchAllStaff();
    }, []);

    /* ================= RENDER ================= */
    return (
        <div className="admin-page">
            <div className="admin-dashboard split">

                {/* ================= LEFT PANEL ================= */}
                <div className="admin-left">
                    <header className="admin-header">
                        <h1>Admin Dashboard</h1>
                        <p>Pending Amenity Requests</p>
                    </header>

                    {loading ? (
                        <p className="admin-loading">Loading...</p>
                    ) : error ? (
                        <p className="admin-error">{error}</p>
                    ) : requests.length === 0 ? (
                        <p className="empty-text">No pending requests 🎉</p>
                    ) : (
                        <div className="request-grid">
                            {requests.map(req => (
                                <div className="request-card" key={req.id}>
                                    <div className="card-header">
                                        <span className="department">{req.department}</span>
                                        <span className="status pending">{req.status}</span>
                                    </div>

                                    <p className="classroom">
                                        📍 Classroom: <strong>{req.classRoom}</strong>
                                    </p>

                                    <div className="items">
                                        {req.items.map((item, idx) => (
                                            <span key={idx} className="item-chip">
                                                {item}
                                            </span>
                                        ))}
                                    </div>

                                    <p className="meta">
                                        Requested by <strong>{req.professorName}</strong>
                                    </p>

                                    <button
                                        className="assign-btn"
                                        onClick={() => {
                                            setSelectedRequestId(req.id);
                                            setShowStaffModal(true);
                                            fetchStaff();
                                        }}
                                    >
                                        Assign Staff
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* ================= RIGHT PANEL ================= */}
                <div className="admin-right">
                    <h2>Staff Performance</h2>

                    {staffStatsLoading ? (
                        <p>Loading staff...</p>
                    ) : allStaff.length === 0 ? (
                        <p>No staff found</p>
                    ) : (
                        <div className="staff-performance-list">
                            {allStaff.map(staff => (
                                <div key={staff.staffId} className="staff-performance-card">
                                    <div className="staff-top">
                                        <span className="staff-name">{staff.name}</span>
                                        <span
                                            className={`staff-status ${staff.online ? "online" : "offline"
                                                }`}
                                        >
                                            {staff.online ? "Online" : "Offline"}
                                        </span>
                                    </div>

                                    <div className="staff-metrics">
                                        <span>⭐ {staff.stars}</span>
                                        <span>📦 {staff.totalDeliveries}</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* ================= ASSIGN MODAL ================= */}
                {showStaffModal && (
                    <div className="modal-overlay">
                        <div className="modal">
                            <h3>Assign Staff</h3>

                            {staffLoading ? (
                                <p>Loading staff...</p>
                            ) : (
                                <div className="staff-list">
                                    {staffList.map(staff => (
                                        <button
                                            key={staff.staffId}
                                            className={`staff-btn ${staff.online ? "online" : "offline"
                                                }`}
                                            disabled={!staff.online}
                                            onClick={() => assignStaff(staff.staffId)}
                                        >
                                            <div className="staff-stats">
                                                <p>⭐ Stars: {staff.stars}</p>
                                                <p>📦 Deliveries: {staff.totalDeliveries}</p>
                                            </div>
                                            <div>
                                                <div className="staff-name">{staff.name}</div>
                                                <div className="staff-status">
                                                    {staff.online ? "Available" : "Unavailable"}
                                                </div>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            )}

                            <button
                                className="close-btn"
                                onClick={() => setShowStaffModal(false)}
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
