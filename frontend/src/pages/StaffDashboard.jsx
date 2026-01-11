import { useEffect, useState } from "react";
import axios from "axios";
import "./StaffDashboard.css";

export default function StaffDashboard() {

    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const token = localStorage.getItem("token");

    useEffect(() => {
        fetchAssignedRequests();
    }, []);

    const fetchAssignedRequests = async () => {
        try {
            const res = await axios.get(
                "http://localhost:8080/api/staff/amenities/my",
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );
            setRequests(res.data);
        } catch (err) {
            setError("Failed to load assigned requests");
        } finally {
            setLoading(false);
        }
    };

    const markDelivered = async (requestId) => {
        try {
            await axios.put(
                `http://localhost:8080/api/staff/amenities/${requestId}/delivered`,
                {},
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            );
            fetchAssignedRequests(); // refresh
        } catch (err) {
            alert("Failed to mark as delivered");
        }
    };

    if (loading) return <p className="staff-status">Loading...</p>;
    if (error) return <p className="staff-status error">{error}</p>;

    return (
        <div className="staff-dashboard">

            <div className="staff-header">
                <h1>My Assigned Tasks</h1>
                <p>Complete assigned amenity requests</p>
            </div>

            {requests.length === 0 ? (
                <p className="empty-text">No active assignments</p>
            ) : (
                <div className="task-grid">
                    {requests.map(req => (
                        <div className="task-card" key={req.id}>

                            <div className="task-top">
                                <span className="classroom">
                                    {req.classRoom}
                                </span>
                                <span className="status assigned">
                                    ASSIGNED
                                </span>
                            </div>

                            <p className="prof">
                                Requested by: <b>{req.professorName}</b>
                            </p>

                            <div className="items">
                                {req.items.map((item, i) => (
                                    <span className="item-chip" key={i}>
                                        {item}
                                    </span>
                                ))}
                            </div>

                            <button
                                className="deliver-btn"
                                onClick={() => markDelivered(req.id)}
                            >
                                Mark Delivered
                            </button>

                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
