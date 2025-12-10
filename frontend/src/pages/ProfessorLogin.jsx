import { useState } from "react";
import axios from "../services/api";
import "./ProfessorLogin.css";

export default function ProfessorLogin() {
  const [profId, setProfId] = useState("");
  const [password, setPassword] = useState("");

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

        <input
          className="input-box"
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <button className="login-btn" onClick={login}>
          Login
        </button>

        <p className="register-text">
          Not registered?{" "}
          <a className="register-link" href="/professor/register">Register here</a>
        </p>

      </div>
    </div>
  );
}
