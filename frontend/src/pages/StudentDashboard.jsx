import { useEffect, useState } from "react";
import api from "../services/api";
import "./StudentDashboard.css";
import { useNavigate } from "react-router-dom";

export default function StudentDashboard() {
  const [activeTab, setActiveTab] = useState("attendance");
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [classes, setClasses] = useState([]);
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [classCode, setClassCode] = useState("");
  const [joinError, setJoinError] = useState("");
  const [notices, setNotices] = useState([]);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const navigate = useNavigate();

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

  //================== NOTICE BOARD ==================
  useEffect(() => {
    if (activeTab === "notice") {
      loadStudentNotices();
    }
  }, [activeTab]);

  const loadStudentNotices = async () => {
    try {
      const res = await api.get("/notices/student/my");
      setNotices(res.data);
    } catch (err) {
      console.error("Failed to load notices", err);
    }
  };

  /* ================= LOAD JOINED CLASSES ================= */
  const loadMyClasses = async () => {
    try {
      const res = await api.get("/attendance/student/my-attendance");
      setClasses(res.data);
    } catch (err) {
      console.error("Failed to load classes", err);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    delete api.defaults.headers.common["Authorization"];
    navigate("/student/login");
  };

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="spinner"></div>
        <p>Loading dashboard...</p>
      </div>
    );
  }

  // Calculate statistics correctly
  const calculateStatistics = () => {
    const totalClasses = classes.length;
    
    // Calculate individual percentages for active classes
    const activeClasses = classes.filter(c => c.totalLectures > 0);
    const totalPresent = classes.reduce((sum, c) => sum + c.presentCount, 0);
    const totalLectures = classes.reduce((sum, c) => sum + c.totalLectures, 0);
    
    // Calculate average attendance correctly
    let averageAttendance = 0;
    if (activeClasses.length > 0) {
      const sumPercentages = activeClasses.reduce((sum, c) => {
        const percentage = (c.presentCount / c.totalLectures) * 100;
        return sum + percentage;
      }, 0);
      averageAttendance = sumPercentages / activeClasses.length;
    }
    
    // Count classes below 75%
    const classesBelow75 = classes.filter(c => {
      if (c.totalLectures === 0) return false;
      const percentage = (c.presentCount / c.totalLectures) * 100;
      return percentage < 75;
    }).length;
    
    return {
      totalClasses,
      totalPresent,
      totalLectures,
      averageAttendance: averageAttendance.toFixed(1),
      classesBelow75
    };
  };

  const stats = calculateStatistics();
  
  const groupedNotices = Object.values(
    notices.reduce((acc, notice) => {
      const key = `${notice.title}-${notice.message}-${notice.createdAt}-${notice.professorName}`;

      if (!acc[key]) {
        acc[key] = {
          title: notice.title,
          message: notice.message,
          createdAt: notice.createdAt,
          professorName: notice.professorName,
          classCodes: [notice.classCode],
        };
      } else {
        acc[key].classCodes.push(notice.classCode);
      }

      return acc;
    }, {})
  ).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  return (
    <div className="student-dashboard">
      {/* ================= SIDEBAR ================= */}
      <div className="sidebar">
        <h2 className="sidebar-logo">ProfMojo</h2>

        <nav>
          <button
            className={`nav-item ${activeTab === "attendance" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("attendance");
            }}
          >
            Attendance
          </button>

          <button
            className={`nav-item ${activeTab === "notice" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("notice");
            }}
          >
            Notice Board
          </button>

          <button
            className={`nav-item ${activeTab === "profile" ? "active" : ""}`}
            onClick={() => setActiveTab("profile")}
          >
            My Profile
          </button>
        </nav>

        <div className="sidebar-footer">
          <div className="student-info-card">
            <div className="avatar">{student.name.charAt(0).toUpperCase()}</div>
            <div className="student-details">
              <strong>{student.name}</strong>
              <span>{student.regNo}</span>
            </div>
          </div>
          <button className="logout-btn" onClick={() => setShowLogoutConfirm(true)}>
            <span>Logout</span>
          </button>
        </div>
      </div>

      {/* ================= MAIN ================= */}
      <div className="main-area">
        {activeTab === "attendance" && (
          <>
            <div className="header">
              <h1>My Attendance</h1>
              <button
                className="create-btn"
                onClick={() => setShowJoinModal(true)}
              >
                + Join Class
              </button>
            </div>

            {showJoinModal && (
              <div className="modal-overlay">
                <div className="modal">
                  <h2>Join a Class</h2>
                  <input
                    value={classCode}
                    onChange={(e) => {
                      setClassCode(e.target.value);
                      setJoinError("");
                    }}
                    placeholder="Enter class code"
                    onKeyPress={(e) => e.key === 'Enter' && document.getElementById('joinBtn').click()}
                  />
                  {joinError && <p className="error-message">{joinError}</p>}
                  <div className="modal-actions">
                    <button
                      className="cancel"
                      onClick={() => {
                        setShowJoinModal(false);
                        setClassCode("");
                        setJoinError("");
                      }}
                    >
                      Cancel
                    </button>
                    <button
                      id="joinBtn"
                      className="confirm"
                      onClick={async () => {
                        if (!classCode.trim()) {
                          setJoinError("Please enter a class code");
                          return;
                        }
                        try {
                          await api.post(`/students/join/${classCode}`);
                          setShowJoinModal(false);
                          setClassCode("");
                          loadMyClasses();
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

            {classes.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">📊</div>
                <h3>No Classes Joined</h3>
                <p>Join a class to start tracking your attendance.</p>
                <button
                  className="create-btn"
                  onClick={() => setShowJoinModal(true)}
                >
                  + Join Your First Class
                </button>
              </div>
            ) : (
              <>
                <div className="attendance-summary">
                  <div>
                    <strong>Total Classes</strong>
                    <span>{stats.totalClasses}</span>
                  </div>
                  <div>
                    <strong>Average Attendance</strong>
                    <span>{stats.averageAttendance}%</span>
                  </div>
                  <div className="warning">
                    <strong>Classes Below 75%</strong>
                    <span>{stats.classesBelow75}</span>
                  </div>
                </div>

                <div className="attendance-table">
                  <div className="table-header">
                    <div className="header-cell class-col">CLASS</div>
                    <div className="header-cell">LECTURES</div>
                    <div className="header-cell">ATTENDANCE %</div>
                    <div className="header-cell">STATUS</div>
                  </div>

                  {classes.map((item, index) => {
                    const total = item.totalLectures;
                    const present = item.presentCount;
                    const percentage = total === 0 ? 0 : ((present / total) * 100).toFixed(1);
                    
                    let status = "low";
                    let statusText = "LOW";
                    if (total === 0) {
                      status = "not-started";
                      statusText = "NOT STARTED";
                    } else if (percentage >= 75) {
                      status = "good";
                      statusText = "GOOD";
                    } else if (percentage >= 60) {
                      status = "warning";
                      statusText = "WARNING";
                    }

                    return (
                      <div className="table-row" key={index}>
                        <div className="table-cell class-col">
                          <div className="class-info">
                            <h4 className="class-name">{item.className}</h4>
                            <p className="prof-name">Prof. {item.professorName}</p>
                          </div>
                        </div>
                        <div className="table-cell">
                          <span className="lecture-count">{present}/{total}</span>
                        </div>
                        <div className="table-cell">
                          <div className="percentage-display">
                            <span className="percentage-text">{percentage}%</span>
                            <div className="percentage-bar">
                              <div 
                                className={`bar-fill ${status}`}
                                style={{ width: `${Math.min(percentage, 100)}%` }}
                              ></div>
                            </div>
                          </div>
                        </div>
                        <div className="table-cell">
                          <span className={`status-pill ${status}`}>
                            {statusText}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </>
            )}
          </>
        )}

        {activeTab === "notice" && (
          <div className="notice-board">
            <div className="header">
              <h1>Notice Board</h1>
            </div>

            {groupedNotices.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">📢</div>
                <h3>No Notices Yet</h3>
                <p>Check back later for announcements from your professors.</p>
              </div>
            ) : (
              <div className="notice-list">
                {groupedNotices.map((notice, index) => (
                  <div className="notice-card" key={index}>
                    <div className="notice-header">
                      <h3>{notice.title}</h3>
                      <span className="notice-date">
                        {new Date(notice.createdAt).toLocaleDateString('en-US', {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric'
                        })}
                      </span>
                    </div>
                    <p className="notice-message">{notice.message}</p>
                    <div className="notice-footer">
                      <span className="notice-prof">
                        — Prof. {notice.professorName}
                      </span>
                      <div className="notice-classes">
                        {notice.classCodes.map((code) => (
                          <span key={code} className="class-pill">
                            {code}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === "profile" && (
          <div className="profile-section">
            <div className="header">
              <h1>My Profile</h1>
            </div>
            
            <div className="profile-card">
              <div className="profile-header">
                <div className="profile-avatar-large">
                  {student.name.charAt(0).toUpperCase()}
                </div>
                <div className="profile-title">
                  <h2>{student.name}</h2>
                  <p className="profile-role">Student</p>
                </div>
              </div>
              
              <div className="profile-details">
                <div className="detail-row">
                  <span className="detail-label">Registration Number</span>
                  <span className="detail-value">{student.regNo}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Branch</span>
                  <span className="detail-value">{student.branch || "Not specified"}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Year</span>
                  <span className="detail-value">{student.year || "Not specified"}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Contact Number</span>
                  <span className="detail-value">{student.contactNo || "Not provided"}</span>
                </div>
              </div>
              
              <div className="profile-stats-grid">
                <div className="stat-card">
                  <span className="stat-number">{stats.totalClasses}</span>
                  <span className="stat-label">Classes Joined</span>
                </div>
                <div className="stat-card">
                  <span className="stat-number">
                    {stats.totalPresent}
                  </span>
                  <span className="stat-label">Total Present</span>
                </div>
                <div className="stat-card">
                  <span className="stat-number">
                    {stats.classesBelow75}
                  </span>
                  <span className="stat-label">Need Improvement</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {showLogoutConfirm && (
          <div className="modal-overlay">
            <div className="modal logout-modal">
              <h2>Log out?</h2>
              <p>You will be signed out of your student account.</p>
              <div className="modal-actions">
                <button
                  className="cancel"
                  onClick={() => setShowLogoutConfirm(false)}
                >
                  Cancel
                </button>
                <button
                  className="confirm logout-confirm"
                  onClick={handleLogout}
                >
                  Yes, Logout
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}