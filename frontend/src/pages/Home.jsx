import { Link } from "react-router-dom";
import "./Home.css";

export default function Home() {
    return (
        <div className="home-page">
            {/* Header */}
            <header className="home-header">
                <div className="header-content">
                    <h1 className="home-logo">ProfMojo</h1>
                    <p className="home-subtitle">Empowering Campus Operations</p>
                </div>
            </header>

            {/* Main Content - Centered */}
            <main className="home-main">
                <div className="main-content">
                    {/* Welcome Section */}
                    <div className="welcome-section">
                        <h2 className="welcome-title">Welcome to ProfMojo</h2>
                        <p className="welcome-text">
                            Select your role to access the campus management platform
                        </p>
                    </div>

                    {/* Role Selection */}
                    <div className="role-section">
                        <div className="role-grid">
                            <Link to="/professor/login" className="role-card professor">
                                <div className="role-icon">👨‍🏫</div>
                                <div className="role-info">
                                    <h3>Professor</h3>
                                    <p>Attendance, notices, and amenities</p>
                                </div>
                            </Link>

                            <Link to="/student/login" className="role-card student">
                                <div className="role-icon">🎓</div>
                                <div className="role-info">
                                    <h3>Student</h3>
                                    <p>View attendance and notices</p>
                                </div>
                            </Link>

                            <Link to="/staff/login" className="role-card staff">
                                <div className="role-icon">👷‍♂️</div>
                                <div className="role-info">
                                    <h3>Staff</h3>
                                    <p>Manage amenities and requests</p>
                                </div>
                            </Link>

                            <Link to="/admin/login" className="role-card admin">
                                <div className="role-icon">⚙️</div>
                                <div className="role-info">
                                    <h3>Admin</h3>
                                    <p>System administration</p>
                                </div>
                            </Link>
                        </div>
                    </div>

                    {/* Instructions */}
                    <div className="instructions-section">
                        <h3 className="instructions-title">How to Get Started</h3>
                        <div className="steps-grid">
                            <div className="step">
                                <div className="step-number">1</div>
                                <h4>Select Your Role</h4>
                                <p>Choose the appropriate role from the options above</p>
                            </div>
                            <div className="step">
                                <div className="step-number">2</div>
                                <h4>Login or Register</h4>
                                <p>Use your credentials or create a new account</p>
                            </div>
                            <div className="step">
                                <div className="step-number">3</div>
                                <h4>Access Platform</h4>
                                <p>Start using the campus management features</p>
                            </div>
                        </div>
                    </div>
                </div>
            </main>

            {/* Footer */}
            <footer className="home-footer">
                <p>© 2024 ProfMojo Campus Management System</p>
                <p className="footer-links">
                    <a href="/about">About</a> • 
                    <a href="/contact">Contact</a> • 
                    <a href="/privacy">Privacy</a> • 
                    <a href="/terms">Terms</a>
                </p>
            </footer>
        </div>
    );
}