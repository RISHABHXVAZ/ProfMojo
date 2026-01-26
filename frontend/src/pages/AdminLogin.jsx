import { useState } from "react";
import axios from "axios";
import { useNavigate, Link } from "react-router-dom";
import { Eye, EyeOff, Shield, Key, User, AlertCircle } from "lucide-react";
import "./AdminLogin.css";

export default function AdminLogin() {
    const [adminId, setAdminId] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [step, setStep] = useState("LOGIN"); // LOGIN | SET_PASSWORD
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [passwordStrength, setPasswordStrength] = useState(0);

    const navigate = useNavigate();

    const calculatePasswordStrength = (pwd) => {
        let strength = 0;
        if (pwd.length >= 8) strength += 1;
        if (/[A-Z]/.test(pwd)) strength += 1;
        if (/[0-9]/.test(pwd)) strength += 1;
        if (/[^A-Za-z0-9]/.test(pwd)) strength += 1;
        return strength;
    };

    const handlePasswordChange = (e) => {
        const newPassword = e.target.value;
        setPassword(newPassword);
        if (step === "SET_PASSWORD") {
            setPasswordStrength(calculatePasswordStrength(newPassword));
        }
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            const res = await axios.post(
                "http://localhost:8080/api/admin/auth/login",
                { adminId, password }
            );

            localStorage.setItem("token", res.data.token);
            localStorage.setItem("role", "ADMIN");
            localStorage.setItem("adminId", adminId);

            navigate("/admin/dashboard");

        } catch (err) {
            const status = err.response?.status;
            const message = err.response?.data?.message || "";

            // Add 403 to the list of statuses that trigger the switch to SET_PASSWORD
            if (status === 401 || status === 403 || status === 400 || status === 404 || message.toLowerCase().includes("not set")) {
                setStep("SET_PASSWORD");
                setError("First-time access detected or setup required. Please set your admin password.");
            } else {
                setError(message || "Invalid Admin ID or password");
            }
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

        if (password.length < 8) {
            setError("Password must be at least 8 characters long");
            setLoading(false);
            return;
        }

        if (passwordStrength < 2) {
            setError("Please use a stronger password (include uppercase, numbers, or special characters)");
            setLoading(false);
            return;
        }

        try {
            // Backend API Call to update NULL password to encrypted string
            await axios.post(
                "http://localhost:8080/api/admin/auth/set-password",
                { adminId, password }
            );

            // Success Visual Feedback using a toast (existing CSS classes used)
            const successToast = document.createElement("div");
            successToast.className = "admin-toast success";
            successToast.innerHTML = `
                <span class="admin-toast-icon">✓</span>
                <span>Password set successfully! Redirecting to login...</span>
            `;
            document.body.appendChild(successToast);
            setTimeout(() => successToast.remove(), 3000);

            // Auto switch back to login step
            setTimeout(() => {
                setStep("LOGIN");
                setPassword("");
                setConfirmPassword("");
                setPasswordStrength(0);
                setError("");
            }, 2000);

        } catch (err) {
            setError(err.response?.data?.message || "Unable to set password. Please check your Admin ID.");
        } finally {
            setLoading(false);
        }
    };

    const getPasswordStrengthColor = () => {
        switch (passwordStrength) {
            case 0: return "#ef4444";
            case 1: return "#f97316";
            case 2: return "#eab308";
            case 3: return "#84cc16";
            case 4: return "#10b981";
            default: return "#e5e7eb";
        }
    };

    const getPasswordStrengthText = () => {
        switch (passwordStrength) {
            case 0: return "Very Weak";
            case 1: return "Weak";
            case 2: return "Fair";
            case 3: return "Good";
            case 4: return "Strong";
            default: return "";
        }
    };

    return (
        <div className="admin-login-page">
            <div className="admin-login-bg">
                <div className="admin-bg-shapes">
                    <div className="admin-bg-shape shape-1"></div>
                    <div className="admin-bg-shape shape-2"></div>
                    <div className="admin-bg-shape shape-3"></div>
                </div>
            </div>

            <header className="admin-login-header">
                <div className="admin-header-content">
                    <Link to="/" className="admin-back-home">
                        <span className="admin-back-arrow">←</span>
                        Back to Home
                    </Link>
                    <div className="admin-logo-container">
                        <div className="admin-logo-wrapper">
                            <Shield className="admin-logo-icon" />
                            <h1 className="admin-login-logo">ProfMojo</h1>
                        </div>
                        <p className="admin-login-subtitle">System Administration Portal</p>
                    </div>
                </div>
            </header>

            <main className="admin-login-main">
                <div className="admin-login-container">
                    <div className="admin-welcome-card">
                        <div className="admin-role-indicator">
                            <div className="admin-role-icon">
                                <Shield size={40} />
                            </div>
                            <div className="admin-welcome-texts">
                                <h2 className="admin-welcome-title">
                                    {step === "LOGIN" ? "Admin Portal Login" : "Setup Admin Access"}
                                </h2>
                                <p className="admin-welcome-desc">
                                    {step === "LOGIN"
                                        ? "Access the system administration dashboard with elevated privileges"
                                        : "Configure your admin credentials for first-time access"
                                    }
                                </p>
                            </div>
                        </div>

                        <div className="admin-step-indicator">
                            <div className={`admin-step ${step === "LOGIN" ? "active" : ""}`}>
                                <span className="admin-step-number">1</span>
                                <span className="admin-step-label">Login</span>
                            </div>
                            <div className="admin-step-line"></div>
                            <div className={`admin-step ${step === "SET_PASSWORD" ? "active" : ""}`}>
                                <span className="admin-step-number">2</span>
                                <span className="admin-step-label">Setup</span>
                            </div>
                        </div>
                    </div>

                    <div className="admin-form-card">
                        <form
                            className="admin-login-form"
                            onSubmit={step === "LOGIN" ? handleLogin : handleSetPassword}
                        >
                            <div className="admin-input-group">
                                <label htmlFor="adminId" className="admin-input-label">
                                    <User size={16} />
                                    <span>Admin ID</span>
                                </label>
                                <div className="admin-input-wrapper">
                                    <input
                                        id="adminId"
                                        type="text"
                                        className="admin-form-input"
                                        placeholder="Enter your Admin ID"
                                        value={adminId}
                                        onChange={(e) => setAdminId(e.target.value)}
                                        required
                                        disabled={loading}
                                        autoComplete="username"
                                    />
                                </div>
                            </div>

                            <div className="admin-input-group">
                                <label htmlFor="password" className="admin-input-label">
                                    <Key size={16} />
                                    <span>{step === "LOGIN" ? "Password" : "New Password"}</span>
                                </label>
                                <div className="admin-input-wrapper">
                                    <input
                                        id="password"
                                        type={showPassword ? "text" : "password"}
                                        className="admin-form-input"
                                        placeholder={step === "LOGIN" ? "Enter your password" : "Create a secure password"}
                                        value={password}
                                        onChange={handlePasswordChange}
                                        required
                                        disabled={loading}
                                        autoComplete={step === "LOGIN" ? "current-password" : "new-password"}
                                    />
                                    <button
                                        type="button"
                                        className="admin-password-toggle"
                                        onClick={() => setShowPassword(!showPassword)}
                                        tabIndex="-1"
                                    >
                                        {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                    </button>
                                </div>

                                {step === "SET_PASSWORD" && password && (
                                    <div className="admin-password-strength">
                                        <div className="admin-strength-meter">
                                            <div
                                                className="admin-strength-bar"
                                                style={{
                                                    width: `${(passwordStrength / 4) * 100}%`,
                                                    backgroundColor: getPasswordStrengthColor()
                                                }}
                                            ></div>
                                        </div>
                                        <span className="admin-strength-text" style={{ color: getPasswordStrengthColor() }}>
                                            {getPasswordStrengthText()}
                                        </span>
                                    </div>
                                )}
                            </div>

                            {step === "SET_PASSWORD" && (
                                <div className="admin-input-group">
                                    <label htmlFor="confirmPassword" className="admin-input-label">
                                        <Key size={16} />
                                        <span>Confirm Password</span>
                                    </label>
                                    <div className="admin-input-wrapper">
                                        <input
                                            id="confirmPassword"
                                            type={showConfirmPassword ? "text" : "password"}
                                            className="admin-form-input"
                                            placeholder="Confirm your new password"
                                            value={confirmPassword}
                                            onChange={(e) => setConfirmPassword(e.target.value)}
                                            required
                                            disabled={loading}
                                            autoComplete="new-password"
                                        />
                                        <button
                                            type="button"
                                            className="admin-password-toggle"
                                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                            tabIndex="-1"
                                        >
                                            {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                        </button>
                                    </div>
                                </div>
                            )}

                            {error && (
                                <div className="admin-error-message">
                                    <AlertCircle size={20} />
                                    <div className="admin-error-content">
                                        <strong>Error</strong>
                                        <p>{error}</p>
                                    </div>
                                </div>
                            )}

                            <button
                                type="submit"
                                className="admin-submit-button"
                                disabled={loading}
                            >
                                {loading ? (
                                    <span className="admin-button-loading">
                                        <span className="admin-loading-spinner"></span>
                                        {step === "LOGIN" ? "Authenticating..." : "Setting up access..."}
                                    </span>
                                ) : (
                                    <>
                                        <Shield size={18} />
                                        <span>
                                            {step === "LOGIN" ? "Access Admin Dashboard" : "Complete Setup"}
                                        </span>
                                    </>
                                )}
                            </button>

                            {step === "SET_PASSWORD" && (
                                <div className="admin-requirements">
                                    <h4 className="admin-requirements-title">Password Requirements:</h4>
                                    <ul className="admin-requirements-list">
                                        <li className={password.length >= 8 ? "valid" : ""}>At least 8 characters long</li>
                                        <li className={/[A-Z]/.test(password) ? "valid" : ""}>Contains uppercase letter</li>
                                        <li className={/[0-9]/.test(password) ? "valid" : ""}>Contains number</li>
                                        <li className={/[^A-Za-z0-9]/.test(password) ? "valid" : ""}>Contains special character</li>
                                    </ul>
                                </div>
                            )}
                        </form>

                        <div className="admin-security-info">
                            <div className="admin-security-header">
                                <Shield size={20} />
                                <h3>Security Information</h3>
                            </div>
                            <div className="admin-security-grid">
                                <div className="admin-security-item">
                                    <div className="admin-security-icon">🔐</div>
                                    <div className="admin-security-content">
                                        <h4>Two-Factor Ready</h4>
                                        <p>Support for 2FA authentication available</p>
                                    </div>
                                </div>
                                <div className="admin-security-item">
                                    <div className="admin-security-icon">📊</div>
                                    <div className="admin-security-content">
                                        <h4>Activity Logging</h4>
                                        <p>All admin actions are logged and monitored</p>
                                    </div>
                                </div>
                                <div className="admin-security-item">
                                    <div className="admin-security-icon">🛡️</div>
                                    <div className="admin-security-content">
                                        <h4>Encrypted Session</h4>
                                        <p>End-to-end encrypted secure sessions</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </main>

            <footer className="admin-login-footer">
                <div className="admin-footer-content">
                    <p className="admin-footer-text">
                        <span className="admin-footer-copyright">© 2024 ProfMojo Campus Management System</span>
                        <span className="admin-footer-version">v2.1.0</span>
                    </p>
                    <div className="admin-footer-links">
                        <Link to="/about" className="admin-footer-link">About</Link>
                        <span className="admin-footer-separator">•</span>
                        <Link to="/contact" className="admin-footer-link">Contact Support</Link>
                        <span className="admin-footer-separator">•</span>
                        <Link to="/privacy" className="admin-footer-link">Privacy Policy</Link>
                        <span className="admin-footer-separator">•</span>
                        <Link to="/terms" className="admin-footer-link">Terms of Service</Link>
                    </div>
                </div>
            </footer>
        </div>
    );
}