import { useState } from "react";
import axios from "../services/api";
import "./StudentRegisterAndLogin.css";

export default function StudentLogin() {
  const [regNo, setRegNo] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const login = async () => {
    try {
      const res = await axios.post("/students/login", { regNo, password });
      localStorage.setItem("token", res.data.token);
      window.location.href = "/student/dashboard";
    } catch {
      alert("Invalid Registration Number or Password");
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <h2 className="title">Student Login</h2>

        <input
          className="input-box"
          placeholder="Registration Number"
          value={regNo}
          onChange={(e) => setRegNo(e.target.value)}
        />

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
              <svg width="20" height="20" stroke="#000" fill="none" viewBox="0 0 24 24">
                <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            ) : (
              <svg width="20" height="20" stroke="#000" fill="none" viewBox="0 0 24 24">
                <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-5 0-9.27-3.11-11-7.5a11.76 11.76 0 0 1 4.26-5.22" />
                <path d="M1 1l22 22" />
              </svg>
            )}
          </span>
        </div>

        <button className="login-btn" onClick={login}>Login</button>

        <p className="register-text">
          Not registered?{" "}
          <a href="/student/register" className="register-link">Register here</a>
        </p>
      </div>
    </div>
  );
}
