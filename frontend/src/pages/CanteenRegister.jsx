import { useState } from "react";
import api from "../services/api";
import "./ProfessorRegister.css"; // reuse same CSS

export default function CanteenRegister() {
  const [canteenId, setCanteenId] = useState("");
  const [canteenName, setCanteenName] = useState("");
  const [password, setPassword] = useState("");
  const [contactNo, setContactNo] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      await api.post("/canteen/register", {
        canteenId,
        canteenName,
        password,
        contactNo,
      });
      setSuccess(true);
      setError("");
    } catch {
      setError("Canteen ID not found or already registered");
    }
  };

  /* ================= SUCCESS STATE ================= */
  if (success) {
    return (
      <div className="register-page">
        <div className="register-card">
          <h2 className="title">Registration Successful ✅</h2>
          <p>You can now login as a canteen receptionist.</p>

          <a href="/canteen/login">
            <button className="register-btn" style={{ marginTop: "20px" }}>
              Go to Login
            </button>
          </a>
        </div>
      </div>
    );
  }

  /* ================= REGISTER FORM ================= */
  return (
    <div className="register-page">
      <form className="register-card" onSubmit={handleRegister}>
        <h2 className="title">Canteen Registration</h2>

        {error && (
          <p style={{ color: "red", marginBottom: "12px" }}>{error}</p>
        )}

        <input
          className="input-box"
          placeholder="Canteen ID"
          value={canteenId}
          onChange={(e) => setCanteenId(e.target.value)}
          required
        />

        <input
          className="input-box"
          placeholder="Canteen Name"
          value={canteenName}
          onChange={(e) => setCanteenName(e.target.value)}
          required
        />

        <input
          className="input-box"
          placeholder="Contact Number"
          value={contactNo}
          onChange={(e) => setContactNo(e.target.value)}
          required
        />

        {/* PASSWORD WITH EYE ICON */}
        <div className="password-wrapper">
          <input
            className="input-box"
            type={showPassword ? "text" : "password"}
            placeholder="Set Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <span
            className="toggle-eye"
            onClick={() => setShowPassword(!showPassword)}
          >
            {showPassword ? (
              // Eye open
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#000"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            ) : (
              // Eye closed
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#000"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-5 0-9.27-3.11-11-7.5a11.76 11.76 0 0 1 4.26-5.22" />
                <path d="M1 1l22 22" />
              </svg>
            )}
          </span>
        </div>

        <button className="register-btn" type="submit">
          Register
        </button>

        <p className="login-text">
          Already registered?{" "}
          <a className="login-link" href="/canteen/login">
            Login
          </a>
        </p>
      </form>
    </div>
  );
}
