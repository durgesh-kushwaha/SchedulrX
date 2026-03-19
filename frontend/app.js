const API_BASE = "http://localhost:8080/api/v1";
const TOKEN_KEY = "ses_token";

const state = {
  rows: [],
  filteredRows: [],
  analytics: null,
  notifications: [],
  auth: {
    token: localStorage.getItem(TOKEN_KEY),
    username: "",
    roles: [],
  },
};

const scheduleBody = document.getElementById("scheduleBody");
const rowCount = document.getElementById("rowCount");
const totalExams = document.getElementById("totalExams");
const scheduledExams = document.getElementById("scheduledExams");
const unplacedExams = document.getElementById("unplacedExams");
const utilization = document.getElementById("utilization");
const conflictList = document.getElementById("conflictList");
const roomUtilizationList = document.getElementById("roomUtilizationList");
const teacherLoadList = document.getElementById("teacherLoadList");
const notificationList = document.getElementById("notificationList");
const searchInput = document.getElementById("searchInput");
const statusFilter = document.getElementById("statusFilter");
const sortBy = document.getElementById("sortBy");
const generateBtn = document.getElementById("generateBtn");
const refreshBtn = document.getElementById("refreshBtn");
const loginBtn = document.getElementById("loginBtn");
const logoutBtn = document.getElementById("logoutBtn");
const usernameInput = document.getElementById("usernameInput");
const passwordInput = document.getElementById("passwordInput");
const authState = document.getElementById("authState");
const whoami = document.getElementById("whoami");
const lastUpdated = document.getElementById("lastUpdated");
const toast = document.getElementById("toast");

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("show");
  setTimeout(() => toast.classList.remove("show"), 2200);
}

function setAuthUi() {
  if (state.auth.token) {
    authState.textContent = "Signed in";
    authState.className = "pill";
    whoami.textContent = `User: ${state.auth.username || "unknown"} | Roles: ${(state.auth.roles || []).join(", ") || "-"}`;
  } else {
    authState.textContent = "Not signed in";
    authState.className = "pill pill-muted";
    whoami.textContent = "User: -";
  }
}

async function apiRequest(path, { method = "GET", body, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (auth && state.auth.token) {
    headers.Authorization = `Bearer ${state.auth.token}`;
  }

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }

  if (res.status === 204) return null;
  return res.json();
}

function mapScheduleRow(row) {
  return {
    examDate: row.examDate || "-",
    start: row.startTime || "-",
    end: row.endTime || "-",
    subjectCode: row.subjectCode || "-",
    subjectName: row.subjectName || "-",
    room: row.room || "-",
    teacher: row.teacher || "TBD",
    status: row.status || "UNPLACED",
    conflictReason: row.conflictReason || "",
  };
}

async function login() {
  const username = usernameInput.value.trim();
  const password = passwordInput.value;

  if (!username || !password) {
    showToast("Enter username and password.");
    return;
  }

  loginBtn.disabled = true;
  try {
    const data = await apiRequest("/auth/login", {
      method: "POST",
      auth: false,
      body: { username, password },
    });

    state.auth.token = data.token;
    state.auth.username = data.username;
    state.auth.roles = data.roles || [];
    localStorage.setItem(TOKEN_KEY, data.token);
    setAuthUi();
    showToast("Signed in successfully.");
    await refreshDashboard();
  } catch (err) {
    console.error(err);
    showToast("Sign in failed. Check credentials/backend.");
  } finally {
    loginBtn.disabled = false;
  }
}

function logout() {
  state.auth = { token: null, username: "", roles: [] };
  localStorage.removeItem(TOKEN_KEY);
  setAuthUi();
  showToast("Signed out.");
}

function applyFiltersAndSorting() {
  const q = searchInput.value.trim().toLowerCase();
  const status = statusFilter.value;

  let rows = state.rows.filter((r) => {
    const searchHit =
      r.subjectCode.toLowerCase().includes(q) ||
      r.subjectName.toLowerCase().includes(q) ||
      r.teacher.toLowerCase().includes(q);

    const statusHit =
      status === "all" ||
      (status === "scheduled" && r.status === "SCHEDULED") ||
      (status === "unplaced" && r.status !== "SCHEDULED");

    return searchHit && statusHit;
  });

  const key = sortBy.value;
  rows.sort((a, b) => {
    if (key === "subject") return a.subjectCode.localeCompare(b.subjectCode);
    if (key === "room") return a.room.localeCompare(b.room);
    const da = `${a.examDate}T${a.start}`;
    const db = `${b.examDate}T${b.start}`;
    return da.localeCompare(db);
  });

  state.filteredRows = rows;
  render();
}

function renderSummary() {
  const total = state.analytics?.totalExams ?? state.rows.length;
  const scheduled = state.analytics?.scheduledExams ?? state.rows.filter((r) => r.status === "SCHEDULED").length;
  const unplaced = state.analytics?.unplacedExams ?? Math.max(0, total - scheduled);
  const pct = total === 0 ? 0 : Math.round((scheduled / total) * 100);

  totalExams.textContent = String(total);
  scheduledExams.textContent = String(scheduled);
  unplacedExams.textContent = String(unplaced);
  utilization.textContent = `${pct}%`;
}

