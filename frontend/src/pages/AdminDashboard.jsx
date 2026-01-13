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



    const token = localStorage.getItem("token");

    useEffect(() => {
        fetchPendingRequests();
    }, []);

    const fetchPendingRequests = async () => {
        try {
            const res = await axios.get(
                "http://localhost:8080/api/admin/amenities/pending",
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );

            setRequests(res.data);
        } catch (err) {
            setError("Failed to load pending requests");
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return <div className="admin-loading">Loading...</div>;
    }

    if (error) {
        return <div className="admin-error">{error}</div>;
    }
    const fetchStaff = async () => {
        setStaffLoading(true);
        try {
            const res = await axios.get(
                "http://localhost:8080/api/admin/amenities/staff/available",
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );
            setStaffList(res.data);
        } catch (err) {
            alert("Failed to load staff");
        } finally {
            setStaffLoading(false);
        }
    };
    const assignStaff = async (staffId) => {
        try {
            await axios.put(
                `http://localhost:8080/api/admin/amenities/${selectedRequestId}/assign/${staffId}`,
                {},
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );
            setShowStaffModal(false);
            fetchPendingRequests(); // refresh cards
        } catch (err) {
            alert("Assignment failed");
        }
    };



    return (
        <div className="admin-dashboard">

            <header className="admin-header">
                <h1>Admin Dashboard</h1>
                <p>Pending Amenity Requests</p>
            </header>

            {requests.length === 0 ? (
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
                                        className={`staff-btn ${staff.online ? "online" : "offline"}`}
                                        disabled={!staff.online}
                                        onClick={() => assignStaff(staff.staffId)}
                                    >
                                        {staff.name}
                                        {!staff.online && " (offline)"}
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
    );
}
