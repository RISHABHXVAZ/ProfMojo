import { useState } from "react";
import axios from "../services/api";
import "./AdminOnboarding.css";

export default function AdminOnboarding() {
  const [activeTab, setActiveTab] = useState("professor");

  const goBack = () => {
    window.history.back();
  };

  return (
    <div className="admin-onboarding-page">
      <button className="back-button" onClick={goBack}>
        ← Back
      </button>

      <h1 className="admin-onboarding-title">User Onboarding</h1>

      <div className="admin-onboarding-tabs">
        <button
          className={activeTab === "professor" ? "active" : ""}
          onClick={() => setActiveTab("professor")}
        >
          Professors
        </button>

        <button
          className={activeTab === "student" ? "active" : ""}
          onClick={() => setActiveTab("student")}
        >
          Students
        </button>

        <button
          className={activeTab === "staff" ? "active" : ""}
          onClick={() => setActiveTab("staff")}
        >
          Staff
        </button>
      </div>

      {activeTab === "professor" && <ProfessorOnboardingForm />}
      {activeTab === "student" && <StudentOnboardingForm />}
      {activeTab === "staff" && <StaffOnboardingForm />}
    </div>
  );
}

/* ===================== */
/* PROFESSOR ONBOARDING  */
/* ===================== */

function AddProfessorManual() {
  const [form, setForm] = useState({
    profId: "",
    name: "",
    department: "",
    email: ""
  });

  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    for (const key in form) {
      if (!form[key].trim()) {
        alert(`Please fill ${key}`);
        return;
      }
    }

    setLoading(true);
    try {
      await axios.post("/admin/onboarding/add-professor", form);
      alert("Professor onboarded successfully");
      setForm({ profId: "", name: "", department: "", email: "" });
    } catch (err) {
      alert(err.response?.data?.message || "Failed to add professor");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="onboarding-card">
      <h2>Add Professor (Manual)</h2>

      <input placeholder="Professor ID" value={form.profId}
        onChange={e => setForm({ ...form, profId: e.target.value })} />

      <input placeholder="Full Name" value={form.name}
        onChange={e => setForm({ ...form, name: e.target.value })} />

      <input placeholder="Department" value={form.department}
        onChange={e => setForm({ ...form, department: e.target.value })} />

      <input placeholder="Email" type="email" value={form.email}
        onChange={e => setForm({ ...form, email: e.target.value })} />

      <button onClick={handleSubmit} disabled={loading}>
        {loading ? "Adding..." : "Add Professor"}
      </button>
    </div>
  );
}

function ImportProfessorCsv() {
  return <CsvImportCard
    title="Import Professors via CSV"
    endpoint="/admin/onboarding/add-professors-csv"
    hint="CSV format: prof_id, name, department, email"
  />;
}

function ProfessorOnboardingForm() {
  const [mode, setMode] = useState("manual");

  return (
    <OnboardingWrapper
      manualLabel="Add Professor"
      csvLabel="Import CSV"
      mode={mode}
      setMode={setMode}
      manual={<AddProfessorManual />}
      csv={<ImportProfessorCsv />}
    />
  );
}

/* ===================== */
/* STUDENT ONBOARDING    */
/* ===================== */

function AddStudentManual() {
  const [form, setForm] = useState({
    regNo: "",
    name: "",
    department: "",
    email: ""
  });

  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    for (const key in form) {
      if (!form[key].trim()) {
        alert(`Please fill ${key}`);
        return;
      }
    }

    setLoading(true);
    try {
      await axios.post("/admin/onboarding/add-student", form);
      alert("Student onboarded successfully");
      setForm({ regNo: "", name: "", department: "", email: "" });
    } catch (err) {
      alert(err.response?.data?.message || "Failed to add student");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="onboarding-card">
      <h2>Add Student (Manual)</h2>

      <input placeholder="Registration Number" value={form.regNo}
        onChange={e => setForm({ ...form, regNo: e.target.value })} />

      <input placeholder="Full Name" value={form.name}
        onChange={e => setForm({ ...form, name: e.target.value })} />

      <input placeholder="Department" value={form.department}
        onChange={e => setForm({ ...form, department: e.target.value })} />

      <input placeholder="Email" type="email" value={form.email}
        onChange={e => setForm({ ...form, email: e.target.value })} />

      <button onClick={handleSubmit} disabled={loading}>
        {loading ? "Adding..." : "Add Student"}
      </button>
    </div>
  );
}

