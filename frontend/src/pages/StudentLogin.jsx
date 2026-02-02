import { useState } from "react";
import axios from "../services/api";
import "./StudentLogin.css";

export default function StudentLogin() {
  const [regNo, setRegNo] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const login = async () => {
    if (!regNo || !password) {
      alert("Please enter both registration number and password");
      return;
    }

    setIsLoading(true);
    try {
      const res = await axios.post("/students/login", { regNo, password });
      localStorage.setItem("token", res.data.token);
      window.location.href = "/student/dashboard";
    } catch (err) {
      alert("Invalid registration number or password!");
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter") {
      login();
    }
  };
  const goHome = () => {
    window.location.href = "/";
  };


  return (
    <div className="student-login-page">
      {/* Header */}
      <header className="login-header">
        <div className="header-content">
          <h1 className="login-logo">ProfMojo</h1>
          <p className="login-subtitle">Student Login</p>
        </div>
      </header>
      <button className="back-button" onClick={goHome}>
        ← Back to Home
      </button>


      {/* Main Content */}
      <main className="login-main">
        <div className="login-content">
          <div className="login-card">
            <div className="card-header">
              <h2>Welcome Back</h2>
              <p>Enter your credentials to access your dashboard</p>
            </div>

            <div className="login-form">
              {/* Registration Number */}
              <div className="form-group">
                <label className="form-label">
                  Registration Number <span className="required">*</span>
                </label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Enter your registration number"
                  value={regNo}
                  onChange={(e) => setRegNo(e.target.value)}
                  onKeyPress={handleKeyPress}
                  disabled={isLoading}
                />
              </div>

              {/* Password */}
              <div className="form-group">
                <label className="form-label">
                  Password <span className="required">*</span>
                </label>
                <div className="password-container">
                  <input
                    type={showPassword ? "text" : "password"}
                    className="form-input"
                    placeholder="Enter your password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onKeyPress={handleKeyPress}
                    disabled={isLoading}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword(!showPassword)}
                    disabled={isLoading}
                  >
                    {showPassword ? (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    ) : (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-5 0-9.27-3.11-11-7.5a11.76 11.76 0 0 1 4.26-5.22" />
                        <path d="M1 1l22 22" />
                      </svg>
                    )}
                  </button>
                </div>
              </div>

              {/* Login Button */}
              <button
                className="login-button"
                onClick={login}
                disabled={isLoading}
              >
                {isLoading ? (
                  <span className="button-content">
                    <svg className="spinner" viewBox="0 0 50 50">
                      <circle cx="25" cy="25" r="20" fill="none" strokeWidth="5"></circle>
                    </svg>
                    Logging in...
                  </span>
                ) : (
                  "Login"
                )}
              </button>

              <div className="register-prompt">
                <a href="/student/forgot-password" className="register-link">
                  Forgot Password?
                </a>
              </div>

            </div>
          </div>

          {/* Info Section */}
          <div className="login-info">
            <h3>Access Your Dashboard</h3>
            <div className="features">
              <div className="feature">
                <div className="feature-icon">📊</div>
                <div>
                  <h4>Attendance Tracking</h4>
                  <p>View your attendance records and statistics</p>
                </div>
              </div>
              <div className="feature">
                <div className="feature-icon">📢</div>
                <div>
                  <h4>Campus Notices</h4>
                  <p>Stay updated with important announcements</p>
                </div>
              </div>
              <div className="feature">
                <div className="feature-icon">📚</div>
                <div>
                  <h4>Academic Resources</h4>
                  <p>Access study materials and schedules</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="login-footer">
        <p>© 2024 ProfMojo Campus Management System</p>
      </footer>
    </div>
  );
}