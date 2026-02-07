import { useState } from "react";
import axios from "../services/api";
import { useNavigate, Link } from "react-router-dom";
import "./StaffForgotPassword.css";

export default function StaffForgotPassword() {
  const [staffId, setStaffId] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const sendOtp = async () => {
    if (!staffId.trim()) {
      alert("Enter Staff ID");
      return;
    }

    setLoading(true);
    try {
      await axios.post("/onboarding/staff/send-otp", {
        userId: staffId
      });

      alert("OTP sent to registered email");
      navigate("/staff/verify-otp", { state: { staffId } });
    } catch (err) {
      alert(err.response?.data?.message || "Failed to send OTP");
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter") {
      sendOtp();
    }
  };

  return (
    <div className="staff-auth-page">
      <Link to="/staff/login" className="back-link">
        ← Back to Login
      </Link>
      
      <div className="auth-card">
        <h2>Reset Password</h2>
        
        <p className="instructions">
          Enter your Staff ID to receive a password reset OTP via email.
        </p>
        
        <input
          placeholder="Enter your Staff ID"
          value={staffId}
          onChange={(e) => setStaffId(e.target.value)}
          onKeyPress={handleKeyPress}
          disabled={loading}
        />

        <button onClick={sendOtp} disabled={loading}>
          {loading ? "Sending OTP..." : "Send OTP"}
        </button>
        
        <div style={{ marginTop: "20px", fontSize: "14px", color: "#64748b" }}>
          <p>Didn't receive OTP? <button 
            onClick={sendOtp} 
            style={{ 
              background: "none", 
              border: "none", 
              color: "#3b82f6", 
              cursor: "pointer",
              fontSize: "14px",
              padding: 0,
              textDecoration: "underline"
            }}
            disabled={loading}
          >
            Resend OTP
          </button></p>
        </div>
      </div>
    </div>
  );
}