import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import LoginForm from "./components/LoginForm";
import RoleSelector from "./components/RoleSelector";
import ScheduleGrid from "./components/ScheduleGrid";
import OverrideForm from "./components/OverrideForm";
import NotificationsPanel from "./components/NotificationsPanel";
import AnalyticsPanel from "./components/AnalyticsPanel";
import AuditPanel from "./components/AuditPanel";
import {
  analyticsOverview,
  clearSession,
  deleteNotification,
  downloadCsv,
  downloadPdf,
  generateSchedule,
  listAuditLogs,
  listNotifications,
  listSchedule,
  login,
  markNotificationRead,
  overrideSchedule,
  readSession,
  saveSession,
  sseUrl,
} from "./api";

function LoginPage({ onLoggedIn }) {
  const mutation = useMutation({
    mutationFn: ({ username, password }) => login(username, password),
    onSuccess: (payload) => {
      const session = saveSession(payload);
      onLoggedIn(session);
    },
  });

  return (
    <main className="login-layout">
      <section className="login-hero">
        <p className="hero-tag">Smart Exam Scheduling</p>
        <h1>Unified Examination Operations Platform</h1>
        <p>
          Generate conflict-aware schedules, coordinate overrides, monitor room utilization,
          and run role-specific workflows from one command surface.
        </p>
      </section>
      <LoginForm
        onSubmit={(username, password) => mutation.mutate({ username, password })}
        isPending={mutation.isPending}
        errorMessage={mutation.error?.message ?? ""}
      />
    </main>
  );
}

