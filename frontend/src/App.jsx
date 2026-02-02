import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import ProfessorLogin from "./pages/ProfessorLogin";
import ProfessorRegister from "./pages/ProfessorRegister";
import ProfessorDashboard from "./pages/ProfessorDashboard";
import StudentLogin from "./pages/StudentLogin";
import StudentDashboard from "./pages/StudentDashboard";
import AdminLogin from "./pages/AdminLogin";
import AdminDashboard from "./pages/AdminDashboard";
import StaffLogin from "./pages//StaffLogin";
import StaffDashboard from "./pages/StaffDashboard";
import ProfessorForgotPassword from "./pages/ProfessorForgotPassword";
import ProfessorVerifyOtp from "./pages/ProfessorVerifyOtp";
import AdminOnboarding from "./pages/AdminOnboarding";
import StudentForgotPassword from "./pages/StudentForgotPassword";
import StudentVerifyOtp from "./pages/StudentVerifyOtp";
import StaffForgotPassword from "./pages/StaffForgotPassword";
import StaffVerifyOtp from "./pages/StaffVerifyOtp";


function App() {
  return (
    <BrowserRouter>
      <Routes>

        {/* Home */}
        <Route path="/" element={<Home />} />

        {/* Professor Auth */}
        <Route path="/professor" element={<ProfessorLogin />} />
        <Route path="/professor/login" element={<ProfessorLogin />} />
        <Route path="/professor/register" element={<ProfessorRegister />} />
        <Route path="/professor/dashboard" element={<ProfessorDashboard />} />
        <Route path="/professor/forgot-password" element={<ProfessorForgotPassword />} />
        <Route path="/professor/verify-otp" element={<ProfessorVerifyOtp />} />


        {/* Student Auth */}
        <Route path="/student" element={<StudentLogin />} />
        <Route path="/student/login" element={<StudentLogin />} />
        <Route path="/student/forgot-password" element={<StudentForgotPassword />} />
        <Route path="/student/verify-otp" element={<StudentVerifyOtp />} />
        <Route path="/student/dashboard" element={<StudentDashboard />} />

        {/* Admin Auth */}
        <Route path="/admin" element={<AdminLogin />} />
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        <Route path="/admin/onboarding" element={<AdminOnboarding />} />


        {/* Staff Auth */}
        <Route path="/staff/login" element={<StaffLogin />} />
        <Route path="/staff/dashboard" element={<StaffDashboard />} />
        <Route path="/staff/forgot-password" element={<StaffForgotPassword />} />
        <Route path="/staff/verify-otp" element={<StaffVerifyOtp />} />


      </Routes>
    </BrowserRouter>
  );
}

export default App;