function ImportStudentCsv() {
  return <CsvImportCard
    title="Import Students via CSV"
    endpoint="/admin/onboarding/add-students-csv"
    hint="CSV format: reg_no, name, department, email"
  />;
}

function StudentOnboardingForm() {
  const [mode, setMode] = useState("manual");

  return (
    <OnboardingWrapper
      manualLabel="Add Student"
      csvLabel="Import CSV"
      mode={mode}
      setMode={setMode}
      manual={<AddStudentManual />}
      csv={<ImportStudentCsv />}
    />
  );
}

/* ===================== */
/* STAFF ONBOARDING ⭐   */
/* ===================== */

function AddStaffManual() {
  const [form, setForm] = useState({
    staffId: "",
    name: "",
    department: "",
    email: "",
    contactNo: ""
  });

  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    for (const key in form) {
      if (!form[key].trim()) {
        alert(`Please fill ${key}`);
        return;
      }
    }

    setLoading(true);
    try {
      await axios.post("/admin/onboarding/add-staff", form);
      alert("Staff onboarded successfully");
      setForm({
        staffId: "",
        name: "",
        department: "",
        email: "",
        contactNo: ""
      });
    } catch (err) {
      alert(err.response?.data?.message || "Failed to add staff");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="onboarding-card">
      <h2>Add Staff (Manual)</h2>

      <input placeholder="Staff ID" value={form.staffId}
        onChange={e => setForm({ ...form, staffId: e.target.value })} />

      <input placeholder="Full Name" value={form.name}
        onChange={e => setForm({ ...form, name: e.target.value })} />

      <input placeholder="Department" value={form.department}
        onChange={e => setForm({ ...form, department: e.target.value })} />

      <input placeholder="Email" type="email" value={form.email}
        onChange={e => setForm({ ...form, email: e.target.value })} />

      <input placeholder="Contact Number" value={form.contactNo}
        onChange={e => setForm({ ...form, contactNo: e.target.value })} />

      <button onClick={handleSubmit} disabled={loading}>
        {loading ? "Adding..." : "Add Staff"}
      </button>
    </div>
  );
}

function ImportStaffCsv() {
  return <CsvImportCard
    title="Import Staff via CSV"
    endpoint="/admin/onboarding/add-staff-csv"
    hint="CSV format: staff_id, name, department, email, contact_no"
  />;
}

function StaffOnboardingForm() {
  const [mode, setMode] = useState("manual");

  return (
    <OnboardingWrapper
      manualLabel="Add Staff"
      csvLabel="Import CSV"
      mode={mode}
      setMode={setMode}
      manual={<AddStaffManual />}
      csv={<ImportStaffCsv />}
    />
  );
}

/* ===================== */
/* SHARED COMPONENTS    */
/* ===================== */

function OnboardingWrapper({ manualLabel, csvLabel, mode, setMode, manual, csv }) {
  return (
    <div className="professor-onboarding-wrapper">
      <div className="sub-tabs">
        <button className={mode === "manual" ? "active" : ""}
          onClick={() => setMode("manual")}>
          {manualLabel}
        </button>

        <button className={mode === "csv" ? "active" : ""}
          onClick={() => setMode("csv")}>
          {csvLabel}
        </button>
      </div>

      {mode === "manual" && manual}
      {mode === "csv" && csv}
    </div>
  );
}

function CsvImportCard({ title, endpoint, hint }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);

  const uploadCsv = async () => {
    if (!file) return alert("Select a CSV file");

    const formData = new FormData();
    formData.append("file", file);

    setUploading(true);
    try {
      const res = await axios.post(endpoint, formData, {
        headers: { "Content-Type": "multipart/form-data" }
      });

      alert(`Upload Complete\nAdded: ${res.data.added}\nSkipped: ${res.data.skipped}`);
      setFile(null);
    } catch {
      alert("CSV upload failed");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="onboarding-card">
      <h2>{title}</h2>

      <input type="file" accept=".csv"
        onChange={e => setFile(e.target.files[0])} />

      <p className="csv-hint">{hint}</p>

      <button onClick={uploadCsv} disabled={!file || uploading}>
        {uploading ? "Uploading..." : "Upload CSV"}
      </button>
    </div>
  );
}
