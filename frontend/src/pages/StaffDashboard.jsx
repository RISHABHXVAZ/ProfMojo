import { useEffect, useState, useRef } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { Bell, X, CheckCircle, AlertCircle, Key } from "lucide-react";
import "./StaffDashboard.css";

export default function StaffDashboard() {
    const [requests, setRequests] = useState([]);
    const [staff, setStaff] = useState(null);
    const [loading, setLoading] = useState(true);
    const [now, setNow] = useState(Date.now());
    const [showLevels, setShowLevels] = useState(false);
    const [activeTab, setActiveTab] = useState("tasks");
    const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
    
    // Notification state
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [showNotificationPanel, setShowNotificationPanel] = useState(false);
    
    // NEW: Confirmation code modal state
    const [showConfirmationModal, setShowConfirmationModal] = useState(false);
    const [selectedRequest, setSelectedRequest] = useState(null);
    const [confirmationCode, setConfirmationCode] = useState("");
    const [confirming, setConfirming] = useState(false);

    const token = localStorage.getItem("token");
    const navigate = useNavigate();

    // Fetch notifications from backend
    const fetchNotifications = async () => {
    try {
        const res = await axios.get("http://localhost:8080/api/notifications/staff", {
            headers: { Authorization: `Bearer ${token}` }
        });
        setNotifications(res.data.notifications || []);
        setUnreadCount(res.data.unreadCount || 0);
    } catch (err) {
        console.error("Failed to fetch notifications", err);
    }
};

    // Add this useEffect to close notification panel when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (showNotificationPanel && !event.target.closest('.staff-notification-center')) {
                setShowNotificationPanel(false);
            }
        };

        document.addEventListener('click', handleClickOutside);
        
        return () => {
            document.removeEventListener('click', handleClickOutside);
        };
    }, [showNotificationPanel]);

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

    // UPDATED: Open confirmation modal instead of directly marking delivered
    const openConfirmationModal = (request) => {
        setSelectedRequest(request);
        setConfirmationCode("");
        setShowConfirmationModal(true);
    };

    // UPDATED: Mark delivered with confirmation code
    const markDeliveredWithConfirmation = async () => {
        if (!selectedRequest || !confirmationCode.trim()) {
            alert("Please enter the confirmation code");
            return;
        }

        if (confirmationCode.length !== 4) {
            alert("Confirmation code must be 4 digits");
            return;
        }

        setConfirming(true);
        try {
            const isBreached = new Date(selectedRequest.slaDeadline).getTime() < Date.now();

            const res = await axios.put(
                `http://localhost:8080/api/staff/amenities/${selectedRequest.id}/delivered`,
                {},
                {
                    headers: { Authorization: `Bearer ${token}` },
                    params: { 
                        confirmationCode: confirmationCode.trim(),
                        slaBreached: isBreached
                    }
                }
            );

            // Show success message
            if (res.data.message) {
                alert(res.data.message);
            }

            // Refresh data
            fetchAssignedRequests();
            fetchProfile();
            fetchNotifications();
            
            // Close modal
            setShowConfirmationModal(false);
            setSelectedRequest(null);
            setConfirmationCode("");
            
        } catch (err) {
            console.error("Failed to mark as delivered", err);
            if (err.response?.data?.error) {
                alert(`Error: ${err.response.data.error}`);
            } else {
                alert("Failed to mark as delivered. Please try again.");
            }
        } finally {
            setConfirming(false);
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

    // Notification Panel Component (Read-only)
    const NotificationPanel = () => (
        <div 
            className="staff-notification-panel"
            onClick={(e) => e.stopPropagation()}
        >
            <div className="staff-notification-header">
                <h3>Notifications {unreadCount > 0 && `(${unreadCount})`}</h3>
                <button 
                    className="staff-notification-close-btn" 
                    onClick={(e) => {
                        e.stopPropagation();
                        setShowNotificationPanel(false);
                    }}
                >
                    <X size={18} />
                </button>
            </div>

            <div className="staff-notification-list">
                {notifications.length === 0 ? (
                    <div className="staff-notification-empty">
                        <Bell size={32} />
                        <p>No notifications</p>
                        <span>All caught up!</span>
                    </div>
                ) : (
                    notifications.map(notification => (
                        <div 
                            key={notification.id} 
                            className={`staff-notification-item ${notification.type}`}
                        >
                            <div className="staff-notification-icon">
                                {notification.type === 'success' && <CheckCircle size={18} />}
                                {notification.type === 'error' && <AlertCircle size={18} />}
                                {notification.type === 'warning' && <AlertCircle size={18} />}
                                {notification.type === 'info' && <Bell size={18} />}
                            </div>
                            <div className="staff-notification-content">
                                <p className="staff-notification-message">{notification.message || notification.msg}</p>
                                <span className="staff-notification-time">
                                    {notification.createdAt ? 
                                        new Date(notification.createdAt).toLocaleTimeString([], { 
                                            hour: '2-digit', 
                                            minute: '2-digit' 
                                        }) : 
                                        "Recently"
                                    }
                                </span>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );

    useEffect(() => {
        fetchAssignedRequests();
        fetchProfile();
        fetchNotifications();
        
        // Set up polling for notifications
        const pollInterval = setInterval(() => {
            fetchNotifications();
            fetchAssignedRequests();
        }, 10000); // Poll every 10 seconds
        
        return () => clearInterval(pollInterval);
    }, []);

    useEffect(() => {
        const i = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(i);
    }, []);

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
            {/* Notification Area */}
            <div className="staff-notification-center">
                <div 
                    className="staff-notification-bell" 
                    onClick={(e) => {
                        e.stopPropagation();
                        setShowNotificationPanel(!showNotificationPanel);
                    }}
                >
                    <Bell size={22} />
                    {unreadCount > 0 && (
                        <span className="staff-notification-counter">{unreadCount}</span>
                    )}
                </div>

                {showNotificationPanel && (
                    <div 
                        className="staff-notification-panel-wrapper"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <NotificationPanel />
                    </div>
                )}
            </div>

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

                                        {/* UPDATED: Open confirmation modal */}
                                        <button
                                            className="staff-deliver-btn"
                                            onClick={() => openConfirmationModal(r)}
                                        >
                                            <Key size={16} style={{ marginRight: '8px' }} />
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

                {/* NEW: Confirmation Code Modal */}
                {showConfirmationModal && selectedRequest && (
                    <div className="staff-modal-overlay">
                        <div className="staff-modal confirmation-modal">
                            <div className="modal-header">
                                <h2><Key size={20} /> Confirm Delivery</h2>
                                <button 
                                    className="close-btn" 
                                    onClick={() => {
                                        setShowConfirmationModal(false);
                                        setSelectedRequest(null);
                                        setConfirmationCode("");
                                    }}
                                >
                                    <X size={18} />
                                </button>
                            </div>
                            
                            <div className="modal-content">
                                <div className="confirmation-info">
                                    <p><strong>Room:</strong> {selectedRequest.classRoom}</p>
                                    <p><strong>Professor:</strong> {selectedRequest.professorName}</p>
                                    <p><strong>Items:</strong> {selectedRequest.items?.join(", ")}</p>
                                    <p><strong>SLA Status:</strong> <span className={slaText(selectedRequest.slaDeadline).includes("BREACHED") ? "danger-text" : "safe-text"}>
                                        {slaText(selectedRequest.slaDeadline)}
                                    </span></p>
                                </div>
                                
                                <div className="form-group">
                                    <label htmlFor="confirmationCode">
                                        <Key size={16} /> Enter 4-digit confirmation code from professor:
                                    </label>
                                    <input
                                        type="text"
                                        id="confirmationCode"
                                        value={confirmationCode}
                                        onChange={(e) => {
                                            // Only allow numbers and limit to 4 digits
                                            const value = e.target.value.replace(/\D/g, '').slice(0, 4);
                                            setConfirmationCode(value);
                                        }}
                                        placeholder="0000"
                                        maxLength={4}
                                        className="confirmation-input"
                                        autoFocus
                                    />
                                    <small className="hint">
                                        Get this code from the professor after delivering items to {selectedRequest.classRoom}
                                    </small>
                                </div>
                                
                                <div className="modal-actions">
                                    <button 
                                        className="cancel-btn" 
                                        onClick={() => {
                                            setShowConfirmationModal(false);
                                            setSelectedRequest(null);
                                            setConfirmationCode("");
                                        }}
                                        disabled={confirming}
                                    >
                                        Cancel
                                    </button>
                                    <button 
                                        className={`confirm-btn ${confirming ? "loading" : ""}`}
                                        onClick={markDeliveredWithConfirmation}
                                        disabled={confirming || confirmationCode.length !== 4}
                                    >
                                        {confirming ? "Confirming..." : "Confirm Delivery"}
                                    </button>
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