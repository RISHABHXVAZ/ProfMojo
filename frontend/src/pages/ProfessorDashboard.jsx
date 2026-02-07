import { useEffect, useState } from "react";
import api from "../services/api";
import "./ProfessorDashboard.css";
import { useNavigate } from "react-router-dom";
import { Flag, AlertTriangle, UserX, Key, Copy } from "lucide-react";

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
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [isViewingAttendance, setIsViewingAttendance] = useState(false);

  // ===== AMENITIES =====
  const [amenityRequests, setAmenityRequests] = useState([]);
  const [showAmenityModal, setShowAmenityModal] = useState(false);
  const [department, setDepartment] = useState("");
  const [classroom, setClassroom] = useState("");
  const [items, setItems] = useState([]);
  const [itemInput, setItemInput] = useState("");
  const [showAmenityHistory, setShowAmenityHistory] = useState(false);
  const [amenityHistory, setAmenityHistory] = useState([]);
  const [now, setNow] = useState(Date.now());

  const [slaChecks, setSlaChecks] = useState({});
  const [reRequesting, setReRequesting] = useState(false);
  const [reRequestModal, setReRequestModal] = useState(false);
  const [selectedRequestForReRequest, setSelectedRequestForReRequest] = useState(null);
  const [refreshCounter, setRefreshCounter] = useState(0);

  // NEW: Staff reporting
  const [showReportModal, setShowReportModal] = useState(false);
  const [selectedRequestForReport, setSelectedRequestForReport] = useState(null);
  const [reportReason, setReportReason] = useState("");
  const [reporting, setReporting] = useState(false);

  // NEW: Delivery SLA breach
  const [deliverySlaChecks, setDeliverySlaChecks] = useState({});

  // NEW: Confirmation code modal state
  const [showConfirmationModal, setShowConfirmationModal] = useState(false);
  const [confirmationRequest, setConfirmationRequest] = useState(null);

  useEffect(() => {
    const interval = setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    delete api.defaults.headers.common["Authorization"];
    navigate("/professor/login");
  };

  // UPDATED: Better SLA formatting
  const formatRemaining = (targetTime, type = "assignment") => {
    if (!targetTime) return null;
    const diff = new Date(targetTime).getTime() - Date.now();

    if (diff <= 0) {
      if (type === "delivery") {
        return "DELIVERY BREACHED 🚨";
      }
      return "ASSIGNMENT BREACHED 🚨";
    }

    const mins = Math.floor(diff / 60000);
    const secs = Math.floor((diff % 60000) / 1000);

    if (mins > 60) {
      const hours = Math.floor(mins / 60);
      const remainingMins = mins % 60;
      return `${hours}h ${remainingMins}m`;
    }

    return `${mins}m ${secs}s`;
  };

  const resetAmenityForm = () => {
    setDepartment("");
    setClassroom("");
    setItems([]);
    setItemInput("");
  };

  const resetNoticeForm = () => {
    setNoticeTitle("");
    setNoticeMessage("");
    setSelectedClasses([]);
  };

  const resetCreateClassForm = () => {
    setNewClassName("");
  };

  // NEW: Reset report form
  const resetReportForm = () => {
    setReportReason("");
    setSelectedRequestForReport(null);
  };

  useEffect(() => {
    if (!showAmenityModal) {
      resetAmenityForm();
    }
  }, [showAmenityModal]);

  useEffect(() => {
    if (!showNoticeModal) {
      resetNoticeForm();
    }
  }, [showNoticeModal]);

  useEffect(() => {
    if (!showCreateModal) {
      resetCreateClassForm();
    }
  }, [showCreateModal]);

  useEffect(() => {
    if (!showReportModal) {
      resetReportForm();
    }
  }, [showReportModal]);

  useEffect(() => {
    if (activeTab === "amenities") {
      loadAmenityRequests();
    }
  }, [activeTab]);

  // ================= SLA FUNCTIONS =================
  const checkSlaForRequest = async (requestId) => {
    try {
      const response = await api.get(`/amenities/check-sla/${requestId}`);
      setSlaChecks(prev => ({
        ...prev,
        [requestId]: response.data
      }));
      return response.data;
    } catch (error) {
      console.error("Failed to check SLA:", error);
      setSlaChecks(prev => ({
        ...prev,
        [requestId]: { isSlaBreached: false, canReRequest: false }
      }));
      return null;
    }
  };

  // NEW: Check delivery SLA
  const checkDeliverySla = (request) => {
    if (!request.deliveryDeadline || request.status !== "ASSIGNED") {
      return null;
    }

    const now = new Date();
    const deadline = new Date(request.deliveryDeadline);
    const isBreached = deadline < now;

    if (isBreached) {
      setDeliverySlaChecks(prev => ({
        ...prev,
        [request.id]: {
          isDeliveryBreached: true,
          breachedFor: Math.floor((now - deadline) / 60000) // minutes
        }
      }));
    }

    return isBreached;
  };

  const checkAllPendingSLAs = async () => {
    const pendingRequests = amenityRequests.filter(req => req.status === "PENDING");
    for (const req of pendingRequests) {
      await checkSlaForRequest(req.id);
    }

    // NEW: Check delivery SLAs
    const assignedRequests = amenityRequests.filter(req => req.status === "ASSIGNED");
    assignedRequests.forEach(req => checkDeliverySla(req));
  };

  useEffect(() => {
    if (activeTab === "amenities" && amenityRequests.length > 0) {
      const interval = setInterval(async () => {
        try {
          await checkAllPendingSLAs();
        } catch (error) {
          console.error("Error checking SLAs:", error);
        }
      }, 10000);

      return () => clearInterval(interval);
    }
  }, [activeTab, amenityRequests]);

  // NEW: Function to show confirmation code modal
  const showConfirmationCodeModal = (request) => {
    setConfirmationRequest(request);
    setShowConfirmationModal(true);
  };

  // NEW: Function to copy confirmation code to clipboard
  const copyConfirmationCode = () => {
    if (confirmationRequest?.deliveryConfirmationCode) {
      navigator.clipboard.writeText(confirmationRequest.deliveryConfirmationCode)
        .then(() => {
          alert("Confirmation code copied to clipboard!");
        })
        .catch(err => {
          console.error("Failed to copy code: ", err);
        });
    }
  };

  // NEW: Function to close confirmation modal
  const handleConfirmationModalClose = () => {
    setShowConfirmationModal(false);
    setConfirmationRequest(null);
  };

  const loadAmenityRequests = async () => {
    try {
      const [activeRes, historyRes] = await Promise.all([
        api.get("/amenities/my"),
        api.get("/amenities/my/history"),
      ]);

      const activeOnly = activeRes.data.filter(
        (req) => req.status === "PENDING" || req.status === "ASSIGNED"
      );

      setAmenityRequests(activeOnly);
      setAmenityHistory(historyRes.data);

      // NEW: Check for assigned requests with confirmation codes
      const assignedRequests = activeOnly.filter(req => req.status === "ASSIGNED");
      assignedRequests.forEach(req => {
        // Check if this is a new assignment with a confirmation code
        if (req.deliveryConfirmationCode && req.assignedStaff) {
          // Show confirmation modal for new assignments
          showConfirmationCodeModal(req);
        }
      });

      const pendingRequests = activeOnly.filter(req => req.status === "PENDING");
      for (const req of pendingRequests) {
        await checkSlaForRequest(req.id);
      }

      const assigned = activeOnly.filter(req => req.status === "ASSIGNED");
      assigned.forEach(req => checkDeliverySla(req));

    } catch (err) {
      console.error("Failed to load amenity requests", err);
    }
  };

  const handleReRequest = async (request) => {
    setSelectedRequestForReRequest(request);
    setReRequestModal(true);
  };

  const confirmReRequest = async () => {
    if (!selectedRequestForReRequest) return;

    setReRequesting(true);
    try {
      const response = await api.post(`/amenities/${selectedRequestForReRequest.id}/re-request`);
      alert(`New request created successfully!\nRequest ID: #${response.data.newRequestId}`);
      loadAmenityRequests();
      setReRequestModal(false);
      setSelectedRequestForReRequest(null);
    } catch (error) {
      alert(error.response?.data?.error || "Failed to re-request. Please try again.");
      console.error("Re-request failed:", error);
    } finally {
      setReRequesting(false);
    }
  };

  // NEW: Report staff function
  const handleReportStaff = (request) => {
    if (!request.assignedStaff) {
      alert("No staff assigned to this request.");
      return;
    }
    setSelectedRequestForReport(request);
    setShowReportModal(true);
  };

  const submitReport = async () => {
    if (!reportReason.trim()) {
      alert("Please enter a reason for reporting.");
      return;
    }

    if (!selectedRequestForReport) return;

    setReporting(true);
    try {
      await api.post("/amenities/report-staff", {
        requestId: selectedRequestForReport.id,
        staffId: selectedRequestForReport.assignedStaff.staffId,
        reason: reportReason,
        staffName: selectedRequestForReport.assignedStaff.name,
        department: selectedRequestForReport.department
      });

      alert("Staff reported successfully! Admin has been notified.");
      setShowReportModal(false);
      resetReportForm();
    } catch (error) {
      alert(error.response?.data?.error || "Failed to report staff. Please try again.");
      console.error("Report failed:", error);
    } finally {
      setReporting(false);
    }
  };

  const submitAmenityRequest = async () => {
    if (!department || !classroom || items.length === 0) {
      alert("Fill all fields");
      return;
    }

    try {
      await api.post("/amenities/request", {
        department,
        classroom,
        items
      });
      setShowAmenityModal(false);
      loadAmenityRequests();
    } catch {
      alert("Failed to submit request");
    }
  };

  const [markedMap, setMarkedMap] = useState({});
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split("T")[0]
  );

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

  const loadSummary = async (classCode) => {
    const res = await api.get(`/attendance/${classCode}/summary`);
    setSummary(res.data);
  };

  const createClass = async () => {
    if (!newClassName.trim()) return;

    try {
      const res = await api.post("/professor/classes/create", {
        className: newClassName,
      });
      setCreatedClass(res.data);
      setShowCreateModal(false);
      setShowSuccessModal(true);
      loadClasses();
    } catch {
      alert("Failed to create class");
    }
  };

  const openClass = async (cls) => {
    setSelectedClass(cls);
    setIsViewingAttendance(false);
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

  const loadAttendanceByDate = async (date) => {
    setLoading(true);
    setIsViewingAttendance(true);
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

  const markAttendance = async (studentRegNo, present) => {
    try {
      await api.post("/attendance/mark", {
        classCode: selectedClass.classCode,
        studentRegNo,
        present,
        attendanceDate: selectedDate,
      });
      const summaryRes = await api.get(
        `/attendance/${selectedClass.classCode}/summary`
      );
      setSummary(summaryRes.data);
      const timeoutId = setTimeout(() => {
        setMarkedMap((prev) => ({
          ...prev,
          [studentRegNo]: {
            ...prev[studentRegNo],
            undoable: false,
          },
        }));
      }, 5000);
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
      loadClasses();
    } catch (err) {
      alert("Failed to delete class");
    }
  };

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

  const [notices, setNotices] = useState([]);

  useEffect(() => {
    if (activeTab === "notice") {
      loadProfessorNotices();
    }
  }, [activeTab]);

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
      setNoticeSentClasses(
        classes
          .filter((c) => selectedClasses.includes(c.classCode))
          .map((c) => c.className)
      );
      setShowNoticeModal(false);
      setShowNoticeSuccess(true);
      loadProfessorNotices();
    } catch { }
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
        <nav>
          <button
            className={`nav-item ${activeTab === "attendance" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("attendance");
              setSelectedClass(null);
            }}
          >
            Attendance
          </button>
          <button
            className={`nav-item ${activeTab === "notice" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("notice");
              setSelectedClass(null);
            }}
          >
            Notice Board
          </button>
          <button
            className={`nav-item ${activeTab === "amenities" ? "active" : ""}`}
            onClick={() => setActiveTab("amenities")}
          >
            Missing Amenity ?
          </button>
        </nav>
        <div className="sidebar-footer">
          <button className="logout-btn" onClick={() => setShowLogoutConfirm(true)}>
            <span>Logout</span>
          </button>
        </div>
      </div>

      {/* ================= MAIN ================= */}
      <div className="main-area">
        {activeTab === "attendance" && (
          <>
            {!selectedClass ? (
              <>
                <div className="header">
                  <h1>Your Classes</h1>
                  <button className="create-btn" onClick={() => setShowCreateModal(true)}>+ Create Class</button>
                </div>
                {showCreateModal && (
                  <div className="modal-overlay">
                    <div className="modal">
                      <h2>Create Class</h2>
                      <input value={newClassName} onChange={(e) => setNewClassName(e.target.value)} placeholder="Class name" />
                      <div className="modal-actions">
                        <button className="cancel" onClick={() => setShowCreateModal(false)}>Cancel</button>
                        <button className="confirm" onClick={createClass}>Create</button>
                      </div>
                    </div>
                  </div>
                )}
                {showSuccessModal && createdClass && (
                  <div className="modal-overlay">
                    <div className="modal">
                      <h2>Class Created Successfully</h2>
                      <p><strong>Class Name:</strong> {createdClass.className}</p>
                      <div className="class-code-box">
                        <span>{createdClass.classCode}</span>
                        <button onClick={() => navigator.clipboard.writeText(createdClass.classCode)}>Copy</button>
                      </div>
                      <button className="confirm" onClick={() => setShowSuccessModal(false)}>Done</button>
                    </div>
                  </div>
                )}
                <div className="class-grid">
                  {classes.map((cls) => (
                    <div key={cls.id} className="class-card" onClick={() => openClass(cls)}>{cls.className}</div>
                  ))}
                </div>
              </>
            ) : (
              <>
                <div className="header class-header">
                  <div>
                    <h1>{selectedClass.className}</h1>
                    <div className="class-code-inline">
                      <span className="code">{selectedClass.classCode}</span>
                      <button className="copy-btn" onClick={() => navigator.clipboard.writeText(selectedClass.classCode)}>Copy</button>
                    </div>
                  </div>
                  <div className="header-actions">
                    <button className="delete-btn" onClick={deleteClass}>Delete Class</button>
                    <button className="back-btn" onClick={() => setSelectedClass(null)}>← Back</button>
                  </div>
                </div>
                {summary && (
                  <div className="attendance-summary">
                    <div><strong>Total Lectures</strong><span>{summary.totalLectures}</span></div>
                    <div><strong>Avg Attendance</strong><span>{summary.averageAttendance}%</span></div>
                    <div className="warning"><strong>Low Attendance</strong><span>{summary.lowAttendanceCount}</span></div>
                  </div>
                )}
                <div className="attendance-toggle">
                  <button className={!showAnalytics ? "active" : ""} onClick={() => { setShowAnalytics(false); setIsViewingAttendance(false); }}>Mark Attendance</button>
                  <button className={showAnalytics ? "active" : ""} onClick={() => { setShowAnalytics(true); loadStudentAnalytics(); }}>📊 View Analytics</button>
                </div>
                <div className="attendance-toolbar">
                  <div className="date-picker">
                    <label>Attendance Date</label>
                    <input type="date" value={selectedDate} onChange={(e) => setSelectedDate(e.target.value)} />
                  </div>
                  <button className="view-btn" onClick={() => loadAttendanceByDate(selectedDate)}>View Attendance</button>
                </div>
                <div className="attendance-card">
                  {showAnalytics ? (
                    <table className="analytics-table">
                      <thead><tr><th>#</th><th>Reg No</th><th>Total</th><th>Present</th><th>%</th><th>Status</th></tr></thead>
                      <tbody>
                        {studentAnalytics.map((s, index) => (
                          <tr key={s.studentRegNo}><td>{index + 1}</td><td>{s.studentRegNo}</td><td>{s.totalLectures}</td><td>{s.presentCount}</td><td>{s.attendancePercentage.toFixed(1)}%</td><td><span className={`status-pill ${s.lowAttendance ? "absent" : "present"}`}>{s.lowAttendance ? "Low" : "OK"}</span></td></tr>
                        ))}
                      </tbody>
                    </table>
                  ) : isViewingAttendance ? ( // <-- Check if we're viewing attendance
                    attendance.length > 0 ? (
                      attendance.map((a) => (
                        <div key={a.studentRegNo} className="student-row">
                          <span>{a.studentRegNo}</span>
                          <span className={`status-pill ${a.present ? "present" : "absent"}`}>
                            {a.present ? "Present" : "Absent"}
                          </span>
                        </div>
                      ))
                    ) : (
                      <p className="empty">No attendance available for {selectedDate}</p>
                    )
                  ) : students.length > 0 ? ( // <-- Show student list for marking
                    students.map((enrollment, index) => {
                      const student = enrollment.student;
                      const marked = markedMap[student.regNo];
                      return (
                        <div key={student.regNo} className="student-row grid-row">
                          <div className="col-serial">{index + 1}</div>
                          <div className="student-info"><div className="avatar">{student.name.charAt(0)}</div><span className="student-name">{student.name}</span></div>
                          <div className="reg-no">{student.regNo}</div>
                          <div className="actions">
                            {marked ? (
                              <div className="marked-box">
                                <span className={`status-pill ${marked.status ? "present" : "absent"}`}>{marked.status ? "Marked: Present" : "Marked: Absent"}</span>
                                {marked.undoable && <button className="undo-btn" onClick={() => undoAttendance(student.regNo)}>Undo</button>}
                              </div>
                            ) : (
                              <><button className="present" onClick={() => markAttendance(student.regNo, true)}>Present</button><button className="absent" onClick={() => markAttendance(student.regNo, false)}>Absent</button></>
                            )}
                          </div>
                        </div>
                      );
                    })
                  ) : <p className="empty">No students joined yet</p>
                  }
                </div>
              </>
            )}
          </>
        )}

        {activeTab === "notice" && (
          <div className="notice-board">
            <div className="header"><h1>Notice Board</h1><button className="create-btn" onClick={() => setShowNoticeModal(true)}>+ Create Notice</button></div>
            <div className="notice-list">
              {Object.values(groupedNotices).map((notice, index) => (
                <div key={index} className="notice-card">
                  <div className="notice-header"><h3>{notice.title}</h3><span>{new Date(notice.createdAt).toLocaleDateString()}</span></div>
                  <p className="notice-message">{notice.message}</p>
                  <div className="notice-footer"><span>— {notice.professorName}</span><div>{notice.classCodes.map(code => <span key={code} className="class-pill">{code}</span>)}</div></div>
                </div>
              ))}
            </div>
            {showNoticeModal && (
              <div className="modal-overlay">
                <div className="modal">
                  <h2>Create Notice</h2>
                  <input placeholder="Title" value={noticeTitle} onChange={(e) => setNoticeTitle(e.target.value)} />
                  <textarea placeholder="Write your notice..." value={noticeMessage} onChange={(e) => setNoticeMessage(e.target.value)} />
                  <div className="class-list">{classes.map((cls) => <label key={cls.classCode}><input type="checkbox" checked={selectedClasses.includes(cls.classCode)} onChange={() => toggleClassSelection(cls.classCode)} />{cls.className}</label>)}</div>
                  <div className="modal-actions"><button className="cancel" onClick={() => setShowNoticeModal(false)}>Cancel</button><button className="confirm" onClick={publishNotice}>Publish</button></div>
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === "amenities" && (
          <div className="amenity-board">
            <div className="header amenity-header">
              <h1>Missing Amenities</h1>
              <div className="amenity-header-actions">
                <button className="history-btn" onClick={() => setShowAmenityHistory(true)}>Previous Requests</button>
                <button className="create-btn" onClick={() => setShowAmenityModal(true)}>+ Raise Request</button>
              </div>
            </div>

            <h2 className="section-title" style={{ color: "#111827" }}>Active Requests</h2>

            {amenityRequests.length === 0 ? (
              <p className="empty">No active requests</p>
            ) : (
              <div className="amenity-list">
                {amenityRequests.map(req => {
                  const slaInfo = slaChecks[req.id];
                  const deliverySlaInfo = deliverySlaChecks[req.id];

                  const isAssignmentSlaBreached = slaInfo?.isSlaBreached === true;
                  const canReRequest = slaInfo?.canReRequest === true;
                  const isDeliverySlaBreached = deliverySlaInfo?.isDeliveryBreached === true;

                  return (
                    <div key={req.id} className={`amenity-card ${isAssignmentSlaBreached || isDeliverySlaBreached ? 'sla-breached' : ''}`}>
                      <div className="amenity-top">
                        <strong>{req.department}</strong>
                        <span className={`status-pill ${req.status.toLowerCase()}`}>{req.status}</span>
                      </div>

                      {/* NEW: Confirmation Code Display for Assigned Requests */}
                      {req.status === "ASSIGNED" && req.deliveryConfirmationCode && (
                        <div className="confirmation-code-section">
                          <div className="confirmation-code-header">
                            <Key size={16} />
                            <span className="confirmation-label">Delivery Confirmation Code</span>
                            <button
                              className="show-code-btn"
                              onClick={() => showConfirmationCodeModal(req)}
                            >
                              Show Code
                            </button>
                          </div>
                          <p className="confirmation-instruction">
                            Please share this 4-digit code with {req.assignedStaff?.name || "the staff"} when they deliver your items.
                          </p>
                        </div>
                      )}

                      {/* Assignment SLA Breach */}
                      {isAssignmentSlaBreached && req.status === "PENDING" && (
                        <div className="sla-breach-banner assignment">
                          <div className="sla-breach-content">
                            <span className="sla-breach-icon">⚠️</span>
                            <div>
                              <strong>ASSIGNMENT SLA BREACHED</strong>
                              <p>Admin failed to assign staff within 2 minutes</p>
                              {canReRequest && (
                                <button
                                  className="re-request-btn"
                                  onClick={() => handleReRequest(req)}
                                  disabled={reRequesting}
                                >
                                  {reRequesting ? "Processing..." : "Re-request Now"}
                                </button>
                              )}
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Delivery SLA Breach */}
                      {isDeliverySlaBreached && req.status === "ASSIGNED" && (
                        <div className="sla-breach-banner delivery">
                          <div className="sla-breach-content">
                            <span className="sla-breach-icon">🚨</span>
                            <div>
                              <strong>DELIVERY SLA BREACHED</strong>
                              <p>Staff failed to deliver within 5 minutes</p>
                              <div className="breach-actions">
                                <button
                                  className="report-btn"
                                  onClick={() => handleReportStaff(req)}
                                >
                                  <Flag size={14} /> Report Staff
                                </button>
                                <button
                                  className="re-request-btn"
                                  onClick={() => handleReRequest(req)}
                                  disabled={reRequesting}
                                >
                                  {reRequesting ? "Processing..." : "Re-request Now"}
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      )}

                      <p>📍 Classroom: <strong>{req.classRoom}</strong></p>

                      {req.assignedStaff && (
                        <div className="assigned-staff">
                          <p>👷 Assigned to: <strong>{req.assignedStaff.name}</strong></p>
                          {req.assignedStaff.contactNo && <p>📞 Contact: <strong>{req.assignedStaff.contactNo}</strong></p>}

                          {/* Report Button for Assigned Staff */}
                          {req.status === "ASSIGNED" && !isDeliverySlaBreached && (
                            <button
                              className="report-staff-btn"
                              onClick={() => handleReportStaff(req)}
                            >
                              <UserX size={14} /> Report Staff Issue
                            </button>
                          )}
                        </div>
                      )}

                      {req.status === "PENDING" && !isAssignmentSlaBreached && req.createdAt && (
                        <div className="sla-timer">
                          <span className="sla-label">Assignment SLA:</span>
                          <span className={`sla-countdown ${slaInfo?.minutesPassed >= 1.5 ? "warning" : ""}`}>
                            {formatRemaining(
                              new Date(req.createdAt).getTime() + 120000,
                              "assignment"
                            )}
                          </span>
                        </div>
                      )}

                      {isAssignmentSlaBreached && slaInfo && (
                        <div className="sla-breached-time">
                          <span>Breached for: {(slaInfo.minutesPassed || 0) - 2} minute(s)</span>
                        </div>
                      )}

                      {req.status === "ASSIGNED" && (
                        <div className={`sla-timer delivery ${isDeliverySlaBreached ? "breached" : ""}`}>
                          <span className="sla-label">Delivery SLA:</span>
                          <span className="sla-countdown">
                            {formatRemaining(req.deliveryDeadline, "delivery")}
                          </span>
                          {isDeliverySlaBreached && (
                            <span className="breached-time">
                              Breached for: {deliverySlaInfo?.breachedFor || 0} minute(s)
                            </span>
                          )}
                        </div>
                      )}

                      <div className="item-list">
                        {req.items.map(i => <span key={i} className="item-pill">{i}</span>)}
                      </div>
                      <small style={{ color: "gray" }}>Raised on {new Date(req.createdAt).toLocaleString()}</small>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {activeTab === "amenities" && showAmenityHistory && (
          <div className="history-overlay" onClick={() => setShowAmenityHistory(false)}>
            <div className="history-card" onClick={(e) => e.stopPropagation()}>
              <div className="history-header"><h3> Request History</h3><button onClick={() => setShowAmenityHistory(false)}>✕</button></div>
              {amenityHistory.length === 0 ? <p className="empty">No history yet</p> : (
                <div className="history-list">
                  {amenityHistory.map(req => (
                    <div key={req.id} className="history-item">
                      <strong>{req.department}</strong>
                      <p>📍 {req.classRoom} | {req.status}</p>
                      <div className="item-list">{req.items.map(i => <span key={i} className="item-pill">{i}</span>)}</div>
                      <small>Updated: {new Date(req.deliveredAt || req.createdAt).toLocaleString()}</small>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {showAmenityModal && (
          <div className="modal-overlay">
            <div className="modal">
              <h2>Raise Request</h2>
              <select value={department} onChange={e => setDepartment(e.target.value)}>
                <option value="">Select Dept</option>
                <option value="CSE">CSE</option><option value="ECE">ECE</option><option value="ME">ME</option>
                <option value="Mathematics">Mathematics</option><option value="Physics">Physics</option><option value="Arts">Arts</option><option value="English Department">English</option>
                <option value="Law">Law</option><option value="Philosophy">Philosophy</option><option value="Hindi">Hindi</option>
              </select>
              <input placeholder="Classroom" value={classroom} onChange={e => setClassroom(e.target.value)} />
              <div className="item-input">
                <input placeholder="Add item" value={itemInput} onChange={e => setItemInput(e.target.value)} />
                <button onClick={() => { if (itemInput.trim()) { setItems([...items, itemInput]); setItemInput(""); } }}>Add</button>
              </div>
              <div className="item-list">{items.map((i, idx) => <span key={idx} className="item-pill">{i}</span>)}</div>
              <div className="modal-actions"><button className="cancel" onClick={() => setShowAmenityModal(false)}>Cancel</button><button className="confirm" onClick={submitAmenityRequest}>Submit</button></div>
            </div>
          </div>
        )}

        {/* Re-Request Modal */}
        {reRequestModal && selectedRequestForReRequest && (
          <div className="modal-overlay">
            <div className="modal">
              <h2>⚠️ Re-request Amenities</h2>
              <p>The original request will be marked as cancelled due to SLA breach and a new one will be raised.</p>
              <div className="modal-actions">
                <button className="cancel" onClick={() => setReRequestModal(false)} disabled={reRequesting}>Cancel</button>
                <button className={`confirm ${reRequesting ? "loading" : ""}`} onClick={confirmReRequest} disabled={reRequesting}>
                  {reRequesting ? "Processing..." : "Yes, Re-request Now"}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* NEW: Report Staff Modal */}
        {showReportModal && selectedRequestForReport && (
          <div className="modal-overlay">
            <div className="modal">
              <div className="modal-header">
                <h2><Flag size={20} /> Report Staff</h2>
                <button className="close-btn" onClick={() => setShowReportModal(false)}>✕</button>
              </div>

              <div className="modal-content">
                <div className="report-info">
                  <p><strong>Staff:</strong> {selectedRequestForReport.assignedStaff?.name}</p>
                  <p><strong>Request:</strong> #{selectedRequestForReport.id} - {selectedRequestForReport.classRoom}</p>
                  <p><strong>Items:</strong> {selectedRequestForReport.items?.join(", ")}</p>
                </div>

                <div className="form-group">
                  <label htmlFor="reportReason">
                    <AlertTriangle size={16} /> Reason for Reporting:
                  </label>
                  <textarea
                    id="reportReason"
                    value={reportReason}
                    onChange={(e) => setReportReason(e.target.value)}
                    placeholder="Describe the issue (e.g., staff didn't deliver on time, rude behavior, wrong items delivered, etc.)"
                    rows={4}
                  />
                </div>

                <div className="modal-actions">
                  <button className="cancel" onClick={() => setShowReportModal(false)} disabled={reporting}>
                    Cancel
                  </button>
                  <button
                    className={`confirm ${reporting ? "loading" : ""}`}
                    onClick={submitReport}
                    disabled={reporting || !reportReason.trim()}
                  >
                    {reporting ? "Reporting..." : "Submit Report"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* NEW: Confirmation Code Modal */}
        {showConfirmationModal && confirmationRequest && (
          <div className="modal-overlay" onClick={handleConfirmationModalClose}>
            <div className="modal confirmation-modal" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h2><Key size={20} /> Delivery Confirmation Code</h2>
                <button className="close-btn" onClick={handleConfirmationModalClose}>✕</button>
              </div>

              <div className="modal-content">
                <div className="confirmation-info">
                  <p><strong>Request:</strong> #{confirmationRequest.id}</p>
                  <p><strong>Room:</strong> {confirmationRequest.classRoom}</p>
                  <p><strong>Staff:</strong> {confirmationRequest.assignedStaff?.name || "Unknown"}</p>
                  <p><strong>Items:</strong> {confirmationRequest.items?.join(", ")}</p>
                </div>

                <div className="confirmation-code-display">
                  <div className="code-label">Share this code with staff:</div>
                  <div className="code-container">
                    <div className="confirmation-code">
                      {confirmationRequest.deliveryConfirmationCode}
                    </div>
                    <button
                      className="copy-code-btn"
                      onClick={copyConfirmationCode}
                      title="Copy to clipboard"
                    >
                      <Copy size={18} />
                    </button>
                  </div>
                  <div className="code-instructions">
                    <p>✅ Staff needs this code to mark delivery as complete</p>
                    <p>⚠️ Do not share this code until items are delivered</p>
                    <p>⏰ Code expires in 2 hours</p>
                  </div>
                </div>

                <div className="modal-actions">
                  <button className="confirm" onClick={handleConfirmationModalClose}>
                    Got it, I'll share with staff
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {showLogoutConfirm && (
          <div className="modal-overlay">
            <div className="modal">
              <h2>Log out?</h2>
              <div className="modal-actions">
                <button className="cancel" onClick={() => setShowLogoutConfirm(false)}>Cancel</button>
                <button className="confirm" onClick={handleLogout}>Logout</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}