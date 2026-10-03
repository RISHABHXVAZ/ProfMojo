# ProfMojo 🚀

ProfMojo is a full-stack campus classroom and facility management application built with:
- **Spring Boot 3** (Java 17 Backend)
- **React + Vite** (Frontend)
- **PostgreSQL 15** (Relational Database)
- **Redis 7** (In-Memory Datastore: OTP, Rate Limiting & Token Revocation)
- **Prometheus** (Metrics & Observability)
- **Docker & Docker Compose**

---

## 🔧 Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- Docker Compose enabled
- Git

*(No local Java, Node.js, PostgreSQL, or Redis installation required when running via Docker)*

---

## ▶ Quickstart Setup Guide

### 1. Clone the Repository
```bash
git clone https://github.com/RISHABHXVAZ/ProfMojo.git
cd ProfMojo
```

### 2. Create and Configure Environment Variables
ProfMojo requires specific environment variables (such as `JWT_SECRET` and `REDIS_PASSWORD`) to initialize the application and security layers. Copy `.env.example` to `.env`:

**On Linux / macOS (Bash):**
```bash
cp .env.example .env
```

**On Windows (PowerShell):**
```powershell
Copy-Item .env.example .env
```

Open `.env` in your text editor and review the required variables:
- `JWT_SECRET`: Secret key for HMAC-SHA256 token signing (must be at least 32 characters).
- `REDIS_PASSWORD`: Password for the Redis container.
- `ADMIN_DEPARTMENT_SEEDS`: Initial department admin seed credentials (`DEPARTMENT:EMAIL:SECRET_KEY`).
- `MAIL_USERNAME` / `MAIL_PASSWORD`: Optional SMTP credentials for real email delivery (defaults to log-only delivery if left empty).

### 3. Build and Start All Services
From the project root:

```bash
docker compose up --build
```

### 4. Access the Application
Once the containers report healthy:
- **Frontend Web UI**: [http://localhost:5173](http://localhost:5173)
- **Backend API & Health**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Prometheus Metrics**: [http://localhost:9090](http://localhost:9090)

To stop the stack:
```bash
docker compose down
```
