import { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./AdminLogin.css";

export default function AdminLogin() {

    const [adminId, setAdminId] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [step, setStep] = useState("LOGIN"); // LOGIN | SET_PASSWORD
    const [error, setError] = useState("");

    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");

        try {
            const res = await axios.post(
                "http://localhost:8080/api/admin/auth/login",
                { adminId, password }
            );

            localStorage.setItem("token", res.data.token);
            localStorage.setItem("role", "ADMIN");

            navigate("/admin/dashboard");

        } catch (err) {
            if (err.response?.status === 400 || err.response?.status === 404) {
                // Password not set yet
                setStep("SET_PASSWORD");
            } else {
                setError("Invalid Admin ID or password");
            }
        }
    };

    const handleSetPassword = async (e) => {
        e.preventDefault();
        setError("");

        if (password !== confirmPassword) {
            setError("Passwords do not match");
            return;
        }

        try {
            await axios.post(
                "http://localhost:8080/api/admin/auth/set-password",
                { adminId, password }
            );

            setStep("LOGIN");
            setPassword("");
            setConfirmPassword("");

        } catch (err) {
            setError("Unable to set password");
        }
    };

    return (
        <div className="auth-container">
            <form
                className="auth-card"
                onSubmit={step === "LOGIN" ? handleLogin : handleSetPassword}
            >
                <h2>Admin Login</h2>

                <input
                    type="text"
                    placeholder="Admin ID"
                    value={adminId}
                    onChange={(e) => setAdminId(e.target.value)}
                    required
                />

                <input
                    type="password"
                    placeholder={step === "LOGIN" ? "Password" : "Set Password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />

                {step === "SET_PASSWORD" && (
                    <input
                        type="password"
                        placeholder="Confirm Password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        required
                    />
                )}

                {error && <p className="error">{error}</p>}

                <button type="submit">
                    {step === "LOGIN" ? "Login" : "Set Password"}
                </button>

                {step === "SET_PASSWORD" && (
                    <p className="info-text">
                        Set your password to activate admin access
                    </p>
                )}
            </form>
        </div>
    );
}
