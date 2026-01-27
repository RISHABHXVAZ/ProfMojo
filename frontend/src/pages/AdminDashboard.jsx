import { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import {
    Users,
    Clock,
    CheckCircle,
    AlertCircle,
    UserCheck,
    Shield,
    Package,
    BarChart3,
    LogOut,
    Search,
    Eye,
    RefreshCw,
    X,
    FileText,
    Wifi,
    WifiOff,
    Star,
    Bell
} from "lucide-react";
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer
} from 'recharts';
import "./AdminDashboard.css";
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

export default function AdminDashboard() {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [activeTab, setActiveTab] = useState("pending");
    const [notifications, setNotifications] = useState([]);

    const [showStaffModal, setShowStaffModal] = useState(false);
    const [selectedRequestId, setSelectedRequestId] = useState(null);
    const [selectedRequest, setSelectedRequest] = useState(null);

    const [staffList, setStaffList] = useState([]);
    const [staffLoading, setStaffLoading] = useState(false);

    const [allStaff, setAllStaff] = useState([]);
    const [staffStatsLoading, setStaffStatsLoading] = useState(true);
    const [ongoingRequests, setOngoingRequests] = useState([]);
    const [completedRequests, setCompletedRequests] = useState([]);
    const [now, setNow] = useState(Date.now());
    const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
    const [searchQuery, setSearchQuery] = useState("");
    const [showNotificationPanel, setShowNotificationPanel] = useState(false);

    const [chartData, setChartData] = useState({
        staffRatings: [],
        deliveryDistribution: []
    });

    const COLORS = ['#8b5cf6', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#06b6d4', '#84cc16'];

    const [adminId, setAdminId] = useState(localStorage.getItem("adminId") || "");
    const [adminDepartment, setAdminDepartment] = useState(extractDepartmentFromAdminId(localStorage.getItem("adminId") || ""));

    const [stats, setStats] = useState({
        totalPending: 0,
        totalOngoing: 0,
        totalCompleted: 0,
        totalStaff: 0,
        onlineStaff: 0
    });

    const token = localStorage.getItem("token");
    const navigate = useNavigate();

    function extractDepartmentFromAdminId(id) {
        if (!id) return "";
        const parts = id.split('-');
        if (parts.length >= 2) {
            return parts[1];
        }
        return "";
    }

    const fetchNotificationHistory = async () => {
        try {
            const res = await axios.get("http://localhost:8080/api/notifications/admin/history", {
                headers: { Authorization: `Bearer ${token}` },
                params: { department: adminDepartment }
            });

            const formattedHistory = res.data.map(n => ({
                id: n.id,
                msg: n.message,
                type: n.type,
                timestamp: new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            }));

            setNotifications(formattedHistory);
        } catch (err) {
            console.error("Failed to load notification history", err);
        }
    };

    const addNotification = (msg, type = 'info') => {
        const id = Date.now() + Math.random();
        const notification = {
            id,
            msg,
            type,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
        };

        setNotifications(prev => [notification, ...prev].slice(0, 5));

        setTimeout(() => {
            removeNotification(id);
        }, type === 'success' ? 4000 : 6000);
    };

    const removeNotification = (id) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
    };

    const clearAllNotifications = () => {
        setNotifications([]);
    };

    useEffect(() => {
        fetchNotificationHistory();

        const socket = new SockJS('http://localhost:8080/ws-notifications');
        const stompClient = Stomp.over(socket);

        stompClient.connect({ Authorization: `Bearer ${token}` }, () => {
            stompClient.subscribe('/topic/admin-notifications', (message) => {
                try {
                    const data = JSON.parse(message.body);
                    if (!data.department || data.department === adminDepartment) {
                        addNotification(data.message || data.msg, data.type || 'info');
                        fetchAllData();
                    }
                } catch (e) { console.error("Parse error", e); }
            });
        });

        return () => { if (stompClient.connected) stompClient.disconnect(); };
    }, [token, adminDepartment]);

    const generateChartData = () => {
        if (allStaff.length === 0) return;

        const ratingsData = allStaff
            .map(staff => ({
                name: staff.name?.split(' ')[0] || staff.staffId?.slice(-4) || 'Staff',
                stars: staff.stars || staff.rating || 0,
                deliveries: staff.totalDeliveries || staff.completedTasks || 0,
                fullName: staff.name || 'Staff Member'
            }))
            .sort((a, b) => b.stars - a.stars)
            .slice(0, 8);

        const deliveryData = allStaff
            .filter(staff => (staff.totalDeliveries || staff.completedTasks || 0) > 0)
            .map(staff => ({
                name: staff.name?.split(' ')[0] || staff.staffId?.slice(-4) || 'Staff',
                value: staff.totalDeliveries || staff.completedTasks || 0,
                department: staff.department || 'General',
                fullName: staff.name || 'Staff Member'
            }))
            .sort((a, b) => b.value - a.value)
            .slice(0, 6);

        setChartData({
            staffRatings: ratingsData,
            deliveryDistribution: deliveryData
        });
    };

    const formatRemaining = (deadline) => {
        if (!deadline) return "—";
        const diff = new Date(deadline).getTime() - now;
        if (diff <= 0) return "SLA BREACHED";
        const m = Math.floor(diff / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        if (m > 60) {
            const h = Math.floor(m / 60);
            const remainingM = m % 60;
            return `${h}h ${remainingM}m`;
        }
        return `${m}m ${s}s`;
    };

    const fetchAllData = async () => {
        try {
            setLoading(true);
            await Promise.all([
                fetchPendingRequests(),
                fetchOngoingRequests(),
                fetchCompletedRequests(),
                fetchAllStaff()
            ]);
        } catch (err) {
            console.error("Failed to fetch data", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        calculateStats();
        generateChartData();
    }, [requests, ongoingRequests, completedRequests, allStaff]);

    const fetchPendingRequests = async () => {
        const res = await axios.get(
            "http://localhost:8080/api/admin/amenities/pending",
            {
                headers: { Authorization: `Bearer ${token}` },
                params: { department: adminDepartment }
            }
        );
        setRequests(res.data);
    };

    const fetchOngoingRequests = async () => {
        const res = await axios.get(
            "http://localhost:8080/api/admin/amenities/ongoing",
            {
                headers: { Authorization: `Bearer ${token}` },
                params: { department: adminDepartment }
            }
        );
        setOngoingRequests(res.data);
    };

    const fetchCompletedRequests = async () => {
        const res = await axios.get(
            "http://localhost:8080/api/admin/amenities/completed",
            {
                headers: { Authorization: `Bearer ${token}` },
                params: { department: adminDepartment }
            }
        );
        setCompletedRequests(res.data);
    };

    const fetchAllStaff = async () => {
        try {
            const res = await axios.get(
                "http://localhost:8080/api/admin/amenities/staff/all",
                {
                    headers: { Authorization: `Bearer ${token}` },
                    params: { department: adminDepartment }
                }
            );
            setAllStaff(res.data);
            generateChartData();
        } catch (err) {
            console.error("Failed to load staff list", err);
        } finally {
            setStaffStatsLoading(false);
        }
    };

    const fetchStaff = async (requestId) => {
        setStaffLoading(true);
        try {
            const res = await axios.get(
                "http://localhost:8080/api/admin/amenities/staff/available",
                {
                    headers: { Authorization: `Bearer ${token}` },
                    params: { department: adminDepartment }
                }
            );
            setStaffList(res.data);
            const req = requests.find(r => r.id === requestId) ||
                ongoingRequests.find(r => r.id === requestId) ||
                completedRequests.find(r => r.id === requestId);
            setSelectedRequest(req);
        } catch {
            console.error("Failed to load staff");
        } finally {
            setStaffLoading(false);
        }
    };

    const assignStaff = async (staffId) => {
        try {
            await axios.put(
                `http://localhost:8080/api/admin/amenities/${selectedRequestId}/assign/${staffId}`,
                {},
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setShowStaffModal(false);
            fetchAllData();
        } catch {
            console.error("Assignment failed");
        }
    };

    const viewRequestDetails = (request) => {
        const details = `
Request Details:
----------------
Request ID: #${request.id || request.requestId}
Classroom: ${request.classRoom}
Professor: ${request.professorName}
Department: ${request.department}
Status: ${getStatusText(request.status)}
Items: ${request.items?.join(", ") || "No items specified"}
${request.message ? `Message: ${request.message}` : ""}
${request.assignedStaff ? `Assigned To: ${request.assignedStaff.name} (${request.assignedStaff.staffId || "N/A"})` : ""}
${request.createdAt ? `Requested: ${new Date(request.createdAt).toLocaleString()}` : ""}
${request.deliveryDeadline ? `Delivery Deadline: ${new Date(request.deliveryDeadline).toLocaleString()}` : ""}
        `.trim();
        alert(details);
    };

    const calculateStats = () => {
        const onlineStaff = allStaff.filter(staff => staff.online).length;
        setStats({
            totalPending: requests.length,
            totalOngoing: ongoingRequests.length,
            totalCompleted: completedRequests.length,
            totalStaff: allStaff.length,
            onlineStaff: onlineStaff
        });
    };

    const handleLogout = () => {
        localStorage.clear();
        navigate("/admin/login");
    };

    const getStatusColor = (status) => {
        switch (status?.toLowerCase()) {
            case 'pending': return '#f59e0b';
            case 'assigned': return '#3b82f6';
            case 'delivered': return '#10b981';
            default: return '#64748b';
        }
    };

    const getStatusText = (status) => {
        switch (status?.toLowerCase()) {
            case 'pending': return 'Pending';
            case 'assigned': return 'Assigned';
            case 'delivered': return 'Delivered';
            default: return status || 'Unknown';
        }
    };

    const formatLastActive = (timestamp) => {
        if (!timestamp) return "Unknown";
        const lastActive = new Date(timestamp).getTime();
        const diff = now - lastActive;
        if (diff < 60000) return "Just now";
        if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
        if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
        return `${Math.floor(diff / 86400000)}d ago`;
    };

    useEffect(() => {
        const interval = setInterval(() => {
            setNow(Date.now());
        }, 1000);
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        fetchAllData();
        const pollInterval = setInterval(() => {
            fetchAllData();
        }, 10000);
        return () => clearInterval(pollInterval);
    }, []);

    const getFilteredRequests = () => {
        let filtered = [];
        switch (activeTab) {
            case "pending": filtered = requests; break;
            case "ongoing": filtered = ongoingRequests; break;
            case "completed": filtered = completedRequests; break;
            default: filtered = requests;
        }

        if (searchQuery) {
            filtered = filtered.filter(req =>
                req.classRoom?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                req.professorName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                req.status?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                (req.assignedStaff?.name?.toLowerCase().includes(searchQuery.toLowerCase()))
            );
        }
        return filtered;
    };

    if (loading) {
        return (
            <div className="admin-loading-screen">
                <div className="admin-loading-spinner"></div>
                <p>Loading admin dashboard...</p>
            </div>
        );
    }

    return (
        <div className="admin-dashboard">
            <div className="admin-notification-center">
                <div className="admin-notification-bell" onClick={() => setShowNotificationPanel(!showNotificationPanel)}>
                    <Bell size={22} />
                    {notifications.length > 0 && (
                        <span className="admin-notification-counter">{notifications.length}</span>
                    )}
                </div>

                {showNotificationPanel && (
                    <div className="admin-notification-panel">
                        <div className="admin-notification-header">
                            <h3>Notifications</h3>
                            <div className="admin-notification-actions">
                                <button className="admin-notification-clear-btn" onClick={clearAllNotifications} disabled={notifications.length === 0}>Clear All</button>
                                <button className="admin-notification-close-btn" onClick={() => setShowNotificationPanel(false)}><X size={18} /></button>
                            </div>
                        </div>

                        <div className="admin-notification-list">
                            {notifications.length === 0 ? (
                                <div className="admin-notification-empty">
                                    <Bell size={32} />
                                    <p>No notifications</p>
                                    <span>All caught up!</span>
                                </div>
                            ) : (
                                notifications.map(notification => (
                                    <div key={notification.id} className={`admin-notification-item ${notification.type}`} onClick={() => removeNotification(notification.id)}>
                                        <div className="admin-notification-icon">
                                            {notification.type === 'success' && <CheckCircle size={18} />}
                                            {notification.type === 'error' && <AlertCircle size={18} />}
                                            {notification.type === 'warning' && <AlertCircle size={18} />}
                                            {notification.type === 'info' && <Bell size={18} />}
                                        </div>
                                        <div className="admin-notification-content">
                                            <p className="admin-notification-message">{notification.msg}</p>
                                            <span className="admin-notification-time">{notification.timestamp}</span>
                                        </div>
                                        <button className="admin-notification-dismiss" onClick={(e) => { e.stopPropagation(); removeNotification(notification.id); }}><X size={14} /></button>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                )}
            </div>

            <div className="admin-sidebar">
                <div className="admin-sidebar-header">
                    <div className="admin-logo">
                        <Shield size={32} />
                        <h2>ProfMojo</h2>
                    </div>
                    <p className="admin-role">{adminDepartment} Department Admin</p>
                </div>

                <nav className="admin-nav">
                    <button className={`admin-nav-item ${activeTab === "dashboard" ? "active" : ""}`} onClick={() => setActiveTab("dashboard")}><BarChart3 size={20} /><span>Dashboard</span></button>
                    <button className={`admin-nav-item ${activeTab === "pending" ? "active" : ""}`} onClick={() => setActiveTab("pending")}><AlertCircle size={20} /><span>Pending Requests</span> {stats.totalPending > 0 && <span className="admin-nav-badge">{stats.totalPending}</span>}</button>
                    <button className={`admin-nav-item ${activeTab === "ongoing" ? "active" : ""}`} onClick={() => setActiveTab("ongoing")}><Clock size={20} /><span>Ongoing Tasks</span> {stats.totalOngoing > 0 && <span className="admin-nav-badge ongoing">{stats.totalOngoing}</span>}</button>
                    <button className={`admin-nav-item ${activeTab === "completed" ? "active" : ""}`} onClick={() => setActiveTab("completed")}><CheckCircle size={20} /><span>Completed</span> {stats.totalCompleted > 0 && <span className="admin-nav-badge completed">{stats.totalCompleted}</span>}</button>
                    <button className={`admin-nav-item ${activeTab === "staff" ? "active" : ""}`} onClick={() => setActiveTab("staff")}><Users size={20} /><span>Staff</span></button>
                </nav>

                <div className="admin-sidebar-footer">
                    <div className="admin-user-info">
                        <div className="admin-avatar"><Shield size={24} /></div>
                        <div className="admin-user-details"><strong>{adminId}</strong><span>{adminDepartment} Admin</span></div>
                    </div>
                    <button className="admin-logout-btn" onClick={() => setShowLogoutConfirm(true)}><LogOut size={18} /><span>Logout</span></button>
                </div>
            </div>

            <div className="admin-main">
                <header className="admin-header">
                    <div className="admin-header-left">
                        <h1 className="admin-title">
                            {activeTab === "dashboard" && "Dashboard Overview"}
                            {activeTab === "pending" && "Pending Requests"}
                            {activeTab === "ongoing" && "Ongoing Tasks"}
                            {activeTab === "completed" && "Completed Requests"}
                            {activeTab === "staff" && "Staff Management"}
                        </h1>
                        <p className="admin-subtitle">{adminDepartment} Department • Manage amenity requests</p>
                    </div>
                    <div className="admin-header-right">
                        <div className="admin-search"><Search size={18} /><input type="text" placeholder="Search requests..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} onKeyPress={(e) => e.key === 'Enter' && fetchAllData()} /></div>
                        <button className="admin-refresh-btn" onClick={fetchAllData} title="Refresh data"><RefreshCw size={18} /></button>
                    </div>
                </header>

                {activeTab === "dashboard" && (
                    <>
                        <div className="admin-stats-grid">
                            <div className="admin-stat-card purple"><div className="admin-stat-icon"><AlertCircle size={24} /></div><div className="admin-stat-content"><h3>{stats.totalPending}</h3><p>Pending Requests</p></div></div>
                            <div className="admin-stat-card blue"><div className="admin-stat-icon"><Clock size={24} /></div><div className="admin-stat-content"><h3>{stats.totalOngoing}</h3><p>Ongoing Tasks</p></div></div>
                            <div className="admin-stat-card green"><div className="admin-stat-icon"><CheckCircle size={24} /></div><div className="admin-stat-content"><h3>{stats.totalCompleted}</h3><p>Completed</p></div></div>
                            <div className="admin-stat-card orange"><div className="admin-stat-icon"><Users size={24} /></div><div className="admin-stat-content"><h3>{stats.onlineStaff}/{stats.totalStaff}</h3><p>Staff Online</p></div></div>
                        </div>

                        <div className="admin-charts-section">
                            <div className="admin-chart-card">
                                <div className="admin-chart-header"><h3 className="admin-chart-title"><Star size={20} className="admin-chart-icon" /> Staff Performance Ratings</h3><p className="admin-chart-subtitle">Top staff by star ratings</p></div>
                                <div className="admin-chart-container">
                                    {chartData.staffRatings.length === 0 ? <div className="admin-chart-empty"><Users size={32} /><p>No staff data available</p></div> :
                                        <ResponsiveContainer width="100%" height={280}><BarChart data={chartData.staffRatings} margin={{ top: 20, right: 30, left: 20, bottom: 60 }}><CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" /><XAxis dataKey="name" angle={-45} textAnchor="end" height={60} tick={{ fill: '#64748b', fontSize: 11 }} /><YAxis tick={{ fill: '#64748b', fontSize: 12 }} label={{ value: 'Stars', angle: -90, position: 'insideLeft', offset: -10, style: { fill: '#64748b' } }} /><Tooltip contentStyle={{ backgroundColor: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} formatter={(value, name) => name === 'stars' ? [value, '⭐ Stars'] : [value, '📦 Deliveries']} labelFormatter={(label, payload) => payload && payload[0] ? payload[0].payload.fullName : label} /><Legend /><Bar dataKey="stars" name="Star Rating" fill="#8b5cf6" radius={[4, 4, 0, 0]} /><Bar dataKey="deliveries" name="Deliveries" fill="#3b82f6" radius={[4, 4, 0, 0]} /></BarChart></ResponsiveContainer>}
                                </div>
                            </div>

                            <div className="admin-chart-card">
                                <div className="admin-chart-header"><h3 className="admin-chart-title"><Package size={20} className="admin-chart-icon" /> Delivery Distribution</h3><p className="admin-chart-subtitle">Top performers by deliveries</p></div>
                                <div className="admin-chart-container">
                                    {chartData.deliveryDistribution.length === 0 ? <div className="admin-chart-empty"><Package size={32} /><p>No delivery data available</p></div> :
                                        <ResponsiveContainer width="100%" height={280}><PieChart><Pie data={chartData.deliveryDistribution} cx="50%" cy="50%" labelLine={true} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`} outerRadius={80} fill="#8884d8" dataKey="value" nameKey="name">{chartData.deliveryDistribution.map((entry, index) => <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />)}</Pie><Tooltip contentStyle={{ backgroundColor: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} formatter={(value, name, props) => props.payload && props.payload.fullName ? [value, props.payload.fullName] : [value, name]} /><Legend /></PieChart></ResponsiveContainer>}
                                </div>
                            </div>
                        </div>
                    </>
                )}

                {activeTab !== "dashboard" && (
                    <div className="admin-content-area">
                        {(activeTab === "pending" || activeTab === "ongoing" || activeTab === "completed") && (
                            <>
                                <div className="admin-content-header"><h2 className="admin-section-title">{activeTab === "pending" && `Pending Requests (${stats.totalPending})`}{activeTab === "ongoing" && `Ongoing Tasks (${stats.totalOngoing})`}{activeTab === "completed" && `Completed Requests (${stats.totalCompleted})`}</h2><div className="admin-search-info">{searchQuery && <span className="admin-search-results">Found {getFilteredRequests().length} results for "{searchQuery}"</span>}</div></div>
                                {error && <div className="admin-error-message"><AlertCircle size={20} /><span>{error}</span><button className="admin-retry-btn" onClick={fetchAllData}>Retry</button></div>}
                                {getFilteredRequests().length === 0 ? <div className="admin-empty-state"><Package size={48} /><h3>No requests found</h3><p>{searchQuery ? "No requests match your search" : activeTab === "pending" ? "All requests have been assigned!" : activeTab === "ongoing" ? "No ongoing tasks at the moment" : "No completed requests yet"}</p></div> :
                                    <div className="admin-requests-grid">
                                        {getFilteredRequests().map(req => (
                                            <div className={`admin-request-card ${req.status?.toLowerCase()}`} key={req.id}>
                                                <div className="admin-card-header">
                                                    <div className="admin-card-badges">
                                                        <span className="admin-department-badge">{req.department}</span>
                                                        <span className="admin-status-badge" style={{ backgroundColor: getStatusColor(req.status), color: req.status?.toLowerCase() === 'pending' ? '#92400e' : (req.status?.toLowerCase() === 'completed' || req.status?.toLowerCase() === 'delivered') ? '#065f46' : '#1e40af' }}>{getStatusText(req.status)}</span>
                                                    </div>
                                                </div>
                                                <div className="admin-card-content">
                                                    <h4 className="admin-classroom"><span className="admin-card-icon">📍</span>{req.classRoom}</h4>
                                                    <p className="admin-request-message">{req.message || "No additional message"}</p>
                                                    <div className="admin-items-list">{req.items?.map((item, idx) => <span key={idx} className="admin-item-tag">{item}</span>)}</div>
                                                    <div className="admin-card-meta">
                                                        <div className="admin-meta-item"><span className="admin-meta-label">Professor:</span><span className="admin-meta-value">{req.professorName}</span></div>
                                                        <div className="admin-meta-item"><span className="admin-meta-label">Request ID:</span><span className="admin-meta-value">#{req.id || req.requestId}</span></div>
                                                        {req.assignedStaff && <div className="admin-meta-item"><span className="admin-meta-label">Assigned To:</span><span className="admin-meta-value staff">{req.assignedStaff.name}{req.assignedStaff.online === false && <span className="staff-offline-badge"> (Offline)</span>}</span></div>}
                                                        {req.createdAt && <div className="admin-meta-item"><span className="admin-meta-label">Requested:</span><span className="admin-meta-value">{new Date(req.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span></div>}
                                                    </div>
                                                    {activeTab === "ongoing" && req.deliveryDeadline && <div className={`admin-sla-indicator ${formatRemaining(req.deliveryDeadline).includes("BREACHED") ? "danger" : "safe"}`}><Clock size={14} /><span>SLA: {formatRemaining(req.deliveryDeadline)}</span></div>}
                                                </div>
                                                <div className="admin-card-footer">
                                                    {activeTab === "pending" && <button className="admin-assign-btn" onClick={() => { setSelectedRequestId(req.id || req.requestId); setShowStaffModal(true); fetchStaff(req.id || req.requestId); }}><UserCheck size={16} /><span>Assign Staff</span></button>}
                                                    {activeTab === "ongoing" && <button className="admin-view-btn" onClick={() => viewRequestDetails(req)}><Eye size={16} /><span>View Details</span></button>}
                                                    {activeTab === "completed" && <button className="admin-view-btn" onClick={() => viewRequestDetails(req)}><FileText size={16} /><span>View Details</span></button>}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                }
                            </>
                        )}

                        {activeTab === "staff" && (
                            <div className="admin-staff-management">
                                <div className="admin-content-header">
                                    <h2 className="admin-section-title">Department Staff</h2>
                                    <div className="admin-staff-stats">
                                        <div className="admin-staff-stat"><span className="admin-staff-stat-value">{stats.onlineStaff}</span><span className="admin-staff-stat-label">Online</span></div>
                                        <div className="admin-staff-stat"><span className="admin-staff-stat-value">{stats.totalStaff}</span><span className="admin-staff-stat-label">Total Staff</span></div>
                                        <div className="admin-staff-stat"><span className="admin-staff-stat-value">{allStaff.reduce((sum, s) => sum + (s.totalDeliveries || 0), 0)}</span><span className="admin-staff-stat-label">Total Deliveries</span></div>
                                    </div>
                                </div>
                                {staffStatsLoading ? <div className="admin-loading">Loading staff...</div> : allStaff.length === 0 ? <div className="admin-empty-state"><Users size={48} /><h3>No staff members</h3><p>No staff assigned to {adminDepartment} department</p><button className="admin-refresh-empty-btn" onClick={fetchAllStaff}><RefreshCw size={16} /> Refresh Staff List</button></div> :
                                    <div className="admin-staff-grid">
                                        {allStaff.map(staff => (
                                            <div className="admin-staff-card" key={staff.staffId || staff.id}>
                                                <div className="admin-staff-header">
                                                    <div className="admin-staff-avatar">{staff.name?.charAt(0).toUpperCase() || 'S'}<div className={`admin-staff-avatar-status ${staff.online ? "online" : "offline"}`}>{staff.online ? <Wifi size={12} /> : <WifiOff size={12} />}</div></div>
                                                    <div className="admin-staff-info">
                                                        <h4 className="admin-staff-name">{staff.name || 'Staff Member'}</h4>
                                                        <div className="admin-staff-details">
                                                            <span className="admin-staff-id">ID: {staff.staffId || staff.id}</span>
                                                            <span className={`admin-staff-status ${staff.online ? "online" : "offline"}`}>{staff.online ? "● Online" : "○ Offline"}{!staff.online && staff.lastActive && <span className="admin-staff-last-active">Last active: {formatLastActive(staff.lastActive)}</span>}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div className="admin-staff-metrics">
                                                    <div className="admin-staff-metric"><div className="admin-metric-icon">⭐</div><div className="admin-metric-content"><span className="admin-metric-value">{staff.stars || staff.rating || 0}</span><span className="admin-metric-label">Rating</span></div></div>
                                                    <div className="admin-staff-metric"><div className="admin-metric-icon">📦</div><div className="admin-metric-content"><span className="admin-metric-value">{staff.totalDeliveries || staff.completedTasks || 0}</span><span className="admin-metric-label">Deliveries</span></div></div>
                                                    <div className="admin-staff-metric"><div className="admin-metric-icon">🏅</div><div className="admin-metric-content"><span className="admin-metric-value">{staff.department || 'General'}</span><span className="admin-metric-label">Dept</span></div></div>
                                                </div>
                                                <div className="admin-staff-actions"><button className="admin-staff-view-btn" onClick={() => alert(`Staff Profile: ${staff.name} (${staff.staffId})`)}>View Profile</button></div>
                                            </div>
                                        ))}
                                    </div>
                                }
                            </div>
                        )}
                    </div>
                )}
            </div>

            {showStaffModal && selectedRequest && (
                <div className="admin-modal-overlay">
                    <div className="admin-modal">
                        <div className="admin-modal-header"><h3>Assign Staff Member</h3><button className="admin-modal-close" onClick={() => setShowStaffModal(false)}><X size={20} /></button></div>
                        <div className="admin-modal-content">
                            <div className="admin-request-preview"><h4>Request Details</h4><p><strong>Room:</strong> {selectedRequest.classRoom}</p><p><strong>Professor:</strong> {selectedRequest.professorName}</p></div>
                            <div className="admin-staff-selection">
                                <h4>Available Staff ({staffList.filter(s => s.online).length}/{staffList.length})</h4>
                                {staffLoading ? (
                                    <div className="admin-loading">Loading...</div>
                                ) : staffList.filter(s => s.online).length === 0 ? (
                                    <div className="admin-empty-state">
                                        <p>No staff available</p>

                                        {/* ADD TO QUEUE BUTTON */}
                                        <button
                                            className="admin-assign-btn warning"
                                            onClick={async () => {
                                                try {
                                                    await axios.put(
                                                        `http://localhost:8080/api/admin/amenities/${selectedRequestId}/queue`,
                                                        {},
                                                        { headers: { Authorization: `Bearer ${token}` } }
                                                    );
                                                    setShowStaffModal(false);
                                                    fetchAllData();
                                                } catch (err) {
                                                    console.error("Add to queue failed", err);
                                                }
                                            }}
                                        >
                                            📥 Add to Queue
                                        </button>
                                    </div>
                                ) : (
                                    <div className="admin-staff-options">
                                        {staffList.map(s => (
                                            <button
                                                key={s.staffId || s.id}
                                                className={`admin-staff-option ${s.online ? "online" : "offline"}`}
                                                disabled={!s.online}
                                                onClick={() => s.online && assignStaff(s.staffId || s.id)}
                                            >
                                                <div className="admin-staff-option-avatar">
                                                    {s.name?.charAt(0).toUpperCase()}
                                                    <div
                                                        className={`admin-staff-option-avatar-status ${s.online ? "online" : "offline"
                                                            }`}
                                                    >
                                                        {s.online ? <Wifi size={10} /> : <WifiOff size={10} />}
                                                    </div>
                                                </div>

                                                <div className="admin-staff-option-info">
                                                    <strong>{s.name}</strong>
                                                    <div className="admin-staff-option-stats">
                                                        <span>⭐ {s.stars || 0}</span>
                                                        <span>📦 {s.totalDeliveries || 0}</span>
                                                    </div>
                                                </div>
                                            </button>
                                        ))}
                                    </div>
                                )}

                            </div>
                        </div>
                        <div className="admin-modal-footer"><button className="admin-modal-cancel" onClick={() => setShowStaffModal(false)}>Cancel</button></div>
                    </div>
                </div>
            )}

            {showLogoutConfirm && (
                <div className="admin-modal-overlay">
                    <div className="admin-modal logout-modal">
                        <div className="admin-modal-header"><h3>Confirm Logout</h3></div>
                        <div className="admin-modal-content"><AlertCircle size={48} className="logout-icon" /><p>Are you sure you want to log out from the admin dashboard?</p></div>
                        <div className="admin-modal-footer"><button className="admin-modal-cancel" onClick={() => setShowLogoutConfirm(false)}>Cancel</button><button className="admin-modal-confirm" onClick={handleLogout}>Logout</button></div>
                    </div>
                </div>
            )}
        </div>
    );
}