import { useEffect, useState } from "react";
import axios from "axios";
import "./AdminDashboard.css";

export default function AdminDashboard() {

    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

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

                            <button className="assign-btn">
                                Assign Staff
                            </button>

                        </div>
                    ))}
                </div>
            )}

        </div>
    );
}
