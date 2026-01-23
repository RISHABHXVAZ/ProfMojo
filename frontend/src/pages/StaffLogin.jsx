import { useState } from "react";
import axios from "axios";
import { useNavigate, Link } from "react-router-dom";
import "./StaffLogin.css";

export default function StaffLogin() {
    const [staffId, setStaffId] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [mode, setMode] = useState("LOGIN"); // LOGIN | SET_PASSWORD
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

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

    const handleSetPassword = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        if (password !== confirmPassword) {
            setError("Passwords do not match");
            setLoading(false);
            return;
        }

        if (password.length < 6) {
            setError("Password must be at least 6 characters long");
            setLoading(false);
            return;
        }

        try {
            await axios.post(
                "http://localhost:8080/api/staff/auth/set-password",
                { staffId, password }
            );

            alert("Password set successfully! Please login with your new password.");
            setMode("LOGIN");
            setPassword("");
            setConfirmPassword("");
        } catch (err) {
            setError(err.response?.data?.message || "Unable to set password. Please check your Staff ID.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="staff-login-page">
            {/* Simple Header */}
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
                    {/* Simple Form Area */}
                    <div className="staff-login-area">
                        <div className="staff-form-header">
                            <h2 className="staff-form-title">
                                {mode === "LOGIN" ? "Staff Login" : "Set Password"}
                            </h2>
                            <p className="staff-form-subtitle">
                                {mode === "LOGIN" 
                                    ? "Enter your credentials to access the system"
                                    : "Set your password for first-time access"
                                }
                            </p>
                        </div>

                        {/* Simple Form */}
                        <form 
                            className="staff-login-form"
                            onSubmit={mode === "LOGIN" ? handleLogin : handleSetPassword}
                        >
                            {/* Staff ID Field */}
                            <div className="staff-form-group">
                                <label htmlFor="staffId" className="staff-form-label">
                                    Staff ID
                                </label>
                                <input
                                    id="staffId"
                                    type="text"
                                    className="staff-form-input"
                                    placeholder="Enter your Staff ID"
                                    value={staffId}
                                    onChange={(e) => setStaffId(e.target.value)}
                                    required
                                    disabled={loading}
                                />
                            </div>

                            {/* Password Field */}
                            <div className="staff-form-group">
                                <label htmlFor="password" className="staff-form-label">
                                    {mode === "LOGIN" ? "Password" : "New Password"}
                                </label>
                                <input
                                    id="password"
                                    type="password"
                                    className="staff-form-input"
                                    placeholder={mode === "LOGIN" ? "Enter your password" : "Create new password"}
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                    disabled={loading}
                                />
                            </div>

                            {/* Confirm Password Field (only for SET_PASSWORD) */}
                            {mode === "SET_PASSWORD" && (
                                <div className="staff-form-group">
                                    <label htmlFor="confirmPassword" className="staff-form-label">
                                        Confirm Password
                                    </label>
                                    <input
                                        id="confirmPassword"
                                        type="password"
                                        className="staff-form-input"
                                        placeholder="Confirm your password"
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        required
                                        disabled={loading}
                                    />
                                </div>
                            )}

                            {/* Error Message */}
                            {error && (
                                <div className="staff-error-message">
                                    <span className="staff-error-icon">!</span>
                                    {error}
                                </div>
                            )}

                            {/* Submit Button */}
                            <button 
                                type="submit" 
                                className="staff-submit-button"
                                disabled={loading}
                            >
                                {loading ? (
                                    <span className="staff-button-loading">
                                        <span className="staff-loading-spinner"></span>
                                        {mode === "LOGIN" ? "Logging in..." : "Setting password..."}
                                    </span>
                                ) : (
                                    mode === "LOGIN" ? "Login" : "Set Password"
                                )}
                            </button>

                            {/* Mode Toggle */}
                            <div className="staff-mode-toggle">
                                {mode === "LOGIN" ? (
                                    <p className="staff-toggle-text">
                                        First time here?{" "}
                                        <button
                                            type="button"
                                            className="staff-toggle-link"
                                            onClick={() => {
                                                setMode("SET_PASSWORD");
                                                setError("");
                                            }}
                                            disabled={loading}
                                        >
                                            Set password
                                        </button>
                                    </p>
                                ) : (
                                    <p className="staff-toggle-text">
                                        Already have an account?{" "}
                                        <button
                                            type="button"
                                            className="staff-toggle-link"
                                            onClick={() => {
                                                setMode("LOGIN");
                                                setError("");
                                            }}
                                            disabled={loading}
                                        >
                                            Back to login
                                        </button>
                                    </p>
                                )}
                            </div>
                        </form>
                    </div>

                    {/* Simple Footer Note */}
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

            {/* Simple Footer */}
            <footer className="staff-login-footer">
                <p className="staff-footer-copyright">© 2024 ProfMojo Campus Management System</p>
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