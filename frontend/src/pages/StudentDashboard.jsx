import { useEffect, useState } from "react";
import api from "../services/api";
import "./StudentDashboard.css";

export default function StudentDashboard() {
  const [activeTab, setActiveTab] = useState("attendance");
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [attendanceData, setAttendanceData] = useState([]);
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [classCode, setClassCode] = useState("");
  const [joinError, setJoinError] = useState("");


  /* ================= LOAD STUDENT PROFILE ================= */
  useEffect(() => {
    loadStudentProfile();
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

  useEffect(() => {
    if (activeTab === "attendance") {
      loadMyAttendance();
    }
  }, [activeTab]);

  const loadMyAttendance = async () => {
    try {
      const res = await api.get("/attendance/student/my-attendance");
      setAttendanceData(res.data);
    } catch (err) {
      console.error("Failed to load attendance", err);
    }
  };


  if (loading) return <p className="loading">Loading...</p>;

  return (
    <div className="student-dashboard">
      {/* ================= SIDEBAR ================= */}
      <div className="student-sidebar">
        <h2 className="sidebar-logo">ProfMojo</h2>

        <div className="profile-box">
          <div className="avatar">
            {student.name.charAt(0)}
          </div>
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

          <button
            className={activeTab === "schedule" ? "active" : ""}
            onClick={() => setActiveTab("schedule")}
          >
            Class Schedule
          </button>

          <button
            className={activeTab === "library" ? "active" : ""}
            onClick={() => setActiveTab("library")}
          >
            Library
          </button>

          <button
            className={activeTab === "notice" ? "active" : ""}
            onClick={() => setActiveTab("notice")}
          >
            Notice Board
          </button>
        </div>
      </div>

      {/* ================= MAIN AREA ================= */}
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
              {attendanceData.map((item, index) => {
                const status =
                  item.percentage >= 75
                    ? "good"
                    : item.percentage >= 60
                      ? "warning"
                      : "low";

                return (
                  <div className="attendance-row" key={index}>
                    {/* LEFT: CLASS + PROFESSOR */}
                    <div className="class-info">
                      <div className="class-name">{item.className}</div>
                      <div className="prof-name">
                        Prof. {item.professorName}
                      </div>
                    </div>

                    {/* ATTENDED */}
                    <div className="count">
                      {item.presentCount}/{item.totalLectures}
                    </div>

                    {/* PERCENTAGE */}
                    <div className="percentage">
                      {item.percentage}%
                    </div>

                    {/* STATUS */}
                    <div className={`status-pill ${status}`}>
                      {status.toUpperCase()}
                    </div>
                  </div>
                );
              })}
            </div>
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
                          loadMyAttendance(); // refresh data
                        } catch (err) {
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



        {activeTab === "schedule" && (
          <div className="page">
            <h1>Class Schedule</h1>
            <p>Your weekly timetable will appear here.</p>
          </div>
        )}

        {activeTab === "library" && (
          <div className="page">
            <h1>Library</h1>
            <p>Check book availability and locations.</p>
          </div>
        )}

        {activeTab === "notice" && (
          <div className="page">
            <h1>Notice Board</h1>
            <p>Important announcements will appear here.</p>
          </div>
        )}
      </div>
    </div>
  );
}
