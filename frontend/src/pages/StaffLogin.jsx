import { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./StaffLogin.css";

export default function StaffLogin() {

    const [staffId, setStaffId] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [mode, setMode] = useState("LOGIN"); // LOGIN | SET_PASSWORD
    const [error, setError] = useState("");

    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");

        try {
            const res = await axios.post(
                "http://localhost:8080/api/staff/auth/login",
                { staffId, password }
            );

            localStorage.setItem("token", res.data.token);
            localStorage.setItem("role", "STAFF");

            navigate("/staff/dashboard");

        } catch (err) {
            setError("Invalid Staff ID or password");
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
                "http://localhost:8080/api/staff/auth/set-password",
                { staffId, password }
            );

            setMode("LOGIN");
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
                onSubmit={mode === "LOGIN" ? handleLogin : handleSetPassword}
            >
                <h2>Staff {mode === "LOGIN" ? "Login" : "Set Password"}</h2>

                <input
                    type="text"
                    placeholder="Staff ID"
                    value={staffId}
                    onChange={(e) => setStaffId(e.target.value)}
                    required
                />

                <input
                    type="password"
                    placeholder={mode === "LOGIN" ? "Password" : "Set Password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />

                {mode === "SET_PASSWORD" && (
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
                    {mode === "LOGIN" ? "Login" : "Set Password"}
                </button>

                {mode === "LOGIN" && (
                    <p
                        className="link-text"
                        onClick={() => setMode("SET_PASSWORD")}
                    >
                        First time? Set password here
                    </p>
                )}

                {mode === "SET_PASSWORD" && (
                    <p
                        className="link-text"
                        onClick={() => setMode("LOGIN")}
                    >
                        Back to Login
                    </p>
                )}
            </form>
        </div>
    );
}