function renderTable() {
  scheduleBody.innerHTML = "";

  if (state.filteredRows.length === 0) {
    const tr = document.createElement("tr");
    tr.innerHTML = "<td data-label='Info' colspan='7'>No schedule rows found. Click Generate Schedule after signing in.</td>";
    scheduleBody.appendChild(tr);
    rowCount.textContent = "0 rows";
    return;
  }

  for (const row of state.filteredRows) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td data-label="Date">${row.examDate}</td>
      <td data-label="Start">${row.start}</td>
      <td data-label="End">${row.end}</td>
      <td data-label="Subject"><strong>${row.subjectCode}</strong> ${row.subjectName}</td>
      <td data-label="Room">${row.room}</td>
      <td data-label="Teacher">${row.teacher}</td>
      <td data-label="Status">
        <span class="status-chip ${row.status === "SCHEDULED" ? "status-scheduled" : "status-unplaced"}">
          ${row.status}
        </span>
      </td>
    `;
    scheduleBody.appendChild(tr);
  }

  rowCount.textContent = `${state.filteredRows.length} rows`;
}

function renderConflicts() {
  const conflicts = state.rows.filter((r) => r.status !== "SCHEDULED");
  conflictList.innerHTML = "";

  if (conflicts.length === 0) {
    const li = document.createElement("li");
    li.textContent = "No hard constraint violations. Schedule is clean.";
    li.style.background = "#ecfdf5";
    li.style.borderColor = "#bbf7d0";
    li.style.color = "#166534";
    conflictList.appendChild(li);
    return;
  }

  for (const c of conflicts) {
    const li = document.createElement("li");
    li.innerHTML = `<strong>${c.subjectCode}</strong> ${c.conflictReason || "Unplaced by scheduler"}`;
    conflictList.appendChild(li);
  }
}

function renderAnalyticsLists() {
  roomUtilizationList.innerHTML = "";
  teacherLoadList.innerHTML = "";

  const roomRows = state.analytics?.roomUtilization || [];
  const teacherRows = state.analytics?.teacherLoad || [];

  if (roomRows.length === 0) {
    roomUtilizationList.innerHTML = "<li>No analytics data</li>";
  } else {
    for (const r of roomRows.slice(0, 8)) {
      const li = document.createElement("li");
      li.textContent = `${r.name}: ${r.count}`;
      roomUtilizationList.appendChild(li);
    }
  }

  if (teacherRows.length === 0) {
    teacherLoadList.innerHTML = "<li>No analytics data</li>";
  } else {
    for (const t of teacherRows.slice(0, 8)) {
      const li = document.createElement("li");
      li.textContent = `${t.name}: ${t.count}`;
      teacherLoadList.appendChild(li);
    }
  }
}

function renderNotifications() {
  notificationList.innerHTML = "";
  if (state.notifications.length === 0) {
    notificationList.innerHTML = "<li>No notifications.</li>";
    return;
  }

  for (const n of state.notifications.slice(0, 10)) {
    const li = document.createElement("li");
    li.innerHTML = `<strong>${n.title}</strong><br><span>${n.message}</span>`;
    notificationList.appendChild(li);
  }
}

function render() {
  renderSummary();
  renderTable();
  renderConflicts();
  renderAnalyticsLists();
  renderNotifications();

  const ts = new Date().toLocaleString();
  lastUpdated.textContent = `Last updated: ${ts}`;
}

async function fetchScheduleData() {
  const data = await apiRequest("/schedules?page=0&size=200", { auth: true });
  return (data.items || []).map(mapScheduleRow);
}

async function fetchAnalytics() {
  try {
    state.analytics = await apiRequest("/analytics/overview", { auth: true });
  } catch (err) {
    console.error(err);
    state.analytics = null;
  }
}

async function fetchNotifications() {
  try {
    const data = await apiRequest("/notifications?page=0&size=20", { auth: true });
    state.notifications = data.items || [];
  } catch (err) {
    console.error(err);
    state.notifications = [];
  }
}

async function generateSchedule() {
  if (!state.auth.token) {
    showToast("Sign in first to generate schedule.");
    return;
  }

  generateBtn.disabled = true;
  refreshBtn.disabled = true;
  showToast("Running scheduling engine...");

  try {
    await apiRequest("/schedules/generate", { method: "POST", auth: true });
    await refreshDashboard();
    showToast("Schedule generated.");
  } catch (err) {
    console.error(err);
    showToast("Generate failed. Ensure backend is running and authenticated.");
  } finally {
    generateBtn.disabled = false;
    refreshBtn.disabled = false;
  }
}

async function refreshDashboard() {
  if (!state.auth.token) {
    state.rows = [];
    state.filteredRows = [];
    state.analytics = null;
    state.notifications = [];
    render();
    return;
  }

  try {
    const rows = await fetchScheduleData();
    state.rows = rows;
    await Promise.all([fetchAnalytics(), fetchNotifications()]);
    applyFiltersAndSorting();
  } catch (err) {
    console.error(err);
    showToast("Failed to fetch dashboard data.");
  }
}

searchInput.addEventListener("input", applyFiltersAndSorting);
statusFilter.addEventListener("change", applyFiltersAndSorting);
sortBy.addEventListener("change", applyFiltersAndSorting);
generateBtn.addEventListener("click", generateSchedule);
refreshBtn.addEventListener("click", refreshDashboard);
loginBtn.addEventListener("click", login);
logoutBtn.addEventListener("click", logout);

(async function bootstrap() {
  setAuthUi();
  if (state.auth.token) {
    showToast("Using existing session token.");
    await refreshDashboard();
    return;
  }

  // Auto sign-in with local dev defaults so Generate works out-of-the-box.
  try {
    await login();
  } catch (err) {
    console.error(err);
  }
})();
