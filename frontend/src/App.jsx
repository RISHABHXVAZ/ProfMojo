import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import ProfessorLogin from "./pages/ProfessorLogin";
import ProfessorRegister from "./pages/ProfessorRegister";
import ProfessorDashboard from "./pages/ProfessorDashboard";

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

        {/* Dashboard */}
        <Route path="/professor/dashboard" element={<ProfessorDashboard />} />

      </Routes>
    </BrowserRouter>
  );
}

export default App;
