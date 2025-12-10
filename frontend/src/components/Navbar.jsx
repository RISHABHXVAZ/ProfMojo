import { Link } from "react-router-dom";
import "./Navbar.css";

export default function Navbar() {
  return (
    <nav className="navbar">
      <h2>ProfMojo</h2>
      <ul>
        <li><Link to="/professor">Professor</Link></li>
        <li><Link to="/student">Student</Link></li>
        <li><Link to="/canteeno">Canteeno</Link></li>
        <li><Link to="/librarian">Librarian</Link></li>
      </ul>
    </nav>
  );
}
