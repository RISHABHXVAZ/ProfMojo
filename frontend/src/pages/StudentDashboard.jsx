import { useEffect, useState } from "react";
import api from "../services/api";
import "./StudentDashboard.css";

export default function StudentDashboard() {
  const [activeTab, setActiveTab] = useState("attendance");
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [classes, setClasses] = useState([]);   // ✅ SINGLE SOURCE
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [classCode, setClassCode] = useState("");
  const [joinError, setJoinError] = useState("");

  /* ================= LOAD STUDENT PROFILE ================= */
  useEffect(() => {
    loadStudentProfile();
    loadMyClasses();
  }, []);

  const loadStudentProfile = async () => {
    try {
      const res = await api.get("/students/me");
      setStudent(res.data);
    } catch (err) {
      console.error("Failed to load student profile", err);
    } finally {
      setLoading(false);
    }
  };

  /* ================= LOAD JOINED CLASSES ================= */
const loadMyClasses = async () => {
  try {
    const res = await api.get("/attendance/student/my-classes");
    console.log("MY CLASSES:", res.data); // 👈 ADD THIS
    setClasses(res.data);
  } catch (err) {
    console.error("Failed to load classes", err);
  }
};

  if (loading) return <p className="loading">Loading...</p>;

  return (
    <div className="student-dashboard">
      {/* ================= SIDEBAR ================= */}
      <div className="student-sidebar">
        <h2 className="sidebar-logo">ProfMojo</h2>

        <div className="profile-box">
          <div className="avatar">{student.name.charAt(0)}</div>
          <div className="info">
            <strong>{student.name}</strong>
            <span>{student.regNo}</span>
          </div>
        </div>

        <div className="student-nav">
          <button
            className={activeTab === "attendance" ? "active" : ""}
            onClick={() => setActiveTab("attendance")}
          >
            My Attendance
          </button>
          <button onClick={() => setActiveTab("schedule")}>Class Schedule</button>
          <button onClick={() => setActiveTab("library")}>Library</button>
          <button onClick={() => setActiveTab("notice")}>Notice Board</button>
        </div>
      </div>

      {/* ================= MAIN ================= */}
      <div className="main-area">
        {activeTab === "attendance" && (
          <div className="student-main">
            <div className="student-header">
              <h1>My Attendance</h1>
              <button
                className="join-class-btn"
                onClick={() => setShowJoinModal(true)}
              >
                + Join Class
              </button>
            </div>

            <div className="attendance-list">
              <div className="attendance-header">
                <span>Class</span>
                <span>Lectures</span>
                <span>Attendance %</span>
                <span>Status</span>
              </div>

              {classes.map((item, index) => {
                const percentage =
                  item.totalLectures === 0
                    ? 0
                    : item.percentage.toFixed(1);

                const status =
                  percentage >= 75
                    ? "good"
                    : percentage >= 60
                    ? "warning"
                    : "low";

                return (
                  <div className="attendance-row" key={index}>
                    <div className="class-info">
                      <div className="class-name">{item.className}</div>
                      <div className="prof-name">
                        Prof. {item.professorName}
                      </div>
                    </div>

                    <div className="count">
                      {item.presentCount}/{item.totalLectures}
                    </div>

                    <div className="percentage">{percentage}%</div>

                    <div className={`status-pill ${status}`}>
                      {item.totalLectures === 0
                        ? "NOT STARTED"
                        : status.toUpperCase()}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* ================= JOIN MODAL ================= */}
            {showJoinModal && (
              <div className="modal-backdrop">
                <div className="modal">
                  <h3>Join a Class</h3>

                  <input
                    type="text"
                    placeholder="Enter Class Code"
                    value={classCode}
                    onChange={(e) => setClassCode(e.target.value)}
                  />

                  {joinError && <p className="error">{joinError}</p>}

                  <div className="modal-actions">
                    <button
                      className="btn-secondary"
                      onClick={() => {
                        setShowJoinModal(false);
                        setClassCode("");
                        setJoinError("");
                      }}
                    >
                      Cancel
                    </button>

                    <button
                      className="btn-primary"
                      onClick={async () => {
                        try {
                          await api.post(`/students/join/${classCode}`);
                          setShowJoinModal(false);
                          setClassCode("");
                          loadMyClasses(); // ✅ refresh
                        } catch {
                          setJoinError("Invalid or already joined class code");
                        }
                      }}
                    >
                      Join
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