function DashboardPage({ session, onSessionUpdate, onLogout }) {
  const [activeRole, setActiveRole] = useState(session.roles[0] ?? "STUDENT");
  const [activeView, setActiveView] = useState("schedule");
  const [filters, setFilters] = useState({ status: "", teacher: "", subject: "", page: 0, size: 50 });
  const [liveMessage, setLiveMessage] = useState("");

  useEffect(() => {
    if (!session.roles.includes(activeRole)) {
      setActiveRole(session.roles[0] ?? "STUDENT");
    }
  }, [activeRole, session.roles]);

  const scheduleQuery = useQuery({
    queryKey: ["schedule", filters],
    queryFn: () => listSchedule(filters),
  });

  const notificationsQuery = useQuery({
    queryKey: ["notifications"],
    queryFn: () => listNotifications({ page: 0, size: 25 }),
  });

  const analyticsQuery = useQuery({
    queryKey: ["analytics", activeRole],
    queryFn: analyticsOverview,
    enabled: activeRole === "ADMIN" || activeRole === "TEACHER",
  });

  const auditQuery = useQuery({
    queryKey: ["audit", activeRole],
    queryFn: () => listAuditLogs({ page: 0, size: 25 }),
    enabled: activeRole === "ADMIN",
  });

  const generateMutation = useMutation({
    mutationFn: generateSchedule,
    onSuccess: () => {
      setLiveMessage("New schedule generation was triggered.");
      scheduleQuery.refetch();
      notificationsQuery.refetch();
      analyticsQuery.refetch();
      auditQuery.refetch();
    },
    onError: (error) => setLiveMessage(error.message),
  });

  const overrideMutation = useMutation({
    mutationFn: overrideSchedule,
    onSuccess: () => {
      setLiveMessage("Override applied successfully.");
      scheduleQuery.refetch();
      notificationsQuery.refetch();
      analyticsQuery.refetch();
      auditQuery.refetch();
    },
  });

  const markReadMutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => notificationsQuery.refetch(),
    onError: (error) => setLiveMessage(error.message),
  });

  const deleteNotificationMutation = useMutation({
    mutationFn: deleteNotification,
    onSuccess: () => {
      setLiveMessage("Notification removed.");
      notificationsQuery.refetch();
    },
    onError: (error) => setLiveMessage(error.message),
  });

  useEffect(() => {
    const events = new EventSource(sseUrl());
    events.onmessage = () => {
      scheduleQuery.refetch();
      notificationsQuery.refetch();
      analyticsQuery.refetch();
      auditQuery.refetch();
    };
    events.addEventListener("schedule.generated", () => setLiveMessage("Real-time update: schedule generated."));
    events.addEventListener("schedule.override", () => setLiveMessage("Real-time update: override recorded."));
    events.onerror = () => setLiveMessage("Live channel interrupted. Data may be delayed.");
    return () => events.close();
  }, []);

  const rows = scheduleQuery.data?.items ?? [];
  const notifications = notificationsQuery.data?.items ?? [];
  const audits = auditQuery.data?.items ?? [];

  const stats = useMemo(() => {
    const scheduled = rows.filter((row) => row.status === "SCHEDULED").length;
    const conflicts = rows.filter((row) => row.status !== "SCHEDULED").length;
    return {
      total: scheduleQuery.data?.total ?? rows.length,
      scheduled,
      conflicts,
      unread: notifications.filter((item) => !item.isRead).length,
    };
  }, [notifications, rows, scheduleQuery.data?.total]);

  const menu = [
    { key: "schedule", label: "Schedule board" },
    { key: "notifications", label: "Notification center" },
    { key: "analytics", label: "Analytics" },
    { key: "override", label: "Override desk" },
    { key: "audit", label: "Audit trail" },
  ];

  const visibleMenu = menu.filter((item) => {
    if (item.key === "override" || item.key === "audit") return activeRole === "ADMIN";
    if (item.key === "analytics") return activeRole === "ADMIN" || activeRole === "TEACHER";
    return true;
  });

  useEffect(() => {
    if (!visibleMenu.find((item) => item.key === activeView)) {
      setActiveView(visibleMenu[0].key);
    }
  }, [activeView, visibleMenu]);

  return (
    <div className="workspace-layout">
      <header className="workspace-top">
        <div>
          <p className="hero-tag">Operations Console</p>
          <h1>Exam Control Matrix</h1>
          <p className="workspace-sub">Signed in as {session.username || "unknown"}</p>
        </div>
        <div className="workspace-actions">
          <button
            type="button"
            className="cta-btn"
            disabled={activeRole !== "ADMIN" || generateMutation.isPending}
            onClick={() => generateMutation.mutate()}
          >
            {generateMutation.isPending ? "Generating" : "Generate Schedule"}
          </button>
          <button type="button" className="ghost-btn" onClick={downloadCsv}>Export CSV</button>
          <button type="button" className="ghost-btn" onClick={downloadPdf}>Export PDF</button>
          <button
            type="button"
            className="ghost-btn"
            onClick={() => {
              clearSession();
              onSessionUpdate(null);
              onLogout();
            }}
          >
            Sign Out
          </button>
        </div>
      </header>

      {liveMessage && <div className="live-banner">{liveMessage}</div>}

      <RoleSelector activeRole={activeRole} onChange={setActiveRole} allowedRoles={session.roles} />

      <section className="metric-strip">
        <article>
          <h4>Total Exams</h4>
          <strong>{stats.total}</strong>
        </article>
        <article>
          <h4>Scheduled</h4>
          <strong>{stats.scheduled}</strong>
        </article>
        <article>
          <h4>Conflict or Unplaced</h4>
          <strong>{stats.conflicts}</strong>
        </article>
        <article>
          <h4>Unread Alerts</h4>
          <strong>{stats.unread}</strong>
        </article>
      </section>

      <nav className="view-tabs">
        {visibleMenu.map((item) => (
          <button
            key={item.key}
            type="button"
            className={activeView === item.key ? "view-tab active" : "view-tab"}
            onClick={() => setActiveView(item.key)}
          >
            {item.label}
          </button>
        ))}
      </nav>

      {activeView === "schedule" && (
        <>
          <section className="filter-line">
            <input
              placeholder="Filter by subject or code"
              value={filters.subject}
              onChange={(e) => setFilters((state) => ({ ...state, subject: e.target.value, page: 0 }))}
            />
            <input
              placeholder="Filter by teacher"
              value={filters.teacher}
              onChange={(e) => setFilters((state) => ({ ...state, teacher: e.target.value, page: 0 }))}
            />
            <select
              value={filters.status}
              onChange={(e) => setFilters((state) => ({ ...state, status: e.target.value, page: 0 }))}
            >
              <option value="">All statuses</option>
              <option value="SCHEDULED">Scheduled</option>
              <option value="CONFLICT">Conflict</option>
              <option value="UNPLACED">Unplaced</option>
            </select>
          </section>
          <ScheduleGrid
            title={activeRole === "STUDENT" ? "Student timeline" : "Schedule board"}
            rows={activeRole === "TEACHER" ? rows.filter((row) => row.teacher && row.teacher !== "TBD") : rows}
            isLoading={scheduleQuery.isLoading || scheduleQuery.isFetching}
            errorMessage={scheduleQuery.error?.message ?? ""}
          />
        </>
      )}

      {activeView === "notifications" && (
        <NotificationsPanel
          notifications={notifications}
          onMarkRead={(id) => markReadMutation.mutate(id)}
          onDelete={(id) => {
            const confirmed = window.confirm("Remove this notification?");
            if (confirmed) {
              deleteNotificationMutation.mutate(id);
            }
          }}
          canDelete={activeRole === "ADMIN"}
          isActionPending={markReadMutation.isPending || deleteNotificationMutation.isPending}
          isLoading={notificationsQuery.isLoading || notificationsQuery.isFetching}
          errorMessage={notificationsQuery.error?.message ?? ""}
        />
      )}

      {activeView === "analytics" && (
        <AnalyticsPanel
          data={analyticsQuery.data}
          isLoading={analyticsQuery.isLoading || analyticsQuery.isFetching}
          errorMessage={analyticsQuery.error?.message ?? ""}
        />
      )}

      {activeView === "override" && (
        <OverrideForm
          onSubmit={(payload) => overrideMutation.mutate(payload)}
          isPending={overrideMutation.isPending}
          errorMessage={overrideMutation.error?.message ?? ""}
        />
      )}

      {activeView === "audit" && (
        <AuditPanel
          records={audits}
          isLoading={auditQuery.isLoading || auditQuery.isFetching}
          errorMessage={auditQuery.error?.message ?? ""}
        />
      )}
    </div>
  );
}

function App() {
  const [session, setSession] = useState(() => readSession());
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (session && location.pathname === "/login") {
      navigate("/app", { replace: true });
    }
    if (!session && location.pathname !== "/login") {
      navigate("/login", { replace: true });
    }
  }, [location.pathname, navigate, session]);

  return (
    <Routes>
      <Route
        path="/login"
        element={session ? <Navigate to="/app" replace /> : <LoginPage onLoggedIn={setSession} />}
      />
      <Route
        path="/app"
        element={session ? <DashboardPage session={session} onSessionUpdate={setSession} onLogout={() => navigate("/login", { replace: true })} /> : <Navigate to="/login" replace />}
      />
      <Route path="*" element={<Navigate to={session ? "/app" : "/login"} replace />} />
    </Routes>
  );
}

export default App;
