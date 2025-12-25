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
  const [summary, setSummary] = useState(null);
  const [showAnalytics, setShowAnalytics] = useState(false);
  const [studentAnalytics, setStudentAnalytics] = useState([]);

  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [createdClass, setCreatedClass] = useState(null);

  const [showNoticeModal, setShowNoticeModal] = useState(false);
  const [noticeTitle, setNoticeTitle] = useState("");
  const [noticeMessage, setNoticeMessage] = useState("");
  const [selectedClasses, setSelectedClasses] = useState([]);
  const [showNoticeSuccess, setShowNoticeSuccess] = useState(false);
  const [noticeSentClasses, setNoticeSentClasses] = useState([]);



  const [notices, setNotices] = useState([]);

  useEffect(() => {
    if (activeTab === "notice") {
      loadProfessorNotices();
    }
  }, [activeTab]);




  const [markedMap, setMarkedMap] = useState({});

  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split("T")[0]
  );

  /* ================= LOAD CLASSES ================= */
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

  const loadStudentAnalytics = async () => {
    try {
      const res = await api.get(
        `/attendance/${selectedClass.classCode}/students/summary`
      );
      setStudentAnalytics(res.data);
    } catch (err) {
      console.error("Failed to load analytics", err);
    }
  };


  /* ================= SUMMARY ================= */
  const loadSummary = async (classCode) => {
    const res = await api.get(`/attendance/${classCode}/summary`);
    setSummary(res.data);
  };

  /* ================= CREATE CLASS ================= */
  const createClass = async () => {
    if (!newClassName.trim()) return;

    try {
      const res = await api.post("/professor/classes/create", {
        className: newClassName,
      });

      setCreatedClass(res.data);      // { className, classCode }
      setShowCreateModal(false);
      setShowSuccessModal(true);
      setNewClassName("");

      loadClasses(); // refresh class list
    } catch {
      alert("Failed to create class");
    }
  };


  /* ================= OPEN CLASS ================= */
  const openClass = async (cls) => {
    setSelectedClass(cls);
    setAttendance([]);
    setMarkedMap({});
    setLoading(true);

    try {
      await loadSummary(cls.classCode);

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

  /* ================= LOAD ATTENDANCE ================= */
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

  /* ================= UNDO ================= */
  const undoAttendance = (studentRegNo) => {
    const entry = markedMap[studentRegNo];
    if (!entry) return;

    clearTimeout(entry.timeoutId);

    setMarkedMap((prev) => {
      const updated = { ...prev };
      delete updated[studentRegNo];
      return updated;
    });
  };

  /* ================= MARK ATTENDANCE ================= */
  const markAttendance = async (studentRegNo, present) => {
    try {
      // 1️⃣ Save attendance
      await api.post("/attendance/mark", {
        classCode: selectedClass.classCode,
        studentRegNo,
        present,
        attendanceDate: selectedDate,
      });

      // 2️⃣ Reload summary ONLY
      const summaryRes = await api.get(
        `/attendance/${selectedClass.classCode}/summary`
      );
      setSummary(summaryRes.data);

      // 3️⃣ Undo timer
      const timeoutId = setTimeout(() => {
        setMarkedMap((prev) => ({
          ...prev,
          [studentRegNo]: {
            ...prev[studentRegNo],
            undoable: false,
          },
        }));
      }, 5000);

      // 4️⃣ UI state
      setMarkedMap((prev) => ({
        ...prev,
        [studentRegNo]: {
          status: present,
          undoable: true,
          timeoutId,
        },
      }));
    } catch (err) {
      console.error("Attendance save failed", err);
    }
  };

  const deleteClass = async () => {
    const confirmDelete = window.confirm(
      "Are you sure you want to delete this class?\nThis will remove all students and attendance permanently."
    );

    if (!confirmDelete) return;

    try {
      await api.delete(`/professors/${selectedClass.classCode}`);

      setSelectedClass(null);
      setStudents([]);
      setAttendance([]);
      setSummary(null);
      setMarkedMap({});
      loadClasses(); // refresh class list
    } catch (err) {
      alert("Failed to delete class");
    }
  };

  //=================NOTICE BOARD MODAL=================
  const toggleClassSelection = (code) => {
    setSelectedClasses((prev) =>
      prev.includes(code)
        ? prev.filter((c) => c !== code)
        : [...prev, code]
    );
  };

  const selectAllClasses = () => {
    setSelectedClasses(classes.map((c) => c.classCode));
  };

  const loadProfessorNotices = async () => {
    try {
      const res = await api.get("/notices/professor/my");
      setNotices(res.data);
    } catch (err) {
      console.error("Failed to load notices", err);
    }
  };

  const publishNotice = async () => {
    if (!noticeTitle || !noticeMessage || selectedClasses.length === 0) {
      return;
    }

    try {
      await api.post("/notices/create", {
        title: noticeTitle,
        message: noticeMessage,
        classCodes: selectedClasses,
      });

      // store classes for confirmation modal
      setNoticeSentClasses(
        classes
          .filter((c) => selectedClasses.includes(c.classCode))
          .map((c) => c.className)
      );

      setShowNoticeModal(false);
      setShowNoticeSuccess(true);

      setNoticeTitle("");
      setNoticeMessage("");
      setSelectedClasses([]);

      loadProfessorNotices(); // refresh list
    } catch {

    }
  };

  const groupedNotices = notices.reduce((acc, notice) => {
    const key = `${notice.title}-${notice.message}-${notice.createdAt}`;

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
  }, {});



  return (
    <div className="prof-dashboard">
      {/* ================= SIDEBAR ================= */}
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
        <button
          className={`nav-item ${activeTab === "canteen" ? "active" : ""}`}
          onClick={() => setActiveTab("canteen")}
        >
          Canteen
        </button>

        <button className="nav-item">Library</button>
        <button
          className={`nav-item ${activeTab === "notice" ? "active" : ""}`}
          onClick={() => {
            setActiveTab("notice");
            setSelectedClass(null); // reset attendance view
          }}
        >
          Notice Board
        </button>
      </div>

      {/* ================= MAIN ================= */}
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
                        <button
                          className="cancel"
                          onClick={() => setShowCreateModal(false)}
                        >
                          Cancel
                        </button>
                        <button className="confirm" onClick={createClass}>
                          Create
                        </button>
                      </div>
                    </div>
                  </div>
                )}
                {showSuccessModal && createdClass && (
                  <div className="modal-overlay">
                    <div className="modal">
                      <h2>Class Created Successfully</h2>

                      <p>
                        <strong>Class Name:</strong> {createdClass.className}
                      </p>

                      <div className="class-code-box">
                        <span>{createdClass.classCode}</span>
                        <button
                          onClick={() =>
                            navigator.clipboard.writeText(createdClass.classCode)
                          }
                        >
                          Copy
                        </button>
                      </div>

                      <p className="hint">
                        Share this class code with students to join
                      </p>

                      <button
                        className="confirm"
                        onClick={() => setShowSuccessModal(false)}
                      >
                        Done
                      </button>
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
                <div className="header class-header">
                  <div>
                    <h1>{selectedClass.className}</h1>

                    <div className="class-code-inline">
                      <span className="label">Class Code:</span>
                      <span className="code">{selectedClass.classCode}</span>
                      <button
                        className="copy-btn"
                        onClick={() =>
                          navigator.clipboard.writeText(selectedClass.classCode)
                        }
                      >
                        Copy
                      </button>
                    </div>
                  </div>

                  <div className="header-actions">
                    <button
                      className="delete-btn"
                      onClick={deleteClass}
                    >
                      Delete Class
                    </button>

                    <button
                      className="back-btn"
                      onClick={() => setSelectedClass(null)}
                    >
                      ← Back
                    </button>
                  </div>
                </div>



                {summary && (
                  <div className="attendance-summary">
                    <div>
                      <strong>Total Lectures</strong>
                      <span>{summary.totalLectures}</span>
                    </div>

                    <div>
                      <strong>Avg Attendance</strong>
                      <span>{summary.averageAttendance}%</span>
                    </div>

                    <div className="warning">
                      <strong>Low Attendance</strong>
                      <span>{summary.lowAttendanceCount}</span>
                    </div>
                  </div>
                )}
                <div className="attendance-toggle">
                  <button
                    className={!showAnalytics ? "active" : ""}
                    onClick={() => setShowAnalytics(false)}
                  >
                    Mark Attendance
                  </button>

                  <button
                    className={showAnalytics ? "active" : ""}
                    onClick={() => {
                      setShowAnalytics(true);
                      loadStudentAnalytics();
                    }}
                  >
                    📊 View Analytics
                  </button>
                </div>


                <div className="attendance-toolbar">
                  <div className="date-picker">
                    <label>Attendance Date</label>
                    <input
                      type="date"
                      value={selectedDate}
                      onChange={(e) => setSelectedDate(e.target.value)}
                    />
                  </div>

                  <button
                    className="view-btn"
                    onClick={() => loadAttendanceByDate(selectedDate)}
                  >
                    View Attendance
                  </button>
                </div>

                <div className="attendance-card">
                  {showAnalytics ? (
                    /* ================= ANALYTICS TABLE ================= */
                    <table className="analytics-table">
                      <thead>
                        <tr>
                          <th>#</th>
                          <th>Reg No</th>
                          <th>Total Lectures</th>
                          <th>Present</th>
                          <th>Attendance %</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {studentAnalytics.map((s, index) => (
                          <tr key={s.studentRegNo}>
                            <td>{index + 1}</td>
                            <td>{s.studentRegNo}</td>
                            <td>{s.totalLectures}</td>
                            <td>{s.presentCount}</td>
                            <td>{s.attendancePercentage.toFixed(1)}%</td>
                            <td>
                              <span
                                className={`status-pill ${s.lowAttendance ? "absent" : "present"
                                  }`}
                              >
                                {s.lowAttendance ? "Low" : "OK"}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  ) : (
                    /* ================= MARK ATTENDANCE (UNCHANGED) ================= */
                    attendance.length > 0 ? (
                      attendance.map((a) => (
                        <div key={a.studentRegNo} className="student-row">
                          <span>{a.studentRegNo}</span>
                          <span
                            className={`status-pill ${a.present ? "present" : "absent"}`}
                          >
                            {a.present ? "Present" : "Absent"}
                          </span>
                        </div>
                      ))
                    ) : students.length > 0 ? (
                      students.map((enrollment, index) => {
                        const student = enrollment.student;
                        const marked = markedMap[student.regNo];

                        return (
                          <div key={student.regNo} className="student-row grid-row">
                            <div className="col-serial">{index + 1}</div>

                            <div className="student-info">
                              <div className="avatar">{student.name.charAt(0)}</div>
                              <span className="student-name">{student.name}</span>
                            </div>

                            <div className="reg-no">{student.regNo}</div>

                            <div className="actions">
                              {marked ? (
                                <div className="marked-box">
                                  <span
                                    className={`status-pill ${marked.status ? "present" : "absent"
                                      }`}
                                  >
                                    {marked.status
                                      ? "Marked: Present"
                                      : "Marked: Absent"}
                                  </span>

                                  {marked.undoable && (
                                    <button
                                      className="undo-btn"
                                      onClick={() => undoAttendance(student.regNo)}
                                    >
                                      Undo
                                    </button>
                                  )}
                                </div>
                              ) : (
                                <>
                                  <button
                                    className="present"
                                    onClick={() => markAttendance(student.regNo, true)}
                                  >
                                    Present
                                  </button>
                                  <button
                                    className="absent"
                                    onClick={() => markAttendance(student.regNo, false)}
                                  >
                                    Absent
                                  </button>
                                </>
                              )}
                            </div>
                          </div>
                        );
                      })
                    ) : (
                      <p className="empty">No students joined yet</p>
                    )

                  )}
                </div>

              </>
            )}
          </>
        )}

        {activeTab === "notice" && (
          <div className="notice-board">
            <div className="header">
              <h1>Notice Board</h1>
              <button
                className="create-btn"
                onClick={() => setShowNoticeModal(true)}
              >
                + Create Notice
              </button>
            </div>

            {/* ================= NOTICE LIST ================= */}
            {Object.values(groupedNotices).length === 0 ? (
              <p className="empty">No notices published yet</p>
            ) : (
              <div className="notice-list">
                {Object.values(groupedNotices).map((notice, index) => (
                  <div key={index} className="notice-card">
                    <div className="notice-header">
                      <h3>{notice.title}</h3>
                      <span className="notice-date">
                        {new Date(notice.createdAt).toLocaleDateString()}
                      </span>
                    </div>

                    <p className="notice-message">{notice.message}</p>

                    <div className="notice-footer">
                      <span className="notice-prof">
                        — {notice.professorName}
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



            {showNoticeModal && (
              <div className="modal-overlay">
                <div className="modal">
                  <h2>Create Notice</h2>

                  <input
                    placeholder="Title"
                    value={noticeTitle}
                    onChange={(e) => setNoticeTitle(e.target.value)}
                  />

                  <textarea
                    placeholder="Write your notice..."
                    value={noticeMessage}
                    onChange={(e) => setNoticeMessage(e.target.value)}
                  />

                  <div className="class-select">
                    <div className="class-select-header">
                      <strong>Select Classes</strong>
                      <button className="select-all" onClick={selectAllClasses}>
                        Select All
                      </button>
                    </div>

                    <div className="class-list">
                      {classes.map((cls) => (
                        <label key={cls.classCode}>
                          <input
                            type="checkbox"
                            checked={selectedClasses.includes(cls.classCode)}
                            onChange={() => toggleClassSelection(cls.classCode)}
                          />
                          {cls.className}
                        </label>
                      ))}
                    </div>
                  </div>




                  <div className="modal-actions">
                    <button className="cancel" onClick={() => setShowNoticeModal(false)}>
                      Cancel
                    </button>
                    <button className="confirm" onClick={publishNotice}>
                      Publish
                    </button>
                  </div>
                </div>
              </div>
            )}
            {showNoticeSuccess && (
              <div className="modal-overlay">
                <div className="modal">
                  <h2>Notice Sent ✅</h2>

                  <p>This notice has been sent to:</p>

                  <div className="sent-classes">
                    {noticeSentClasses.map((cls) => (
                      <span key={cls} className="class-pill">
                        {cls}
                      </span>
                    ))}
                  </div>

                  <button
                    className="confirm"
                    onClick={() => setShowNoticeSuccess(false)}
                  >
                    Done
                  </button>
                </div>
              </div>
            )}

          </div>
        )}

      </div>
    </div>

  );
}