import { useEffect, useState, useRef } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import "./StaffDashboard.css";

export default function StaffDashboard() {
    const [requests, setRequests] = useState([]);
    const [staff, setStaff] = useState(null);
    const [loading, setLoading] = useState(true);
    const [now, setNow] = useState(Date.now());
    const [showLevels, setShowLevels] = useState(false);
    const [activeTab, setActiveTab] = useState("tasks");
    const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
    const [notifications, setNotifications] = useState([]);
    const [showNotificationPanel, setShowNotificationPanel] = useState(false);

    // WebSocket refs
    const stompClientRef = useRef(null);
    const notificationIdsRef = useRef(new Set());

    const token = localStorage.getItem("token");
    const navigate = useNavigate();

    const addNotification = (msg, type = 'info') => {
        const notificationId = `notification-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

        setNotifications(prev => {
            const normalizedMsg = msg.toLowerCase().trim();
            const exists = prev.some(n => n.msg.toLowerCase().trim() === normalizedMsg);
            if (exists) return prev;

            return [...prev, {
                id: notificationId,
                msg,
                type,
                timestamp: Date.now()
            }];
        });

        setTimeout(() => {
            setNotifications(prev => prev.filter(n => n.id !== notificationId));
        }, 5000);
    };

    const removeNotification = (id) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
    };

    const clearAllNotifications = () => {
        setNotifications([]);
    };

    const fetchHistory = async () => {
        try {
            const res = await axios.get("http://localhost:8080/api/notifications/staff/history", {
                headers: { Authorization: `Bearer ${token}` }
            });
            const history = res.data.map(n => ({
                id: n.id,
                msg: n.message,
                type: n.type
            }));
            setNotifications(history);
        } catch (err) {
            console.error("Failed to load notification history", err);
        }
    };

    useEffect(() => {
        fetchAssignedRequests();
        fetchProfile();
    }, []);

    useEffect(() => {
        const i = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(i);
    }, []);

    /* ===================== WEBSOCKET ===================== */
    useEffect(() => {
        if (stompClientRef.current && stompClientRef.current.connected) {
            stompClientRef.current.disconnect();
        }

        const socket = new SockJS('http://localhost:8080/ws-notifications');
        const stompClient = Stomp.over(socket);
        stompClientRef.current = stompClient;
        stompClient.debug = null;

        stompClient.connect(
            { Authorization: `Bearer ${token}` },
            () => {
                console.log("WebSocket connected for staff");

                stompClient.subscribe('/user/queue/notifications', (message) => {
                    try {
                        const data = JSON.parse(message.body);

                        // 🔒 ONLY process assignment events
                        if (data.event !== "TASK_ASSIGNED") return;

                        const notificationKey = `${data.event}-${data.requestId}`;

                        if (notificationIdsRef.current.has(notificationKey)) {
                            console.log("Duplicate notification skipped:", notificationKey);
                            return;
                        }

                        notificationIdsRef.current.add(notificationKey);

                        addNotification(data.message, data.type || 'info');
                        fetchAssignedRequests();

                        setTimeout(() => {
                            notificationIdsRef.current.delete(notificationKey);
                        }, 30000);

                    } catch (e) {
                        console.error("Error parsing notification:", e);
                    }
                });
            },
            (error) => {
                console.error("WebSocket connection error:", error);
                addNotification("Connection lost. Reconnecting...", "error");
            }
        );

        return () => {
            if (stompClientRef.current && stompClientRef.current.connected) {
                stompClientRef.current.disconnect();
                console.log("WebSocket disconnected");
            }
        };
    }, [token]);

    /* ===================== API ===================== */

    const fetchAssignedRequests = async () => {
        try {
            const res = await axios.get(
                "http://localhost:8080/api/staff/amenities/my",
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setRequests(res.data);
        } catch (err) {
            console.error("Failed to fetch requests", err);
        } finally {
            setLoading(false);
        }
    };

    const fetchProfile = async () => {
        try {
            const res = await axios.get(
                "http://localhost:8080/api/staff/amenities/me",
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setStaff(res.data);
        } catch (err) {
            console.error("Failed to fetch profile", err);
        }
    };

    const markDelivered = async (id, deadline) => {
        try {
            const isBreached = new Date(deadline).getTime() < Date.now();

            await axios.put(
                `http://localhost:8080/api/staff/amenities/${id}/delivered`,
                {},
                {
                    headers: { Authorization: `Bearer ${token}` },
                    params: { slaBreached: isBreached }
                }
            );

            if (isBreached) {
                addNotification("SLA Breached! Star count decreased by 1.", "warning");
            } else {
                addNotification("Task marked as delivered successfully!", "success");
            }

            fetchAssignedRequests();
            fetchProfile();
        } catch (err) {
            console.error("Failed to mark as delivered", err);
            addNotification("Failed to mark as delivered. Please try again.", "error");
        }
    };

    const handleLogout = async () => {
        try {
            await axios.post(
                "http://localhost:8080/api/staff/auth/logout",
                {},
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                }
            );
        } finally {
            localStorage.removeItem("token");
            localStorage.removeItem("role");
            localStorage.removeItem("staffId");
            navigate("/staff/login");
        }
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

    const NotificationToasts = () => (
        <div className="staff-toast-container">
            {notifications.map(n => (
                <div
                    key={n.id}
                    className={`staff-toast ${n.type || 'info'}`}
                    style={{ animation: 'toastSlideIn 0.3s ease-out' }}
                >
                    <div className="staff-toast-content">
                        <p>{n.msg}</p>
                        <div className="staff-toast-progress">
                            <div
                                className="staff-toast-progress-bar"
                                style={{ animation: 'toastProgress 5s linear forwards' }}
                            />
                        </div>
                    </div>
                    <button
                        className="staff-toast-close"
                        onClick={() => removeNotification(n.id)}
                        aria-label="Dismiss notification"
                    >
                        ✕
                    </button>
                </div>
            ))}
        </div>
    );

    if (loading) {
        return (
            <div className="staff-loading-screen">
                <div className="staff-loading-spinner"></div>
                <p>Loading dashboard...</p>
            </div>
        );
    }

    return (
        <div className="staff-dashboard">
            <NotificationToasts />

            <div className="staff-sidebar">
                <h2 className="staff-sidebar-logo">ProfMojo</h2>

                <nav>
                    <button
                        className={`staff-nav-item ${activeTab === "tasks" ? "active" : ""}`}
                        onClick={() => setActiveTab("tasks")}
                    >
                        My Tasks
                    </button>
                    <button
                        className={`staff-nav-item ${activeTab === "profile" ? "active" : ""}`}
                        onClick={() => setActiveTab("profile")}
                    >
                        My Profile
                    </button>
                </nav>

                <div className="staff-sidebar-footer">
                    {staff && (
                        <div className="staff-info-card">
                            <div className="staff-avatar">
                                {staff.name.charAt(0).toUpperCase()}
                            </div>
                            <div className="staff-details">
                                <strong>{staff.name}</strong>
                                <span>Staff ID: {staff.staffId || "N/A"}</span>
                            </div>
                        </div>
                    )}
                    <button
                        className="staff-logout-btn"
                        onClick={() => setShowLogoutConfirm(true)}
                    >
                        <span>Logout</span>
                    </button>
                </div>
            </div>

            <div className="staff-main-area">
                {activeTab === "tasks" && (
                    <>
                        <div className="staff-header">
                            <h1>My Assigned Tasks</h1>
                        </div>

                        <div className="staff-stats-summary">
                            <div className="staff-stat-card">
                                <div className="staff-stat-icon">⭐</div>
                                <div className="staff-stat-content">
                                    <strong>Stars Earned</strong>
                                    <span>{staff?.stars || 0}</span>
                                </div>
                            </div>
                            <div className="staff-stat-card">
                                <div className="staff-stat-icon">📦</div>
                                <div className="staff-stat-content">
                                    <strong>Total Deliveries</strong>
                                    <span>{staff?.totalDeliveries || 0}</span>
                                </div>
                            </div>
                            <div className="staff-stat-card">
                                <div className="staff-stat-icon">🏅</div>
                                <div className="staff-stat-content">
                                    <strong>Service Level</strong>
                                    <span className={`staff-level-badge ${level}`}>
                                        {levelLabel}
                                        <button
                                            className="staff-info-btn"
                                            onClick={() => setShowLevels(!showLevels)}
                                            title="View levels info"
                                            onMouseEnter={() => setShowLevels(true)}
                                            onMouseLeave={() => setShowLevels(false)}
                                        >
                                            i
                                        </button>
                                    </span>
                                    {showLevels && (
                                        <div className="staff-levels-popup">
                                            <h4>Level Requirements</h4>
                                            <ul>
                                                <li><span className="gold-text">Gold:</span> 20+ Stars</li>
                                                <li><span className="silver-text">Silver:</span> 10-20 Stars</li>
                                                <li><span className="bronze-text">Bronze:</span> 0-10 Stars</li>
                                            </ul>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>

                        {requests.length === 0 ? (
                            <div className="staff-empty-state">
                                <div className="staff-empty-icon">📋</div>
                                <h3>No Active Assignments</h3>
                                <p>You're currently available. New requests will appear here once assigned.</p>
                            </div>
                        ) : (
                            <div className="staff-task-grid">
                                {requests.map(r => (
                                    <div className="staff-task-card" key={r.id}>
                                        <div className="staff-task-header">
                                            <div className="staff-room-info">
                                                <h4 className="staff-room-name">{r.classRoom}</h4>
                                                <span className="staff-badge">ASSIGNED</span>
                                            </div>
                                            <span className={`staff-sla-indicator ${slaText(r.slaDeadline).includes("BREACHED") ? "danger" : "safe"}`}>
                                                {slaText(r.slaDeadline)}
                                            </span>
                                        </div>

                                        <div className="staff-task-details">
                                            <p className="staff-requested-by">
                                                Requested by <strong>{r.professorName}</strong>
                                            </p>

                                            {r.message && (
                                                <p className="staff-request-message">
                                                    "{r.message}"
                                                </p>
                                            )}

                                            <div className="staff-items-list">
                                                <span className="staff-items-label">Items needed:</span>
                                                <div className="staff-items-container">
                                                    {r.items.map((i, idx) => (
                                                        <span key={idx} className="staff-item-chip">
                                                            {i}
                                                        </span>
                                                    ))}
                                                </div>
                                            </div>
                                        </div>

                                        <button
                                            className="staff-deliver-btn"
                                            onClick={() => markDelivered(r.id, r.slaDeadline)}
                                        >
                                            Mark as Delivered
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </>
                )}

                {activeTab === "profile" && staff && (
                    <div className="staff-profile-section">
                        <div className="staff-header">
                            <h1>My Profile</h1>
                        </div>

                        <div className="staff-profile-card">
                            <div className="staff-profile-header">
                                <div className="staff-profile-avatar-large">
                                    {staff.name.charAt(0).toUpperCase()}
                                </div>
                                <div className="staff-profile-title">
                                    <h2>{staff.name}</h2>
                                    <p className="staff-profile-role">Staff Member</p>
                                </div>
                            </div>

                            <div className="staff-profile-details">
                                <div className="staff-detail-row">
                                    <span className="staff-detail-label">Staff ID</span>
                                    <span className="staff-detail-value">{staff.staffId || "Not specified"}</span>
                                </div>
                                <div className="staff-detail-row">
                                    <span className="staff-detail-label">Status</span>
                                    <span className={`staff-detail-value ${staff.online ? "online" : "offline"}`}>
                                        {staff.online ? "Available" : "Unavailable"}
                                    </span>
                                </div>
                                <div className="staff-detail-row">
                                    <span className="staff-detail-label">Stars Earned</span>
                                    <span className="staff-detail-value stars">{staff.stars || 0}</span>
                                </div>
                                <div className="staff-detail-row">
                                    <span className="staff-detail-label">Total Deliveries</span>
                                    <span className="staff-detail-value">{staff.totalDeliveries || 0}</span>
                                </div>
                                <div className="staff-detail-row">
                                    <span className="staff-detail-label">Service Level</span>
                                    <span className={`staff-detail-value level ${level}`}>
                                        {levelLabel}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {showLogoutConfirm && (
                    <div className="staff-modal-overlay">
                        <div className="staff-modal">
                            <h2>Log out?</h2>
                            <p>You will be signed out of your staff account and marked as unavailable.</p>
                            <div className="staff-modal-actions">
                                <button
                                    className="staff-modal-cancel"
                                    onClick={() => setShowLogoutConfirm(false)}
                                >
                                    Cancel
                                </button>
                                <button
                                    className="staff-modal-confirm"
                                    onClick={handleLogout}
                                >
                                    Yes, Logout
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
