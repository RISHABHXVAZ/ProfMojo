import { useState } from "react";
import axios from "../services/api";
import { useNavigate } from "react-router-dom";
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

  return (
    <div className="staff-auth-page">
      <h2>Reset Password</h2>

      <input
        placeholder="Staff ID"
        value={staffId}
        onChange={(e) => setStaffId(e.target.value)}
      />

      <button onClick={sendOtp} disabled={loading}>
        {loading ? "Sending..." : "Send OTP"}
      </button>
    </div>
  );
}
