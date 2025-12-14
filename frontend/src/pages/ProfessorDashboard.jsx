import { useEffect, useState } from "react";
import api from "../services/api";
import "./ProfessorDashboard.css";

export default function ProfessorDashboard() {
  const [activeTab, setActiveTab] = useState("attendance");
  const [classes, setClasses] = useState([]);
  const [students, setStudents] = useState([]);
  const [selectedClass, setSelectedClass] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newClassName, setNewClassName] = useState("");


  /* 🔹 Load professor classes */
  useEffect(() => {
    if (activeTab === "attendance") {
      loadClasses();
    }
  }, [activeTab]);

  const createClass = async () => {
    if (!newClassName.trim()) {
      alert("Class name cannot be empty");
      return;
    }

    try {
      await api.post("/professor/classes/create", {
        className: newClassName,
      });

      setNewClassName("");
      setShowCreateModal(false);
      loadClasses(); // 🔥 refresh classes
    } catch {
      alert("Failed to create class");
    }
  };


  const loadClasses = async () => {
    setLoading(true);
    try {
      const res = await api.get("/professor/classes/my");
      setClasses(res.data);
    } catch (err) {
      alert("Failed to load classes");
    } finally {
      setLoading(false);
    }
  };

  /* 🔹 Load students when class selected */
  const openClass = async (cls) => {
    setSelectedClass(cls);
    setLoading(true);
    try {
      const res = await api.get(
        `/professor/classes/${cls.classCode}/students`
      );
      setStudents(res.data);
    } catch {
      alert("Failed to load students");
    } finally {
      setLoading(false);
    }
  };

  const markAttendance = async (studentRegNo, present) => {
    try {
      await api.post("/attendance/mark", {
        classCode: selectedClass.classCode,
        studentRegNo,
        present,
      });
      alert("Attendance saved");
    } catch {
      alert("Failed to save attendance");
    }
  };

  return (
    <div className="prof-dashboard">
      {/* SIDEBAR */}
      <div className="sidebar">
        <h2 className="sidebar-logo">ProfMojo</h2>

        <button
          className={`nav-item ${activeTab === "attendance" ? "active" : ""}`}
          onClick={() => {
            setActiveTab("attendance");
            setSelectedClass(null);
          }}
        >
          Attendance
        </button>

        <button className="nav-item">Class Schedule</button>
        <button className="nav-item">Canteen</button>
        <button className="nav-item">Library</button>
        <button className="nav-item">Notice Board</button>
      </div>

      {/* MAIN CONTENT */}
      <div className="main-area">
        {activeTab === "attendance" && (
          <>
            {!selectedClass ? (
              <>
                <div className="header">
                  <h1>Your Classes</h1>
                  <button
                    className="create-btn"
                    onClick={() => setShowCreateModal(true)}
                  >
                    + Create Class
                  </button>

                  {showCreateModal && (
                    <div className="modal-overlay">
                      <div className="modal">
                        <h2 className="modal-title">Create Class</h2>

                        <input
                          type="text"
                          placeholder="Enter class name"
                          className="modal-input"
                          value={newClassName}
                          onChange={(e) => setNewClassName(e.target.value)}
                        />

                        <div className="modal-actions">
                          <button className="cancel" onClick={() => setShowCreateModal(false)}>
                            Cancel
                          </button>

                          <button className="confirm" onClick={createClass}>
                            Create
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {loading ? (
                  <p>Loading...</p>
                ) : classes.length === 0 ? (
                  <p>No classes created yet</p>
                ) : (
                  <div className="class-grid">
                    {classes.map((cls) => (
                      <div
                        key={cls.id}
                        className="class-card"
                        onClick={() => openClass(cls)}
                      >
                        {cls.className}
                      </div>
                    ))}
                  </div>
                )}
              </>
            ) : (
              <>
                <div className="header">
                  <h1>{selectedClass.className} – Attendance</h1>
                  <button
                    className="back-btn"
                    onClick={() => setSelectedClass(null)}
                  >
                    ← Back
                  </button>
                </div>

                {loading ? (
                  <p>Loading...</p>
                ) : (
                  <div className="attendance-list">
                    {students.map((enrollment) => (
                      <div
                        key={enrollment.student.regNo}
                        className="student-row"
                      >
                        <span>{enrollment.student.name}</span>

                        <div className="actions">
                          <button
                            className="present"
                            onClick={() =>
                              markAttendance(enrollment.student.regNo, true)
                            }
                          >
                            Present
                          </button>

                          <button
                            className="absent"
                            onClick={() =>
                              markAttendance(enrollment.student.regNo, false)
                            }
                          >
                            Absent
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
}
