import { useState } from "react";
import axios from "../services/api";
import "./StudentLogin.css";

export default function StudentVerifyOtp() {
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const regNo = localStorage.getItem("studentRegNo");

  const goBack = () => {
    window.history.back();
  };

  const verifyOtp = async () => {
  if (!otp || !password) {
    alert("Fill all fields");
    return;
  }

  setLoading(true);
  try {
    await axios.post("/onboarding/student/set-password", {
      userId: regNo,
      otp,
      password
    });

    alert("Password set successfully");
    localStorage.removeItem("studentRegNo");
    window.location.href = "/student/login";
  } catch (err) {
    alert(err.response?.data?.message || "Invalid OTP");
  } finally {
    setLoading(false);
  }
};


  return (
    <div className="student-login-page">
      <header className="login-header">
        <h1 className="login-logo">ProfMojo</h1>
        <p className="login-subtitle">Verify OTP</p>
      </header>

      <main className="login-main">
        <div className="login-card">
          {/* Back button */}
          <button className="card-back-button" onClick={goBack}>
            ← Back
          </button>

          <input
            className="form-input"
            placeholder="Enter OTP"
            value={otp}
            onChange={(e) => setOtp(e.target.value)}
          />

          <input
            type="password"
            className="form-input"
            placeholder="Set New Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <button
            className="login-button"
            onClick={verifyOtp}
            disabled={loading}
          >
            {loading ? "Verifying..." : "Set Password"}
          </button>
        </div>
      </main>
    </div>
  );
}
