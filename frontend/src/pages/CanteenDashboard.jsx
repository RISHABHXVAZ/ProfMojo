import { useEffect, useState } from "react";
import api from "../services/api";
import "./CanteenDashboard.css";

export default function CanteenDashboard() {
    const [activeTab, setActiveTab] = useState("orders");
    const [orders, setOrders] = useState([]);
    const [menu, setMenu] = useState([]);
    const [history, setHistory] = useState([]);
    const [canteen, setCanteen] = useState(null);
    const [incomingOrder, setIncomingOrder] = useState(null);
    const [loading, setLoading] = useState(false);
    const [showAddModal, setShowAddModal] = useState(false);
    const [newItem, setNewItem] = useState({
    name: "",
    price: "",
    available: true
});

    const availableItems = menu.filter(item => item.available);
    const unavailableItems = menu.filter(item => !item.available);




    /* ================= POLLING FOR NEW ORDERS ================= */
    useEffect(() => {
        const interval = setInterval(async () => {
            try {
                const res = await api.get("/orders/active");
                const placed = res.data.find(o => o.status === "PLACED");

                if (placed && !incomingOrder) {
                    setIncomingOrder(placed);
                }
            } catch {
                console.error("Polling failed");
            }
        }, 5000);

        return () => clearInterval(interval);
    }, [incomingOrder]);

    /* ================= CANTEEN INFO ================= */
    useEffect(() => {
        api.get("/canteen/me").then(res => setCanteen(res.data));
    }, []);

    /* ================= TAB LOADERS ================= */
    useEffect(() => {
        if (activeTab === "orders") loadOrders();
        if (activeTab === "menu") loadMenu();
        if (activeTab === "history") loadHistory();
    }, [activeTab]);

    /* ================= API CALLS ================= */
    const loadOrders = async () => {
        setLoading(true);
        try {
            const res = await api.get("/orders/active");
            setOrders(res.data);
        } finally {
            setLoading(false);
        }
    };

    const loadHistory = async () => {
        const res = await api.get("/orders/history");
        setHistory(res.data);
    };

    const loadMenu = async () => {
        const res = await api.get("/canteen/menu/my");
        setMenu(res.data);
    };
    const toggleAvailability = async (itemId, currentStatus) => {
        try {
            await api.put("/canteen/menu/availability", {
                itemId,
                available: !currentStatus
            });
            loadMenu();
        } catch {
            alert("Failed to update availability");
        }
    };


    /* ================= UPDATE STATUS ================= */
    const updateStatus = async (orderId, status) => {
        try {
            await api.put("/orders/status", { orderId, status });
            setIncomingOrder(null);
            loadOrders();
            loadHistory();
        } catch {
            alert("Failed to update order");
        }
    };

    /* ================= RENDER ITEMS (SAFE) ================= */
    const renderItems = (items) =>
        items.map((item, i) => (
            <li key={i}>
                {typeof item === "string"
                    ? item
                    : `${item.name} × ${item.quantity}`}
            </li>
        ));

    return (
        <div className="canteen-dashboard">
            {/* ================= SIDEBAR ================= */}
            <div className="canteen-sidebar">
                <h2 className="canteen-logo">
                    {canteen
                        ? `${canteen.canteenName} (${canteen.canteenId})`
                        : "ProfMojo"}
                </h2>

                <button
                    className={`canteen-nav ${activeTab === "orders" ? "active" : ""}`}
                    onClick={() => setActiveTab("orders")}
                >
                    Orders
                </button>

                <button
                    className={`canteen-nav ${activeTab === "menu" ? "active" : ""}`}
                    onClick={() => setActiveTab("menu")}
                >
                    Menu
                </button>

                <button
                    className={`canteen-nav ${activeTab === "history" ? "active" : ""}`}
                    onClick={() => setActiveTab("history")}
                >
                    Order History
                </button>
            </div>

            {/* ================= MAIN ================= */}
            <div className="canteen-main">

                {/* ===== INCOMING ORDER POPUP ===== */}
                {incomingOrder && (
                    <div className="modal-overlay">
                        <div className="order-card" style={{ maxWidth: "420px" }}>
                            <h3 style={{ color: "green" }}>New Order Request</h3>
                            <p><strong>Prof. {incomingOrder.professorName}</strong></p>
                            <p className="order-location">📍 {incomingOrder.cabinLocation}</p>

                            <ul className="order-items">
                                {renderItems(incomingOrder.items)}
                            </ul>

                            <div className="order-footer">
                                <span className="order-total">
                                    Total Amount: ₹{incomingOrder.totalAmount}
                                </span>
                            </div>

                            <div className="order-actions">
                                <button
                                    className="action-btn btn-prepare"
                                    onClick={() =>
                                        updateStatus(incomingOrder.id, "PREPARING")
                                    }
                                >
                                    Accept
                                </button>

                                <button
                                    className="action-btn btn-reject"
                                    onClick={() =>
                                        updateStatus(incomingOrder.id, "CANCELLED")
                                    }
                                >
                                    Reject
                                </button>
                            </div>
                        </div>
                    </div>
                )}
                {showAddModal && (
                    <div className="modal-overlay">
                        <div className="add-item-modal">
                            <h3>Add New Menu Item</h3>

                            <input
                                type="text"
                                placeholder="Item name"
                                value={newItem.name}
                                onChange={e =>
                                    setNewItem({ ...newItem, name: e.target.value })
                                }
                            />

                            <input
                                type="number"
                                placeholder="Price (₹)"
                                value={newItem.price}
                                onChange={e =>
                                    setNewItem({ ...newItem, price: e.target.value })
                                }
                            />

                            <div className="availability-row">
                                <span
                                    className={`availability-label ${newItem.available ? "on" : "off"
                                        }`}
                                >
                                    {newItem.available ? "Available" : "Not Available"}
                                </span>

                                <label className="switch small">
                                    <input
                                        type="checkbox"
                                        checked={newItem.available}
                                        onChange={e =>
                                            setNewItem({ ...newItem, available: e.target.checked })
                                        }
                                    />
                                    <span className="slider"></span>
                                </label>
                            </div>



                            <div className="modal-actions">
                                <button
                                    className="action-btn btn-prepare"
                                    onClick={async () => {
                                        await api.post("/canteen/menu/add", newItem);
                                        setShowAddModal(false);
                                        setNewItem({ name: "", price: "" });
                                        loadMenu();
                                    }}
                                >
                                    Add Item
                                </button>

                                <button
                                    className="action-btn btn-reject"
                                    onClick={() => setShowAddModal(false)}
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    </div>
                )}


                {/* ===== CURRENT ORDERS ===== */}
                {activeTab === "orders" && (
                    <>
                        <div className="canteen-header">
                            <h1>Current Orders</h1>
                        </div>

                        {loading ? (
                            <p className="empty">Loading orders...</p>
                        ) : orders.length === 0 ? (
                            <p className="empty">No orders yet</p>
                        ) : (
                            orders.map(order => (
                                <div key={order.id} className="order-card">
                                    <div className="order-top">
                                        <h3>Prof. {order.professorName}</h3>
                                        <span className={`status-badge status-${order.status}`}>
                                            {order.status}
                                        </span>
                                    </div>

                                    <p className="order-location">📍 {order.cabinLocation}</p>

                                    <ul className="order-items">
                                        {renderItems(order.items)}
                                    </ul>

                                    <div className="order-footer">
                                        <span className="order-total">
                                            ₹{order.totalAmount}
                                        </span>
                                    </div>

                                    <div className="order-actions">
                                        {order.status === "PLACED" && (
                                            <button
                                                className="action-btn btn-prepare"
                                                onClick={() =>
                                                    updateStatus(order.id, "PREPARING")
                                                }
                                            >
                                                Start Preparing
                                            </button>
                                        )}
                                        {order.status === "PREPARING" && (
                                            <button
                                                className="action-btn btn-ready"
                                                onClick={() =>
                                                    updateStatus(order.id, "READY")
                                                }
                                            >
                                                Mark Ready
                                            </button>
                                        )}
                                        {order.status === "READY" && (
                                            <button
                                                className="action-btn btn-delivered"
                                                onClick={() =>
                                                    updateStatus(order.id, "DELIVERED")
                                                }
                                            >
                                                Mark Delivered
                                            </button>
                                        )}
                                    </div>
                                </div>
                            ))
                        )}
                    </>
                )}

                {/* ===== MENU ===== */}
                {activeTab === "menu" && (
                    <>
                        <div className="canteen-header menu-header">
                            <h1>Menu</h1>

                            <button
                                className="add-item-btn"
                                onClick={() => setShowAddModal(true)}
                            >
                                + Add Item
                            </button>
                        </div>

                        {menu.length === 0 ? (
                            <p className="empty">No menu items</p>
                        ) : (
                            <>
                                {/* ===== AVAILABLE ITEMS ===== */}
                                {availableItems.length > 0 && (
                                    <>
                                        <h2 className="menu-section-title">Available Items</h2>
                                        <div className="menu-grid">
                                            {availableItems.map(item => (
                                                <div
                                                    key={item.id}
                                                    className="menu-item-card"
                                                >
                                                    <div className="menu-item-top">
                                                        <h3>{item.name}</h3>
                                                        <span className="menu-price-pill">₹{item.price}</span>
                                                    </div>

                                                    <div className="menu-item-footer">
                                                        <span className="status-pill on">
                                                            Available
                                                        </span>

                                                        <label className="switch">
                                                            <input
                                                                type="checkbox"
                                                                checked={true}
                                                                onChange={() =>
                                                                    toggleAvailability(item.id, item.available)
                                                                }
                                                            />
                                                            <span className="slider"></span>
                                                        </label>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </>
                                )}

                                {/* ===== UNAVAILABLE ITEMS ===== */}
                                {unavailableItems.length > 0 && (
                                    <>
                                        <h2 className="menu-section-title muted">
                                            Unavailable Items
                                        </h2>
                                        <div className="menu-grid unavailable-grid">
                                            {unavailableItems.map(item => (
                                                <div
                                                    key={item.id}
                                                    className="menu-item-card item-disabled"
                                                >
                                                    <div className="menu-item-top">
                                                        <h3>{item.name}</h3>
                                                        <span className="menu-price-pill">₹{item.price}</span>
                                                    </div>

                                                    <div className="menu-item-footer">
                                                        <span className="status-pill off">
                                                            Unavailable
                                                        </span>

                                                        <label className="switch">
                                                            <input
                                                                type="checkbox"
                                                                checked={false}
                                                                onChange={() =>
                                                                    toggleAvailability(item.id, item.available)
                                                                }
                                                            />
                                                            <span className="slider"></span>
                                                        </label>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </>
                                )}
                            </>
                        )}

                    </>
                )}


                {/* ===== HISTORY ===== */}
                {activeTab === "history" && (
                    <>
                        <div className="canteen-header">
                            <h1>Order History</h1>
                        </div>

                        {history.length === 0 ? (
                            <p className="empty">No delivered orders</p>
                        ) : (
                            <div className="history-list">
                                {history.map(order => (
                                    <div key={order.id} className="history-row">

                                        <div className="history-left">
                                            <h4>Prof. {order.professorName}</h4>

                                            <p className="history-location">
                                                📍 {order.cabinLocation}
                                            </p>

                                            <p className="history-meta">
                                                Order #{order.id} •{" "}
                                                {new Date(order.createdAt).toLocaleString("en-IN", {
                                                    day: "2-digit",
                                                    month: "short",
                                                    year: "numeric",
                                                    hour: "2-digit",
                                                    minute: "2-digit",
                                                })}
                                            </p>

                                            <p className="history-items">
                                                {order.items.join(", ")}
                                            </p>
                                        </div>

                                        <div className="history-right">
                                            <span className="history-amount">
                                                Total Amount Paid: ₹{order.totalAmount}
                                            </span>
                                            <span className="status-badge status-DELIVERED">
                                                DELIVERED
                                            </span>
                                        </div>

                                    </div>
                                ))}
                            </div>
                        )}
                    </>
                )}

            </div>
        </div>
    );
}
