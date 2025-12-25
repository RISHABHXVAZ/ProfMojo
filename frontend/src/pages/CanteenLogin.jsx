import { useState } from "react";
import api from "../services/api";
import "./ProfessorLogin.css"; // reuse same CSS

export default function CanteenLogin() {
  const [canteenId, setCanteenId] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");

  const login = async () => {
    try {
      const res = await api.post("/canteen/login", {
        canteenId,
        password,
      });

      localStorage.setItem("token", res.data.token);
      localStorage.setItem("role", "CANTEEN");

      window.location.href = "/canteen/dashboard";
    } catch {
      setError("Invalid canteen ID or password");
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">

        <h2 className="title">Canteen Login</h2>

        {error && <p style={{ color: "red", marginBottom: "10px" }}>{error}</p>}

        <input
          className="input-box"
          placeholder="Canteen ID"
          value={canteenId}
          onChange={(e) => setCanteenId(e.target.value)}
        />

        {/* PASSWORD WITH EYE ICON */}
        <div className="password-wrapper">
          <input
            className="input-box"
            type={showPassword ? "text" : "password"}
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
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

        <button className="login-btn" onClick={login}>
          Login
        </button>

        <p className="register-text">
          Not registered?{" "}
          <a className="register-link" href="/canteen/register">
            Register here
          </a>
        </p>

      </div>
    </div>
  );
}
