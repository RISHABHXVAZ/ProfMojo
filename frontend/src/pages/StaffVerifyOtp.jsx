import { useState } from "react";
import axios from "../services/api";
import { useLocation, useNavigate } from "react-router-dom";
import "./StaffVerifyOtp.css";

export default function StaffVerifyOtp() {
  const { state } = useLocation();
  const navigate = useNavigate();

  const staffId = state?.staffId;

  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const verifyOtp = async () => {
    if (!otp || !password) {
      alert("Fill all fields");
      return;
    }

    setLoading(true);
    try {
      await axios.post("/onboarding/staff/set-password", {
        userId: staffId,
        otp,
        password
      });

      alert("Password set successfully");
      navigate("/staff/login");
    } catch (err) {
      alert(err.response?.data?.message || "OTP verification failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="staff-auth-page">
      <h2>Verify OTP</h2>

      <input placeholder="OTP" value={otp} onChange={(e) => setOtp(e.target.value)} />
      <input
        type="password"
        placeholder="New Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />

      <button onClick={verifyOtp} disabled={loading}>
        {loading ? "Verifying..." : "Set Password"}
      </button>
    </div>
  );
}
