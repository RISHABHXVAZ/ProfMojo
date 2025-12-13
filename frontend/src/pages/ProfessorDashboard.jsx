import { useState } from "react";
import "./ProfessorDashboard.css";

export default function ProfessorDashboard() {
  const [activeTab, setActiveTab] = useState("attendance");
  const [classes, setClasses] = useState(["C Section", "D Section"]);
  const [selectedClass, setSelectedClass] = useState(null);

  const students = [
    "Aarav Sharma",
    "Ananya Verma",
    "Kunal Singh",
    "Rishabh Srivastava",
    "Sneha Patel"
  ];

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
                  <button className="create-btn">+ Create Class</button>
                </div>

                <div className="class-grid">
                  {classes.map((cls) => (
                    <div
                      key={cls}
                      className="class-card"
                      onClick={() => setSelectedClass(cls)}
                    >
                      {cls}
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <>
                <div className="header">
                  <h1>{selectedClass} – Attendance</h1>
                  <button
                    className="back-btn"
                    onClick={() => setSelectedClass(null)}
                  >
                    ← Back
                  </button>
                </div>

                <div className="attendance-list">
                  {students.map((student) => (
                    <div key={student} className="student-row">
                      <span>{student}</span>
                      <div className="actions">
                        <button className="present">Present</button>
                        <button className="absent">Absent</button>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
}
