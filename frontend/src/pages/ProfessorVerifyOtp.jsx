import { useState } from "react";
import axios from "../services/api";
import styles from "./ProfessorVerifyOtp.module.css";

export default function ProfessorVerifyOtp() {
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const userId = localStorage.getItem("resetProfId");

  const verifyAndSet = async () => {
    if (!otp || !password) {
      alert("All fields required");
      return;
    }

    setLoading(true);
    try {
      await axios.post("/onboarding/professor/set-password", {
        userId,
        otp,
        password
      });

      const loginRes = await axios.post("/professors/login", {
        profId: userId,
        password
      });

      localStorage.setItem("token", loginRes.data.token);
      localStorage.removeItem("resetProfId");

      window.location.href = "/professor/login";
    } catch (err) {
      alert(err.response?.data?.message || "Verification failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.loginMain}>
      <div className={styles.loginCard}>
        <h2 className={styles.title}>Verify OTP</h2>

        <input
          className={styles.input}
          type="text"
          placeholder="Enter OTP"
          value={otp}
          onChange={(e) => setOtp(e.target.value)}
        />

        <input
          className={styles.input}
          type="password"
          placeholder="New Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <button
          className={styles.button}
          onClick={verifyAndSet}
          disabled={loading}
        >
          {loading ? "Verifying..." : "Set Password"}
        </button>
      </div>
    </div>
  );
}
