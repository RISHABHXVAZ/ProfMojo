import { useState } from "react";
import axios from "../services/api";
import styles from "./ProfessorForgotPassword.module.css";

export default function ProfessorForgotPassword() {
  const [profId, setProfId] = useState("");
  const [loading, setLoading] = useState(false);

  const sendOtp = async () => {
    if (!profId.trim()) {
      alert("Please enter Professor ID");
      return;
    }

    setLoading(true);
    try {
      // ✅ await + userId
      await axios.post("/onboarding/professor/send-otp", {
        userId: profId
      });

      localStorage.setItem("resetProfId", profId);
      window.location.href = "/professor/verify-otp";
    } catch (err) {
      alert(err.response?.data?.message || "Failed to send OTP");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.loginMain}>
      <div className={styles.loginCard}>
        <h2 className={styles.title}>Reset Password</h2>

        <input
          className={styles.input}
          type="text"
          placeholder="Enter Professor ID"
          value={profId}
          onChange={(e) => setProfId(e.target.value)}
        />

        <button
          className={styles.button}
          onClick={sendOtp}
          disabled={loading}
        >
          {loading ? "Sending OTP..." : "Send OTP"}
        </button>
      </div>
    </div>
  );
}
