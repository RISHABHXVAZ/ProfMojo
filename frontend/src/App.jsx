import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import ProfessorLogin from "./pages/ProfessorLogin";
import ProfessorRegister from "./pages/ProfessorRegister";
import ProfessorDashboard from "./pages/ProfessorDashboard";
import StudentLogin from "./pages/StudentLogin";
import StudentRegister from "./pages/StudentRegister";
import StudentDashboard from "./pages/StudentDashboard";

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

      
        <Route path="/student/login" element={<StudentLogin />} />
        <Route path="/student/register" element={<StudentRegister />} />
        <Route path="/student/dashboard" element={<StudentDashboard />} />

      </Routes>
    </BrowserRouter>
  );
}

export default App;
