import { useState } from "react";
import axios from "axios";
import { useNavigate, Link } from "react-router-dom";
import "./StaffLogin.css";

export default function StaffLogin() {
    const [staffId, setStaffId] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        if (!staffId.trim() || !password.trim()) {
            setError("Please enter Staff ID and password");
            setLoading(false);
            return;
        }

        try {
            const res = await axios.post(
                "http://localhost:8080/api/staff/auth/login",
                { staffId, password }
            );

            localStorage.setItem("token", res.data.token);
            localStorage.setItem("role", "STAFF");
            localStorage.setItem("staffId", staffId);

            navigate("/staff/dashboard");
        } catch (err) {
            setError(err.response?.data?.message || "Invalid Staff ID or password");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="staff-login-page">
            {/* Header */}
            <header className="staff-login-header">
                <div className="staff-header-content">
                    <Link to="/" className="staff-back-link">
                        ← Back to Home
                    </Link>

                    <div className="staff-logo-container">
                        <h1 className="staff-login-logo">ProfMojo</h1>
                        <p className="staff-login-subtitle">Staff Portal</p>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="staff-login-main">
                <div className="staff-login-container">
                    <div className="staff-login-area">
                        <div className="staff-form-header">
                            <h2 className="staff-form-title">Staff Login</h2>
                            <p className="staff-form-subtitle">
                                Enter your credentials to access the system
                            </p>
                        </div>

                        <form className="staff-login-form" onSubmit={handleLogin}>
                            {/* Staff ID */}
                            <div className="staff-form-group">
                                <label className="staff-form-label">Staff ID</label>
                                <input
                                    type="text"
                                    className="staff-form-input"
                                    placeholder="Enter your Staff ID"
                                    value={staffId}
                                    onChange={(e) => setStaffId(e.target.value)}
                                    disabled={loading}
                                    required
                                />
                            </div>

                            {/* Password */}
                            <div className="staff-form-group">
                                <label className="staff-form-label">Password</label>
                                <input
                                    type="password"
                                    className="staff-form-input"
                                    placeholder="Enter your password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    disabled={loading}
                                    required
                                />
                            </div>

                            {/* Error */}
                            {error && (
                                <div className="staff-error-message">
                                    <span className="staff-error-icon">!</span>
                                    {error}
                                </div>
                            )}

                            {/* Login Button */}
                            <button
                                type="submit"
                                className="staff-submit-button"
                                disabled={loading}
                            >
                                {loading ? (
                                    <span className="staff-button-loading">
                                        <span className="staff-loading-spinner"></span>
                                        Logging in...
                                    </span>
                                ) : (
                                    "Login"
                                )}
                            </button>

                            {/* Forgot Password */}
                            <div className="staff-mode-toggle">
                                <p className="staff-toggle-text">
                                    Forgot your password?{" "}
                                    <Link to="/staff/forgot-password" className="staff-toggle-link">
                                        Reset via OTP
                                    </Link>
                                </p>
                            </div>
                        </form>
                    </div>

                    {/* Footer Note */}
                    <div className="staff-login-footer-note">
                        <p className="staff-footer-text">
                            Having trouble? Contact your administrator at{" "}
                            <a href="mailto:support@profmojo.edu" className="staff-footer-link">
                                support@profmojo.edu
                            </a>
                        </p>
                    </div>
                </div>
            </main>

            {/* Footer */}
            <footer className="staff-login-footer">
                <p className="staff-footer-copyright">
                    © 2024 ProfMojo Campus Management System
                </p>
                <p className="staff-footer-links">
                    <Link to="/about" className="staff-footer-link">About</Link> •
                    <Link to="/contact" className="staff-footer-link">Contact</Link> •
                    <Link to="/privacy" className="staff-footer-link">Privacy</Link> •
                    <Link to="/terms" className="staff-footer-link">Terms</Link>
                </p>
            </footer>
        </div>
    );
}
