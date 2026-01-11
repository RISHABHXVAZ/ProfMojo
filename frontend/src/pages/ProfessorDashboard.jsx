import { useEffect, useState } from "react";
import api from "../services/api";
import "./ProfessorDashboard.css";
import { QRCodeCanvas } from "qrcode.react";


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
  // ================= CANTEEN (PROFESSOR) =================
  const [canteens, setCanteens] = useState([]);
  const [selectedCanteen, setSelectedCanteen] = useState(null);
  const [canteenMenu, setCanteenMenu] = useState([]);
  const [cart, setCart] = useState([]);
  const [cabinLocation, setCabinLocation] = useState("");
  const [paymentMode, setPaymentMode] = useState("CASH");
  const [cartStep, setCartStep] = useState(null);
  const [professorName, setProfessorName] = useState("");
  const [contactNo, setContactNo] = useState("");
  const [showOrderSuccess, setShowOrderSuccess] = useState(false);
  const [placedOrder, setPlacedOrder] = useState(null);
  const [showTrackOrder, setShowTrackOrder] = useState(false);
  // ===== AMENITIES =====
  const [amenityRequests, setAmenityRequests] = useState([]);
  const [showAmenityModal, setShowAmenityModal] = useState(false);
  const [department, setDepartment] = useState("");
  const [classroom, setClassroom] = useState("");
  const [items, setItems] = useState([]);
  const [itemInput, setItemInput] = useState("");

  const [searchTerm, setSearchTerm] = useState("");
  const [canteenUpi, setCanteenUpi] = useState(null);
  const [confirmingPayment, setConfirmingPayment] = useState(false);
  const [upiOpened, setUpiOpened] = useState(false);
  useEffect(() => {
    if (paymentMode === "UPI" && canteenUpi) {
      setUpiOpened(true);
    }
  }, [paymentMode, canteenUpi]);


  useEffect(() => {
    if (activeTab === "amenities") {
      loadAmenityRequests();
    }
  }, [activeTab]);

  const loadAmenityRequests = async () => {
    try {
      const res = await api.get("/amenities/my");
      setAmenityRequests(res.data);
    } catch (err) {
      console.error("Failed to load amenity requests", err);
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
      setDepartment("");
      setClassroom("");
      setItems([]);
      setItemInput("");

      loadAmenityRequests();
    } catch {
      alert("Failed to submit request");
    }
  };



  useEffect(() => {
    if (
      cartStep === "PAYMENT" &&
      paymentMode === "UPI" &&
      selectedCanteen
    ) {
      fetchCanteenUpi();
    }
  }, [cartStep, paymentMode, selectedCanteen]);

  const fetchCanteenUpi = async () => {
    try {
      const res = await api.get(
        `/canteen/${selectedCanteen.canteenId}/upi`
      );
      setCanteenUpi(res.data);
    } catch (err) {
      console.error("Failed to fetch UPI ID", err);
    }
  };

  const getUpiPaymentString = () => {
    if (!canteenUpi) return "";

    const params = new URLSearchParams({
      pa: canteenUpi.upiId,
      pn: canteenUpi.canteenName,
      am: grandTotal.toString(),
      cu: "INR",
      tn: `Order Payment`
    });

    return `upi://pay?${params.toString()}`;
  };



  const DELIVERY_CHARGE = 20;

  useEffect(() => {
    loadProfessorProfile();
  }, []);

  useEffect(() => {
    if (cart.length === 0 && cartStep !== null) {
      setCartStep(null);
    }
  }, [cart, cartStep]);


  const loadProfessorProfile = async () => {
    try {
      const res = await api.get("/professors/me");
      setProfessorName(res.data.name);
      setContactNo(res.data.contactNo || "");
    } catch (err) {
      console.error("Failed to load professor profile", err);
    }
  };

  const hasOngoingOrder =
    placedOrder &&
    ["PLACED", "PREPARING", "READY"].includes(placedOrder.status);

  const totalItems = cart.reduce((sum, i) => sum + i.quantity, 0);

  const [notices, setNotices] = useState([]);

  useEffect(() => {
    if (activeTab === "notice") {
      loadProfessorNotices();
    }
  }, [activeTab]);

  useEffect(() => {
    if (activeTab === "canteen") {
      loadCanteens();
    }
  }, [activeTab]);

  const loadCanteens = async () => {
    try {
      const res = await api.get("/canteen/active"); // list of canteens
      setCanteens(res.data);
    } catch {
      alert("Failed to load canteens");
    }
  };
  const loadActiveOrder = async () => {
    try {
      const res = await api.get("/orders/my/active");
      setPlacedOrder(res.data); // may be null
    } catch (err) {
      console.error("Failed to load active order", err);
    }
  };

  useEffect(() => {
    if (activeTab === "canteen") {
      loadCanteens();
      loadActiveOrder(); // ✅ THIS IS THE KEY LINE
    }
  }, [activeTab]);


  const confirmPayment = async () => {
    try {
      setConfirmingPayment(true);

      const res = await api.put(
        `/orders/${placedOrder.id}/confirm-payment`
      );

      setPlacedOrder(res.data); // update local state
    } catch (err) {
      alert("Failed to confirm payment");
    } finally {
      setConfirmingPayment(false);
    }
  };

  const openCanteen = async (canteen) => {
    setSelectedCanteen(canteen);
    setCanteenUpi(null);
    setCanteenMenu([]);
    setCart([]);
    setLoading(true);

    try {
      const res = await api.get(
        `canteen/menu/${canteen.canteenId}`
      );
      setCanteenMenu(res.data);
    } catch (err) {
      console.error(err);
      alert("Failed to load menu");
    } finally {
      setLoading(false);
    }
  };


  const addToCart = (item) => {
    setCart(prev => {
      const found = prev.find(i => i.id === item.id);
      if (found) {
        return prev.map(i =>
          i.id === item.id
            ? { ...i, quantity: i.quantity + 1 }
            : i
        );
      }
      return [...prev, { ...item, quantity: 1 }];
    });
  };

  const removeFromCart = (itemId) => {
    setCart(prev =>
      prev
        .map(i =>
          i.id === itemId
            ? { ...i, quantity: i.quantity - 1 }
            : i
        )
        .filter(i => i.quantity > 0)
    );
  };


  const cartTotal = cart.reduce(
    (sum, i) => sum + i.price * i.quantity,
    0
  );

  const grandTotal = cartTotal + (cart.length > 0 ? DELIVERY_CHARGE : 0);



  const placeOrder = async () => {
    if (grandTotal < DELIVERY_CHARGE) {
      alert("Please add items to cart");
      return;
    }

    if (!cabinLocation.trim()) {
      alert("Enter cabin location");
      return;
    }

    if (cart.length === 0) {
      alert("Cart is empty");
      return;
    }

    try {
      const res = await api.post("/orders/place", {
        canteenId: selectedCanteen.canteenId,
        cabinLocation,
        contactNo,
        paymentMode,
        items: cart.map(i => ({
          itemId: i.id,
          itemName: i.name,
          quantity: i.quantity
        })),
        totalAmount: grandTotal
      });


      // ✅ FIX: Manually attach canteen info if backend response lacks it
      setPlacedOrder({
        ...res.data,
        canteenContactNo: res.data.canteenContactNo || selectedCanteen.contactNo || selectedCanteen.contact
      });

      setShowOrderSuccess(true);

      // reset cart UI
      setCart([]);
      setCabinLocation("");
      setPaymentMode("CASH");
      setUpiOpened(false);
      setCartStep(null);

    } catch (err) {
      alert("Failed to place order");
    }
  };


  const filteredMenu = canteenMenu.filter(item =>
    item.name.toLowerCase().includes(searchTerm.toLowerCase())
  );


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

  const StatusStep = ({ label, active }) => (
    <div className={`status-step ${active ? "active" : ""}`}>
      <div className="dot" />
      <span>{label}</span>
    </div>
  );




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
        <button
          className={`nav-item ${activeTab === "amenities" ? "active" : ""}`}
          onClick={() => setActiveTab("amenities")}
        >
          Missing Amenity ?
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
                    /* ================= MARK ATTENDANCE ================= */
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
        {activeTab === "canteen" && !selectedCanteen && (
          <>
            <div className="header">
              <h1>Select Canteen</h1>
            </div>

            <div className="class-grid">
              {canteens.map(c => (
                <div
                  key={c.canteenId}
                  className="canteen-card"
                  onClick={() => openCanteen(c)}
                >
                  <h3 style={{ textAlign: "center" }}>{c.canteenName}</h3>
                  <p style={{ textAlign: "center" }}>📍 {c.location}</p>
                  <span className="enter-pill">ENTER →</span>
                </div>


              ))}
            </div>
          </>
        )}
        {activeTab === "canteen" && selectedCanteen && (
          <>
            <div className="header canteen-header">
              <h1>{selectedCanteen.canteenName}</h1>

              <div className="search-container">
                <input
                  type="text"
                  className="menu-search-bar"
                  placeholder="Search for dishes..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>

              <div className="header-actions">
                {hasOngoingOrder && (
                  <button
                    className="track-btn"
                    onClick={() => setShowTrackOrder(true)}
                  >
                    📦 Track Order
                  </button>
                )}

                <button
                  className="back-btn"
                  onClick={() => setSelectedCanteen(null)}
                >
                  ← Back
                </button>
              </div>
            </div>


            <div className="menu-grid">
              {filteredMenu.length > 0 ? (
                filteredMenu.map(item => (
                  <div key={item.id} className="menu-item-card">
                    <div className="menu-card-top">
                      <h4>{item.name}</h4>
                      <div className="price-tag">
                        <span className="currency">₹</span>
                        <span className="amount">{item.price}</span>
                      </div>
                    </div>

                    <button className="add-btn" onClick={() => addToCart(item)}>
                      <span>+</span> Add to Cart
                    </button>
                  </div>
                ))
              ) : (
                <p className="no-results">No items found matching "{searchTerm}"</p>
              )}
            </div>


            {cart.length > 0 && cartStep === null && (
              <div className="floating-cart">
                <div className="cart-left">
                  <span className="cart-icon">🛒</span>
                  <span>{totalItems} items</span>
                </div>

                <div className="cart-right">
                  <strong>₹{grandTotal}</strong>
                  <button onClick={() => setCartStep("CART")}>
                    View Cart →
                  </button>
                </div>
              </div>
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
        {activeTab === "amenities" && (
          <div className="amenity-board">
            <div className="header">
              <h1>Missing Amenities</h1>
              <button
                className="create-btn"
                onClick={() => setShowAmenityModal(true)}
              >
                + Raise Request
              </button>
            </div>

            {amenityRequests.length === 0 ? (
              <p className="empty">No requests raised yet</p>
            ) : (
              <div className="amenity-list">
                {amenityRequests.map(req => (
                  <div key={req.id} className="amenity-card">
                    <div className="amenity-top">
                      <strong>{req.department}</strong>
                      <span className={`status-pill ${req.status.toLowerCase()}`}>
                        {req.status}
                      </span>
                    </div>

                    <p>📍 Classroom: <strong>{req.classRoom}</strong></p>

                    <div className="item-list">
                      {req.items.map(i => (
                        <span key={i} className="item-pill">{i}</span>
                      ))}
                    </div>

                    <small>
                      Raised on {new Date(req.createdAt).toLocaleString()}
                    </small>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        {showAmenityModal && (
          <div className="modal-overlay">
            <div className="modal">
              <h2>Raise Amenity Request</h2>

              <select
                value={department}
                onChange={e => setDepartment(e.target.value)}
              >
                <option value="">Select Department</option>
                <option value="CSE">CSE</option>
                <option value="ECE">ECE</option>
                <option value="ME">ME</option>
              </select>

              <input
                placeholder="Classroom (e.g. GS7, Lab L1)"
                value={classroom}
                onChange={e => setClassroom(e.target.value)}
              />

              <div className="item-input">
                <input
                  placeholder="Add item (e.g. Chalk)"
                  value={itemInput}
                  onChange={e => setItemInput(e.target.value)}
                />
                <button
                  onClick={() => {
                    if (itemInput.trim()) {
                      setItems([...items, itemInput]);
                      setItemInput("");
                    }
                  }}
                >
                  Add
                </button>
              </div>

              <div className="item-list">
                {items.map((i, idx) => (
                  <span key={idx} className="item-pill">{i}</span>
                ))}
              </div>

              <div className="modal-actions">
                <button className="cancel" onClick={() => setShowAmenityModal(false)}>
                  Cancel
                </button>
                <button className="confirm" onClick={submitAmenityRequest}>
                  Submit
                </button>
              </div>
            </div>
          </div>
        )}



        {/* ================= MODALS ================= */}

        {/* CART FLOW */}
        {cartStep && (
          <div className="modal-overlay">
            <div className="modal cart-modal">
              <h2>
                {cartStep === "CART" && "Your Order"}
                {cartStep === "ADDRESS" && "Delivery Details"}
                {cartStep === "PAYMENT" && "Payment Method"}
              </h2>

              {cartStep === "CART" && cart.map(i => (
                <div key={i.id} className="cart-row">
                  <span>{i.name}</span>
                  <div className="qty-controls">
                    <button onClick={() => removeFromCart(i.id)}>-</button>
                    <span>{i.quantity}</span>
                    <button onClick={() => addToCart(i)}>+</button>
                  </div>
                  <span>₹{i.price * i.quantity}</span>
                </div>
              ))}

              {cartStep === "ADDRESS" && (
                <div className="address-form">
                  <div className="form-group"><label>Professor Name</label><input className="modal-input" value={professorName} disabled /></div>
                  <div className="form-group"><label>Cabin Location</label><input className="modal-input" placeholder="e.g. CS-302" value={cabinLocation} onChange={e => setCabinLocation(e.target.value)} /></div>
                  <div className="form-group"><label>Contact Number</label><input className="modal-input" placeholder="10-digit mobile number" value={contactNo} onChange={e => setContactNo(e.target.value)} /></div>
                </div>
              )}

              {cartStep === "PAYMENT" && (
                <div className="payment-options">

                  {["CASH", "UPI"].map(mode => (
                    <label
                      key={mode}
                      className={`payment-option ${paymentMode === mode ? "active" : ""}`}
                      onClick={() => setPaymentMode(mode)}
                    >
                      <div className="payment-left">
                        <input
                          type="radio"
                          checked={paymentMode === mode}
                          readOnly
                        />
                        <span>{mode}</span>
                      </div>
                    </label>
                  ))}

                  {paymentMode === "UPI" && canteenUpi && (
                    <div className="upi-box">
                      <p><strong>Pay via UPI</strong></p>

                      <div className="upi-id">
                        <span>{canteenUpi.upiId}</span>
                        <button
                          onClick={() =>
                            navigator.clipboard.writeText(canteenUpi.upiId)
                          }
                        >
                          Copy
                        </button>
                      </div>

                      <a
                        href={`upi://pay?pa=${canteenUpi.upiId}&pn=${canteenUpi.canteenName}&am=${grandTotal}&cu=INR`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="upi-pay-btn"
                        onClick={() => setUpiOpened(true)}
                      >
                        Open UPI App
                      </a>
                      <div className="upi-qr-section">
                        <p><strong>Scan & Pay</strong></p>

                        <QRCodeCanvas
                          value={getUpiPaymentString()}
                          size={180}
                          bgColor="#ffffff"
                          fgColor="#000000"
                          level="H"
                          includeMargin
                        />



                        <p className="upi-note">
                          Scan with any UPI app (GPay, PhonePe, Paytm)
                        </p>
                      </div>


                      <p className="upi-note">
                        After payment, click <strong>Place Order</strong>
                      </p>
                    </div>
                  )}

                </div>
              )}



              <div className="modal-actions">
                <button className="cancel" onClick={() => cartStep === "CART" ? setCartStep(null) : setCartStep(cartStep === "ADDRESS" ? "CART" : "ADDRESS")}>
                  {cartStep === "CART" ? "Close" : "Back"}
                </button>
                <button
                  className="confirm"
                  disabled={
                    cartStep === "PAYMENT" &&
                    paymentMode === "UPI" &&
                    !upiOpened
                  }
                  onClick={() =>
                    cartStep === "PAYMENT"
                      ? placeOrder()
                      : setCartStep(cartStep === "CART" ? "ADDRESS" : "PAYMENT")
                  }
                >
                  {cartStep === "PAYMENT" ? "Place Order" : "Next"}
                </button>


              </div>
              <div className="cart-summary">
                <div className="summary-row">
                  <span>Items Total</span>
                  <span>₹{cartTotal}</span>
                </div>

                {cart.length > 0 && (
                  <div className="summary-row">
                    <span>Delivery Charges</span>
                    <span>₹{DELIVERY_CHARGE}</span>
                  </div>
                )}

                <div className="summary-row total">
                  <span>Total</span>
                  <span>₹{grandTotal}</span>
                </div>
              </div>



            </div>
          </div>
        )}

        {/* ORDER SUCCESS MODAL */}
        {showOrderSuccess && placedOrder && (
          <div className="modal-overlay">
            <div className="modal success-modal">
              <h2>✅ Order Placed Successfully</h2>
              <p className="order-id">Order ID: <strong>{placedOrder.id}</strong></p>
              <p>Sent to: <strong>{selectedCanteen?.canteenName || placedOrder.canteenId}</strong></p>
              <div className="canteen-contact">
                📞 Contact canteen at:
                <strong> {placedOrder.canteenContactNo || selectedCanteen?.contactNo || "N/A"}</strong>
              </div>
              <div className="modal-actions">
                <button className="confirm" onClick={() => { setShowOrderSuccess(false); setShowTrackOrder(true); }}>Track Order →</button>
                <button className="cancel" onClick={() => setShowOrderSuccess(false)}>Close</button>
              </div>
            </div>
          </div>
        )}

        {/* TRACK ORDER MODAL */}
        {showTrackOrder && placedOrder && (
          <div className="modal-overlay">
            <div className="modal track-modal">
              <h2>📦 Order Status</h2>
              <div className="order-status">
                <StatusStep label="Placed" active />
                <StatusStep label="Preparing" active={placedOrder.status !== "PLACED"} />
                <StatusStep label="Ready" active={placedOrder.status === "READY"} />
                <StatusStep label="Delivered" active={placedOrder.status === "DELIVERED"} />
              </div>
              {placedOrder.paymentMode === "UPI" &&
                placedOrder.paymentStatus === "PENDING" && !confirmingPayment && (
                  <button
                    className="confirm"
                    onClick={confirmPayment}
                  >
                    I have paid
                  </button>
                )}


              <div className="track-footer">
                <p>📍 Delivery Location: <strong> {placedOrder.cabinLocation}</strong></p>
                <p>📞 Canteen Contact: <strong> {placedOrder.canteenContactNo || selectedCanteen?.contactNo || "N/A"}</strong></p>
              </div>
              {placedOrder.paymentMode === "UPI" && (
                <p>
                  💳 Payment Status:{" "}
                  <strong>
                    {placedOrder.paymentStatus === "PAID" ? "Paid ✅" : "Pending ⏳"}
                  </strong>
                </p>
              )}

              <button className="confirm" onClick={() => setShowTrackOrder(false)}>Done</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}