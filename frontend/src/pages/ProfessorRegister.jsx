import { useState } from "react";
import axios from "../services/api";
import "./ProfessorRegister.css";

export default function ProfessorRegister() {

  const [data, setData] = useState({
    profId: "",
    name: "",
    email: "",
    password: "",
    department: "",
    contactNo: ""
  });

  const [showPassword, setShowPassword] = useState(false);

  const placeholders = {
    profId: "Professor ID",
    name: "Name",
    email: "Email",
    password: "Create Password",
    department: "Department",
    contactNo: "Contact Number"
  };

  const register = async () => {
    try {
      await axios.post("/professors/register", data);
      alert("Registered successfully!");
      window.location.href = "/professor/login";
    } catch (err) {
      alert("Registration failed: " + err.response?.data?.message);
    }
  };

  return (
    <div className="register-page">
      <div className="register-card">

        <h2 className="title">Professor Registration</h2>

        {/* INPUT FIELDS */}
        {Object.keys(data).map((key) =>
  key === "password" ? (
    <div key={key} className="password-wrapper">

      <input
        type={showPassword ? "text" : "password"}
        className="input-box"
        placeholder={placeholders[key]}
        value={data.password}
        onChange={(e) => setData({ ...data, password: e.target.value })}
      />

      {/* Very clean black eye icon */}
      <span
        className="toggle-eye"
        onClick={() => setShowPassword(!showPassword)}
      >
        {showPassword ? (
          // Eye open (professional SVG)
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
            <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
        ) : (
          // Eye off
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
            <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-5 0-9.27-3.11-11-7.5a11.76 11.76 0 0 1 4.26-5.22"/>
            <path d="M1 1l22 22"/>
            <path d="M9.9 9.9a3 3 0 1 0 4.24 4.24"/>
          </svg>
        )}
      </span>

    </div>
  ) : (
    <input
      key={key}
      className="input-box"
      placeholder={placeholders[key]}
      value={data[key]}
      onChange={(e) => setData({ ...data, [key]: e.target.value })}
    />
  )
)}

        <button className="register-btn" onClick={register}>
          Register
        </button>

        <p className="login-text">
          Already have an account?{" "}
          <a href="/professor/login" className="login-link">Login</a>
        </p>

      </div>
    </div>
  );
}
