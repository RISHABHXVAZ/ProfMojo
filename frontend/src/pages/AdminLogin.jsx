import { useState } from "react";
import axios from "axios";
import { useNavigate, Link } from "react-router-dom";
import { Shield, Key, Mail, AlertCircle } from "lucide-react";
import "./AdminLogin.css";

export default function AdminLogin() {
    const [secretKey, setSecretKey] = useState("");
    const [otp, setOtp] = useState("");
    const [step, setStep] = useState("ENTER_SECRET"); // ENTER_SECRET | VERIFY_OTP
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();

    const sendOtp = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            await axios.post(
                "http://localhost:8080/api/admin/auth/send-otp",
                { secretKey }
            );

            setStep("VERIFY_OTP");
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Invalid secret key or access denied"
            );
        } finally {
            setLoading(false);
        }
    };

    const verifyOtp = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            const res = await axios.post(
                "http://localhost:8080/api/admin/auth/verify-otp",
                { secretKey, otp }
            );

            localStorage.setItem("token", res.data.token);
            localStorage.setItem("role", "ADMIN");
            localStorage.setItem("department", res.data.department);

            navigate("/admin/dashboard");
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Invalid or expired OTP"
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="admin-login-page">
            <header className="admin-login-header">
                <div className="admin-header-content">
                    <Link to="/" className="admin-back-home">
                        ← Back to Home
                    </Link>
                    <div className="admin-logo-container">
                        <Shield className="admin-logo-icon" />
                        <h1 className="admin-login-logo">ProfMojo</h1>
                        <p className="admin-login-subtitle">
                            Department Admin Access
                        </p>
                    </div>
                </div>
            </header>

            <main className="admin-login-main">
                <div className="admin-login-container">

                    <div className="admin-welcome-card">
                        <h2 className="admin-welcome-title">
                            {step === "ENTER_SECRET"
                                ? "Admin Authentication"
                                : "Verify OTP"}
                        </h2>
                        <p className="admin-welcome-desc">
                            {step === "ENTER_SECRET"
                                ? "Enter your department secret key to receive an OTP"
                                : "Enter the OTP sent to your department email"}
                        </p>
                    </div>

                    <div className="admin-form-card">
                        <form
                            className="admin-login-form"
                            onSubmit={step === "ENTER_SECRET" ? sendOtp : verifyOtp}
                        >
                            {step === "ENTER_SECRET" && (
                                <div className="admin-input-group">
                                    <label className="admin-input-label">
                                        <Key size={16} />
                                        Secret Key
                                    </label>
                                    <input
                                        type="text"
                                        className="admin-form-input"
                                        placeholder="Enter department secret key"
                                        value={secretKey}
                                        onChange={(e) => setSecretKey(e.target.value)}
                                        required
                                        disabled={loading}
                                    />
                                </div>
                            )}

                            {step === "VERIFY_OTP" && (
                                <div className="admin-input-group">
                                    <label className="admin-input-label">
                                        <Mail size={16} />
                                        One-Time Password
                                    </label>
                                    <input
                                        type="text"
                                        className="admin-form-input"
                                        placeholder="Enter 6-digit OTP"
                                        value={otp}
                                        onChange={(e) => setOtp(e.target.value)}
                                        required
                                        disabled={loading}
                                    />
                                </div>
                            )}

                            {error && (
                                <div className="admin-error-message">
                                    <AlertCircle size={18} />
                                    <p>{error}</p>
                                </div>
                            )}

                            <button
                                type="submit"
                                className="admin-submit-button"
                                disabled={loading}
                            >
                                {loading
                                    ? "Please wait..."
                                    : step === "ENTER_SECRET"
                                        ? "Send OTP"
                                        : "Verify & Login"}
                            </button>

                            
                        </form>
                    </div>
                </div>
            </main>

            <footer className="admin-login-footer">
                <p>© 2024 ProfMojo Campus Management System</p>
            </footer>
        </div>
    );
}
