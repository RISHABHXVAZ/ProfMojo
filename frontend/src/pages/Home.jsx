import { Link } from "react-router-dom";
import "./Home.css";

export default function Home() {
    return (
        <div className="home-container">
            <div className="home-card">

                <div className="left-section">
                    <h1 className="logo">ProfMojo</h1>
                    <p className="tagline">Empowering Campus Operations</p>
                </div>

                <div className="center-section">
                    <div className="role-grid">
                        <Link to="/professor/login" className="role-card">
                            Professor
                        </Link>

                        <Link to="/student/login" className="role-card">
                            Student
                        </Link>

                        <Link to="/staff/login" className="role-card">
                            Staff
                        </Link>

                        <Link to="/admin/login" className="role-card">
                            Admin
                        </Link>
                    </div>
                </div>

                <div className="right-section">
                    <h2>Select Your Role</h2>
                    <p className="sub-text">
                        Login or Register to continue using the platform.
                    </p>
                </div>

            </div>
        </div>
    );
}
