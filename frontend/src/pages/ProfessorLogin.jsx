import { useState } from "react";
import axios from "../services/api";
import "./ProfessorLogin.css";

export default function ProfessorLogin() {

  const [profId, setProfId] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false); // NEW STATE

  const login = async () => {
    try {
      const res = await axios.post("/professors/login", { profId, password });
      localStorage.setItem("token", res.data.token);
      window.location.href = "/professor/dashboard";
    } catch (err) {
      alert("Invalid ID or password!");
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">

        <h2 className="title">Professor Login</h2>

        <input
          className="input-box"
          placeholder="Professor ID"
          value={profId}
          onChange={(e) => setProfId(e.target.value)}
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

          {/* clean black SVG eye icon */}
          <span
            className="toggle-eye"
            onClick={() => setShowPassword(!showPassword)}
          >
            {showPassword ? (
              // Eye open (SVG)
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
          <a className="register-link" href="/professor/register">
            Register here
          </a>
        </p>

      </div>
    </div>
  );
}
