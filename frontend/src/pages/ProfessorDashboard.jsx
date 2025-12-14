import { useEffect, useState } from "react";
import api from "../services/api";
import "./ProfessorDashboard.css";

export default function ProfessorDashboard() {
  const [activeTab, setActiveTab] = useState("attendance");
  const [classes, setClasses] = useState([]);
  const [students, setStudents] = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [selectedClass, setSelectedClass] = useState(null);
  const [loading, setLoading] = useState(false);

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newClassName, setNewClassName] = useState("");

  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split("T")[0]
  );

  /* 🔹 Load professor classes */
  useEffect(() => {
    if (activeTab === "attendance") {
      loadClasses();
    }
  }, [activeTab]);

  const loadClasses = async () => {
    setLoading(true);
    try {
      const res = await api.get("/professor/classes/my");
      setClasses(res.data);
    } catch {
      alert("Failed to load classes");
    } finally {
      setLoading(false);
    }
  };

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
      loadClasses();
    } catch {
      alert("Failed to create class");
    }
  };

  /* 🔹 Open class */
  const openClass = async (cls) => {
    setSelectedClass(cls);
    setAttendance([]);
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

  /* 🔹 Mark attendance (Phase 1) */
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

  /* 🔹 Load attendance by date (Phase 2) */
  const loadAttendanceByDate = async (date) => {
    setLoading(true);
    try {
      const res = await api.get(
        `/attendance/${selectedClass.classCode}/date`,
        { params: { date } }
      );
      setAttendance(res.data);
    } catch {
      alert("Failed to load attendance");
    } finally {
      setLoading(false);
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

      {/* MAIN */}
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
                </div>

                {showCreateModal && (
                  <div className="modal-overlay">
                    <div className="modal">
                      <h2>Create Class</h2>
                      <input
                        value={newClassName}
                        onChange={(e) => setNewClassName(e.target.value)}
                        placeholder="Class name"
                      />
                      <div className="modal-actions">
                        <button onClick={() => setShowCreateModal(false)}>
                          Cancel
                        </button>
                        <button onClick={createClass}>Create</button>
                      </div>
                    </div>
                  </div>
                )}

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

                {/* 📅 DATE PICKER */}
                <div className="date-bar">
                  <input
                    type="date"
                    value={selectedDate}
                    onChange={(e) => setSelectedDate(e.target.value)}
                  />
                  <button onClick={() => loadAttendanceByDate(selectedDate)}>
                    View Attendance
                  </button>
                </div>

                {loading ? (
                  <p>Loading...</p>
                ) : attendance.length > 0 ? (
                  <div className="attendance-list">
                    {attendance.map((a) => (
                      <div key={a.studentRegNo} className="student-row">
                        <span>{a.studentRegNo}</span>
                        <span
                          className={a.present ? "present" : "absent"}
                        >
                          {a.present ? "Present" : "Absent"}
                        </span>
                      </div>
                    ))}
                  </div>
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
