import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import ProfessorLogin from "./pages/ProfessorLogin";
import ProfessorRegister from "./pages/ProfessorRegister";
import ProfessorDashboard from "./pages/ProfessorDashboard";
import StudentLogin from "./pages/StudentLogin";
import StudentRegister from "./pages/StudentRegister";
import StudentDashboard from "./pages/StudentDashboard";
import CanteenLogin from "./pages/CanteenLogin";
import CanteenRegister from "./pages/CanteenRegister";
import CanteenDashboard from "./pages/CanteenDashboard";


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

        {/* Student Auth */}
        <Route path="/student" element={<StudentLogin />} />
        <Route path="/student/login" element={<StudentLogin />} />
        <Route path="/student/register" element={<StudentRegister />} />
        <Route path="/student/dashboard" element={<StudentDashboard />} />

        {/* Canteen Auth */}
        <Route path="/canteen/login" element={<CanteenLogin />} />
        <Route path="/canteen/register" element={<CanteenRegister />} />
        <Route path="/canteen/dashboard" element={<CanteenDashboard />} />

      </Routes>
    </BrowserRouter>
  );
}

export default App;
