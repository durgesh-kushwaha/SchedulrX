# Smart Exam Scheduling System

Production-oriented smart exam scheduling platform with:
- Java 21 backend (Spring Boot + layered architecture)
- Constraint-based scheduling engine (greedy + backtracking + soft-penalty optimization)
- MongoDB-backed persistence (local by default)
- React frontend scaffold for role-based dashboards

## Current Implementation Status

### Completed in this phase
- Backend modernization foundation (Spring Boot app bootstrapped)
- REST API for schedule generation and retrieval
- JWT authentication + RBAC with role-protected endpoints
- Manual override endpoint with full hard-constraint revalidation
- Search/filter/pagination APIs
- SSE live update channel + notification module
- Export APIs (CSV/PDF)
- Audit log and analytics APIs
- Global API error handling and CORS/security baseline
- System architecture documentation and implementation roadmap
- React frontend project scaffold with secured login flow

### Next incremental phases
- Expanded integration/regression coverage and CI/CD hardening

## Run Backend

### Prerequisites
- Java 21
- Maven 3.9+
- MongoDB (local)

### Setup
1. Start local MongoDB on default port (`mongodb://localhost:27017`).
2. (Optional) Override database connection via environment variables:
```bash
export MONGODB_URI="mongodb://localhost:27017/exam_scheduler"
export MONGODB_DATABASE="exam_scheduler"
```
3. Start backend:
```bash
mvn spring-boot:run
```
4. API base URL:
- `http://localhost:8080/api/v1`

### Atlas later (no code change)
When you are ready to move to MongoDB Atlas, only replace env values:
```bash
export MONGODB_URI="mongodb+srv://<user>:<password>@<cluster>/<db>?retryWrites=true&w=majority"
export MONGODB_DATABASE="<db>"
```

## API (Phase 1)

## API (Current)

### Login
- `POST /api/v1/auth/login`
- Body: `{ "username": "admin", "password": "admin123" }`
- Returns: JWT token + roles

### Role-based access
- `POST /api/v1/schedules/generate` -> `ROLE_ADMIN`
- `POST /api/v1/schedules/override` -> `ROLE_ADMIN`
- `GET /api/v1/schedules` -> `ROLE_ADMIN`, `ROLE_TEACHER`, `ROLE_STUDENT`
- `GET /api/v1/analytics/overview` -> `ROLE_ADMIN`, `ROLE_TEACHER`
- `GET /api/v1/audit-logs` -> `ROLE_ADMIN`
- `GET /api/v1/notifications` -> all roles
- `GET /api/v1/schedules/export/csv|pdf` -> all roles

Default users are auto-created at startup if `app_user` is empty:
- `admin / admin123`
- `teacher / teacher123`
- `student / student123`

## Frontend (React Scaffold)

The React app scaffold is under `frontend/` and can be started independently.

```bash
cd frontend
npm install
npm run dev
```

Default dev URL: `http://localhost:5173`

## CI/CD and Validation

### GitHub Actions CI
- Workflow file: `.github/workflows/ci.yml`
- Runs on push and pull request
- Executes:
	- Backend tests: `mvn -B -ntp test`
	- Frontend install/build: `npm ci` and `npm run build` in `frontend/`

### One-command local validation
Run from repo root:
```bash
./scripts/validate.sh
```

What it does:
- Runs backend test suite
- Installs frontend dependencies with `npm ci`
- Builds frontend production bundle
- Optionally runs API smoke checks when enabled

### API smoke checks
Use against a running backend instance:
```bash
RUN_API_SMOKE=1 ./scripts/validate.sh
```

Optional environment variables for smoke auth/target:
- `API_BASE_URL` (default: `http://localhost:8080/api/v1`)
- `ADMIN_USERNAME` (default: `admin`)
- `ADMIN_PASSWORD` (default: `admin123`)

## Docs
- `docs/ARCHITECTURE.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/API_REFERENCE.md`
# SchedulrX
