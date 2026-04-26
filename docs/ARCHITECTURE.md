# System Architecture

## 1. High-Level Design

The platform follows a modular monolith architecture with clean layering:

1. Presentation Layer: REST controllers + WebSocket gateways
2. Application Layer: use-case services (generate schedule, reschedule, export)
3. Domain Layer: scheduling engine and constraint model
4. Data Access Layer: repositories/DAOs for persistent storage
5. Infrastructure Layer: DB, logging, security, messaging, file export

## 2. Backend Structure

### Package design
- `com.examscheduler.api`: REST controllers, exception handlers, DTO mapping
- `com.examscheduler.service`: orchestration and application services
- `com.examscheduler.constraints`: hard and soft constraints
- `com.examscheduler.dao`: persistence adapters
- `com.examscheduler.model`: domain entities
- `com.examscheduler.config`: security, CORS, app configuration

### Request flow
1. Admin triggers timetable generation.
2. Service loads exams, rooms, slots, enrollment graph.
3. Engine executes heuristic scheduling:
   - priority sorting (CORE first, high enrollment first)
   - greedy placement
   - backtracking for unresolved exams
   - soft-penalty optimization by local swaps
4. Result persisted to `scheduled_exam`.
5. API returns schedule + conflict summary.

## 3. Scheduling Strategy

### Hard constraints (must always pass)
- No student overlap
- No teacher overlap
- Room capacity respected
- Slot-time overlap validation

### Soft constraints (optimize quality)
- Minimum gap between exams for same student cohort
- Cap preferred maximum daily exam load per student cohort
- Prefer morning slots for CORE subjects
- Subject prioritization and load balancing
- Optional room preference (future phase)

### Algorithm choice
Hybrid CSP approach:
- Greedy with Most Constrained Variable ordering for fast initial solution
- Backtracking for conflict repair when greedy fails
- Local optimization loop to reduce soft-constraint penalty score

Why this approach:
- Performs better than pure brute-force on realistic datasets
- Explainable and maintainable for academic admin workflows
- Supports incremental enhancements (weights, strategy plugins)

### What-if simulation mode
- API endpoint provides non-persistent, ranked schedule alternatives.
- Supports blocked rooms, blocked slots, blocked dates, and configurable min-gap.
- Supports strategy toggle: `HYBRID` (greedy + repair + optimization) or `GREEDY_ONLY`.
- Returns per-alternative metrics (unplaced exams, soft penalty score, runtime).

## 4. Database Design

Core relational model:
- `student`, `teacher`, `room`, `time_slot`, `exam`, `enrollment`, `scheduled_exam`

Scalability considerations:
- Composite keys on enrollment relation
- Join indexes on scheduling-critical columns
- Separate output table for generated schedules
- Future: add `schedule_run`, `audit_log`, `notification`, `exam_override`

## 5. Frontend Architecture (React)

### UI modules
- Authentication + role gates (admin/teacher/student)
- Dashboard per role
- Timetable board/calendar view
- Conflict panel and validation messages
- Manual drag/drop adjustment workspace
- Notification center
- Analytics cards

### State and integration
- React Query for server state
- Axios for API client
- Route-level guards for RBAC
- WebSocket hook for live schedule updates (future phase)

## 6. Security and Observability

- Spring Security baseline now enabled
- Planned JWT auth with refresh token rotation
- Audit logs for manual override and rescheduling
- Actuator health endpoint for monitoring
- Structured logs with correlation ID (future phase)

## 7. Edge Cases Addressed

- Insufficient room capacity -> mark exam unplaced with reason
- Student/teacher overlap -> hard constraint fail, retry via backtracking
- Last-minute room/slot outage -> reschedule endpoint (planned)
- Partial timetable generation -> conflict report + actionable reasons
