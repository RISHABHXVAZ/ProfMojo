import { useState } from "react";
import axios from "../services/api";
import "./StudentLogin.css";

export default function StudentForgotPassword() {
    const [regNo, setRegNo] = useState("");
    const [loading, setLoading] = useState(false);

    const goBack = () => {
        window.history.back();
    };

    const sendOtp = async () => {
    if (!regNo) {
        alert("Enter registration number");
        return;
    }

    setLoading(true);
    try {
        await axios.post("/onboarding/student/send-otp", {
            userId: regNo
        });

        localStorage.setItem("studentRegNo", regNo);
        window.location.href = "/student/verify-otp";
    } catch (err) {
        alert(err.response?.data?.message || "Failed to send OTP");
    } finally {
        setLoading(false);
    }
};


    return (
        <div className="student-login-page">
            <header className="login-header">

                <h1 className="login-logo">ProfMojo</h1>
                <p className="login-subtitle">Student • Forgot Password</p>
            </header>
            <main className="login-main">
                <div className="login-card">
                    <button className="card-back-button" onClick={goBack}>
                        ← Back
                    </button>

                    <h2>Verify Registration Number</h2>

                    <input
                        className="form-input"
                        placeholder="Enter Registration Number"
                        value={regNo}
                        onChange={(e) => setRegNo(e.target.value)}
                    />

                    <button className="login-button" onClick={sendOtp} disabled={loading}>
                        {loading ? "Sending OTP..." : "Send OTP"}
                    </button>
                </div>
            </main>
        </div>
    );
}
