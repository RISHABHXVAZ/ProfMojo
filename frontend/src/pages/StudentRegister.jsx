import { useState } from "react";
import axios from "../services/api";
import "./StudentRegisterAndLogin.css";

export default function StudentRegister() {
  const [data, setData] = useState({
    regNo: "",
    name: "",
    branch: "",
    year: "",
    password: "",
    contactNo: ""
  });

  const [showPassword, setShowPassword] = useState(false);
  const [idAvailable, setIdAvailable] = useState(null);

  const placeholders = {
    regNo: "Registration Number",
    name: "Name",
    branch: "Branch",
    year: "Year",
    password: "Create Password",
    contactNo: "Contact Number"
  };

  const register = async () => {
    try {
      await axios.post("/students/register", data);
      alert("Student Registered Successfully!");
      window.location.href = "/student/login";
    } catch (err) {
      alert("Registration failed: " + err.response?.data?.message);
    }
  };

  return (
    <div className="register-page">
      <div className="register-card">
        <h2 className="title">Student Registration</h2>

        {Object.keys(data).map((key) => {
          // Registration Number Check
          if (key === "regNo") {
            return (
              <div key={key} className="input-with-icon">
                <input
                  className="input-box"
                  placeholder={placeholders[key]}
                  value={data.regNo}
                  onChange={async (e) => {
                    const value = e.target.value;
                    setData({ ...data, regNo: value });

                    if (!value.trim()) {
                      setIdAvailable(null);
                      return;
                    }

                    try {
                      const res = await axios.get(`/students/check-id/${value}`);
                      setIdAvailable(res.data.canRegister);
                    } catch {
                      setIdAvailable(false);
                    }
                  }}
                />

                {idAvailable === true && (
                  <span className="valid-icon">✔</span>
                )}
                {idAvailable === false && (
                  <span className="invalid-icon">✖</span>
                )}
              </div>
            );
          }

          // Password Field
          if (key === "password") {
            return (
              <div key={key} className="password-wrapper">
                <input
                  type={showPassword ? "text" : "password"}
                  className="input-box"
                  placeholder={placeholders[key]}
                  value={data.password}
                  onChange={(e) =>
                    setData({ ...data, password: e.target.value })
                  }
                />

                {/* Eye Icon */}
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
            );
          }

          // Normal Inputs
          return (
            <input
              key={key}
              className="input-box"
              placeholder={placeholders[key]}
              value={data[key]}
              onChange={(e) =>
                setData({ ...data, [key]: e.target.value })
              }
            />
          );
        })}

        <button className="register-btn" onClick={register}>Register</button>

        <p className="login-text">
          Already registered?{" "}
          <a href="/student/login" className="login-link">Login</a>
        </p>
      </div>
    </div>
  );
}