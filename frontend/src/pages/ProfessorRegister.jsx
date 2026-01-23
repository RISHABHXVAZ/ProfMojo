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
  const [idAvailable, setIdAvailable] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [checkingId, setCheckingId] = useState(false);

  const departments = [
    "CSE - Computer Science",
    "ECE - Electronics & Communication", 
    "ME - Mechanical Engineering",
    "CE - Civil Engineering",
    "EE - Electrical Engineering",
    "IT - Information Technology"
  ];

  const checkIdAvailability = async (id) => {
    if (!id.trim()) {
      setIdAvailable(null);
      return;
    }

    setCheckingId(true);
    try {
      const res = await axios.get(`/professors/check-id/${id}`);
      setIdAvailable(res.data.canRegister);
    } catch {
      setIdAvailable(false);
    } finally {
      setCheckingId(false);
    }
  };

  const register = async () => {
    for (const key in data) {
      if (!data[key].trim()) {
        alert(`Please fill in ${key === "profId" ? "Professor ID" : key}`);
        return;
      }
    }

    if (idAvailable === false) {
      alert("Professor ID is already taken. Please choose another.");
      return;
    }

    if (data.password.length < 6) {
      alert("Password must be at least 6 characters long");
      return;
    }

    setIsLoading(true);
    try {
      await axios.post("/professors/register", data);
      alert("Registration successful! You can now login.");
      window.location.href = "/professor/login";
    } catch (err) {
      alert("Registration failed: " + (err.response?.data?.message || "Please try again."));
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter") {
      register();
    }
  };

  return (
    <div className="register-page">
      <div className="register-container">
        <header className="register-header">
          <h1 className="brand-title">ProfMojo</h1>
          <p className="brand-sub">Professor Registration</p>
          <h2 className="section-title">Create Your Account</h2>
          <p className="section-desc">Enter your details to register as a professor</p>
        </header>

        <div className="register-form-grid">
          {/* Professor ID with Availability Check */}
          <div className="input-group">
            <label>Professor ID *</label>
            <div className="status-input-wrapper">
              <input
                type="text"
                placeholder="Enter ID"
                value={data.profId}
                onChange={(e) => {
                  const value = e.target.value;
                  setData({ ...data, profId: value });
                  checkIdAvailability(value);
                }}
                disabled={isLoading}
              />
              <div className="status-indicator">
                {checkingId && <div className="spinner-mini"></div>}
                {!checkingId && idAvailable === true && <span className="valid-icon">✓</span>}
                {!checkingId && idAvailable === false && <span className="invalid-icon">✗</span>}
              </div>
            </div>
            {idAvailable === true && <p className="input-hint success">ID is available</p>}
            {idAvailable === false && <p className="input-hint error">ID is already taken</p>}
          </div>

          <div className="input-group">
            <label>Department *</label>
            <select
              value={data.department}
              onChange={(e) => setData({ ...data, department: e.target.value })}
              disabled={isLoading}
            >
              <option value="">Select Department</option>
              {departments.map((dept) => (
                <option key={dept} value={dept.split(" - ")[0]}>
                  {dept}
                </option>
              ))}
            </select>
          </div>

          <div className="input-group">
            <label>Full Name *</label>
            <input
              type="text"
              placeholder="John Doe"
              value={data.name}
              onChange={(e) => setData({ ...data, name: e.target.value })}
              disabled={isLoading}
            />
          </div>

          <div className="input-group">
            <label>Password *</label>
            <div className="password-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                placeholder="••••••••••"
                value={data.password}
                onKeyPress={handleKeyPress}
                onChange={(e) => setData({ ...data, password: e.target.value })}
                disabled={isLoading}
              />
              <span className="toggle-btn" onClick={() => setShowPassword(!showPassword)}>
                {showPassword ? "Hide" : "Show"}
              </span>
            </div>
          </div>

          <div className="input-group">
            <label>Email Address *</label>
            <input
              type="email"
              placeholder="ADM-CSE"
              value={data.email}
              onChange={(e) => setData({ ...data, email: e.target.value })}
              disabled={isLoading}
            />
          </div>

          <div className="input-group">
            <label>Contact Number *</label>
            <input
              type="tel"
              placeholder="+91"
              value={data.contactNo}
              onChange={(e) => setData({ ...data, contactNo: e.target.value })}
              disabled={isLoading}
            />
          </div>
        </div>

        <div className="terms-section">
          <label>
            <input type="checkbox" required />
            <span>I agree to the Terms and Privacy Policy</span>
          </label>
        </div>

        <div className="form-actions">
          <button className="main-submit-btn" onClick={register} disabled={isLoading || checkingId}>
            {isLoading ? "Creating Account..." : "Create Account"}
          </button>
          <p className="footer-link">
            Already have an account? <a href="/professor/login">Login here</a>
          </p>
        </div>
      </div>
    </div>
  );
}