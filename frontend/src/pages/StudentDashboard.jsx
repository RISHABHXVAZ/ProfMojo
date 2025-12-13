import { useEffect, useState } from "react";
import axios from "../services/api";
import "./StudentDashboard.css";

export default function StudentDashboard() {
  const [student, setStudent] = useState(null);

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const res = await axios.get("/students/me");
        setStudent(res.data);
      } catch (err) {
        alert("Session expired. Please login again.");
        window.location.href = "/student/login";
      }
    };

    loadProfile();
  }, []);

  const logout = () => {
    localStorage.removeItem("token");
    window.location.href = "/student/login";
  };

  if (!student) return <div className="loading">Loading...</div>;

  return (
    <div className="dashboard-container">
      <div className="sidebar">
        <h2 className="sidebar-title">Student Panel</h2>

        <div className="sidebar-menu">
          <button className="sidebar-item active">Dashboard</button>
          <button className="sidebar-item">Profile</button>
          <button className="sidebar-item">Notifications</button>
        </div>

        <button className="logout-btn" onClick={logout}>
          Logout
        </button>
      </div>

      {/* Main content */}
      <div className="main-content">
        <h1 className="welcome-text">Welcome, {student.name} 👋</h1>

        <div className="info-card">
          <p><strong>Registration No:</strong> {student.regNo}</p>
          <p><strong>Branch:</strong> {student.branch}</p>
          <p><strong>Year:</strong> {student.year}</p>
          <p><strong>Contact:</strong> {student.contactNo}</p>
        </div>
      </div>
    </div>
  );
}
