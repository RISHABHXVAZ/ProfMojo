import { useEffect, useState } from "react";
import axios from "axios";
import "./StaffDashboard.css";

export default function StaffDashboard() {
    const [requests, setRequests] = useState([]);
    const [staff, setStaff] = useState(null);
    const [loading, setLoading] = useState(true);
    const [now, setNow] = useState(Date.now());
    const [showLevels, setShowLevels] = useState(false); // State for the info popup

    const token = localStorage.getItem("token");

    useEffect(() => {
        fetchAssignedRequests();
        fetchProfile();
    }, []);

    useEffect(() => {
        const i = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(i);
    }, []);

    const fetchAssignedRequests = async () => {
        const res = await axios.get(
            "http://localhost:8080/api/staff/amenities/my",
            { headers: { Authorization: `Bearer ${token}` } }
        );
        setRequests(res.data);
        setLoading(false);
    };

    const fetchProfile = async () => {
        const res = await axios.get(
            "http://localhost:8080/api/staff/amenities/me",
            { headers: { Authorization: `Bearer ${token}` } }
        );
        setStaff(res.data);
    };

    const markDelivered = async (id) => {
        await axios.put(
            `http://localhost:8080/api/staff/amenities/${id}/delivered`,
            {},
            { headers: { Authorization: `Bearer ${token}` } }
        );
        fetchAssignedRequests();
        fetchProfile();
    };

    const slaText = (deadline) => {
        if (!deadline) return "—";
        const diff = new Date(deadline).getTime() - now;
        if (diff <= 0) return "SLA BREACHED";
        const m = Math.floor(diff / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        return `${m}m ${s}s`;
    };

    const level =
        staff?.stars >= 20 ? "gold" :
            staff?.stars >= 10 ? "silver" : "bronze";

    const levelLabel =
        level === "gold" ? "Gold" :
            level === "silver" ? "Silver" : "Bronze";

    if (loading) return <div className="staff-page">Loading…</div>;

    return (
        <div className="staff-page">
            <div className="staff-dashboard">

                {/* 🔝 PROFILE / STATS CARD */}
                {staff && (
                    <div className="profile-card">
                        <div className="profile-left">
                            <div>
                                <h2 className="staff-name">{staff.name}</h2>
                                <span className={`status ${staff.online ? "online" : "offline"}`}>
                                    {staff.online ? "Available" : "Unavailable"}
                                </span>
                            </div>
                        </div>

                        <div className="profile-stats">
                            <div className="stat-box">
                                <strong>{staff.stars}</strong>
                                <span>Stars Earned</span>
                            </div>

                            <div className="stat-box">
                                <strong>{staff.totalDeliveries}</strong>
                                <span>Total Deliveries</span>
                            </div>

                            <div className={`stat-box level ${level}`}>
                                <strong>{levelLabel}</strong>
                                <span className="service-level-label">
                                    Service Level
                                    <button 
                                        className="info-btn" 
                                        onClick={() => setShowLevels(!showLevels)}
                                        title="View levels info"
                                    >
                                        i
                                    </button>
                                    {showLevels && (
                                        <div className="levels-popup">
                                            <h4>Level Requirements</h4>
                                            <ul>
                                                <li><span className="gold-text">Gold:</span> 20+ Stars</li>
                                                <li><span className="silver-text">Silver:</span> 10-20 Stars</li>
                                                <li><span className="bronze-text">Bronze:</span> 0-10 Stars</li>
                                            </ul>
                                        </div>
                                    )}
                                </span>
                            </div>
                        </div>
                    </div>
                )}

                {/* 📦 TASKS */}
                <h3 className="section-title">My Assigned Tasks</h3>

                {requests.length === 0 ? (
                    <div className="empty-state">
                        <h4 className="empty-title">No active assignments</h4>
                        <p className="empty-subtitle">
                            You’re currently available. New requests will appear here once assigned.
                        </p>
                    </div>
                ) : (
                    <div className="task-grid">
                        {requests.map(r => (
                            <div className="task-card" key={r.id}>
                                <div className="task-top">
                                    <span className="room">{r.classRoom}</span>
                                    <span className="badge">ASSIGNED</span>
                                </div>
                                <p>Requested by <b>{r.professorName}</b></p>
                                <div className="items">
                                    {r.items.map((i, idx) => (
                                        <span key={idx} className="chip">{i}</span>
                                    ))}
                                </div>
                                <p className={`sla ${slaText(r.deliveryDeadline).includes("BREACHED") ? "danger" : "safe"}`}>
                                    SLA Remaining: {slaText(r.deliveryDeadline)}
                                </p>
                                <button onClick={() => markDelivered(r.id)}>
                                    Mark Delivered
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}