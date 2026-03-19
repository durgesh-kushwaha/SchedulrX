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

if (!state.auth.token) {
  window.location.href = "auth.html";
}

const refs = {
  scheduleBody: document.getElementById("scheduleBody"),
  rowCount: document.getElementById("rowCount"),
  totalExams: document.getElementById("totalExams"),
  scheduledExams: document.getElementById("scheduledExams"),
  unplacedExams: document.getElementById("unplacedExams"),
  utilization: document.getElementById("utilization"),
  conflictList: document.getElementById("conflictList"),
  roomUtilizationList: document.getElementById("roomUtilizationList"),
  teacherLoadList: document.getElementById("teacherLoadList"),
  notificationList: document.getElementById("notificationList"),
  searchInput: document.getElementById("searchInput"),
  statusFilter: document.getElementById("statusFilter"),
  sortBy: document.getElementById("sortBy"),
  generateBtn: document.getElementById("generateBtn"),
  refreshBtn: document.getElementById("refreshBtn"),
  downloadCsvBtn: document.getElementById("downloadCsvBtn"),
  downloadPdfBtn: document.getElementById("downloadPdfBtn"),
  logoutBtn: document.getElementById("logoutBtn"),
  whoami: document.getElementById("whoami"),
  roleBadge: document.getElementById("roleBadge"),
  adminWarning: document.getElementById("adminWarning"),
  lastUpdated: document.getElementById("lastUpdated"),
  toast: document.getElementById("toast"),
};

function showToast(message) {
  refs.toast.textContent = message;
  refs.toast.classList.add("show");
  setTimeout(() => refs.toast.classList.remove("show"), 2200);
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split(".")[1];
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function updateRoleUi() {
  const roles = state.auth.roles || [];
  const isAdmin = roles.includes("ROLE_ADMIN");
  const label = roles.length ? roles.join(", ") : "Unknown";

  refs.roleBadge.textContent = `Role: ${label}`;

  if (isAdmin) {
    refs.roleBadge.className = "pill";
    refs.adminWarning.classList.add("hidden");
    refs.generateBtn.disabled = false;
    refs.generateBtn.title = "Generate schedule";
  } else {
    refs.roleBadge.className = "pill pill-muted";
    refs.adminWarning.classList.remove("hidden");
    refs.generateBtn.disabled = true;
    refs.generateBtn.title = "Only ROLE_ADMIN can generate schedule";
  }
}

async function apiRequest(path, { method = "GET", body } = {}) {
  const headers = { "Content-Type": "application/json", Authorization: `Bearer ${state.auth.token}` };
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 || res.status === 403) {
    localStorage.removeItem(TOKEN_KEY);
    window.location.href = "auth.html";
    return null;
  }

  if (!res.ok) {
    const text = await res.text();
    const err = new Error(text || `HTTP ${res.status}`);
    err.status = res.status;
    throw err;
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

function applyFiltersAndSorting() {
  const q = refs.searchInput.value.trim().toLowerCase();
  const status = refs.statusFilter.value;

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

  const key = refs.sortBy.value;
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

  refs.totalExams.textContent = String(total);
  refs.scheduledExams.textContent = String(scheduled);
  refs.unplacedExams.textContent = String(unplaced);
  refs.utilization.textContent = `${pct}%`;
}

function renderTable() {
  refs.scheduleBody.innerHTML = "";

  if (state.filteredRows.length === 0) {
    const tr = document.createElement("tr");
    tr.innerHTML = "<td data-label='Info' colspan='7'>No rows found.</td>";
    refs.scheduleBody.appendChild(tr);
    refs.rowCount.textContent = "0 rows";
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
      <td data-label="Status"><span class="status-chip ${row.status === "SCHEDULED" ? "status-scheduled" : "status-unplaced"}">${row.status}</span></td>
    `;
    refs.scheduleBody.appendChild(tr);
  }

  refs.rowCount.textContent = `${state.filteredRows.length} rows`;
}

function renderConflicts() {
  const conflicts = state.rows.filter((r) => r.status !== "SCHEDULED");
  refs.conflictList.innerHTML = "";

  if (conflicts.length === 0) {
    refs.conflictList.innerHTML = "<li>No active conflicts.</li>";
    return;
  }

  for (const c of conflicts) {
    const li = document.createElement("li");
    li.innerHTML = `<strong>${c.subjectCode}</strong> ${c.conflictReason || "Unplaced by scheduler"}`;
    refs.conflictList.appendChild(li);
  }
}

function renderList(target, rows, formatter) {
  target.innerHTML = "";
  if (!rows.length) {
    target.innerHTML = "<li>No data.</li>";
    return;
  }
  for (const row of rows) {
    const li = document.createElement("li");
    li.innerHTML = formatter(row);
    target.appendChild(li);
  }
}

function render() {
  renderSummary();
  renderTable();
  renderConflicts();
  renderList(refs.roomUtilizationList, state.analytics?.roomUtilization || [], (x) => `${x.name}: ${x.count}`);
  renderList(refs.teacherLoadList, state.analytics?.teacherLoad || [], (x) => `${x.name}: ${x.count}`);
  renderList(refs.notificationList, state.notifications || [], (n) => `<strong>${n.title}</strong><br>${n.message}`);
  refs.lastUpdated.textContent = `Last updated: ${new Date().toLocaleString()}`;
}

async function refreshDashboard() {
  try {
    const [scheduleData, analytics, notifications] = await Promise.all([
      apiRequest("/schedules?page=0&size=250"),
      apiRequest("/analytics/overview"),
      apiRequest("/notifications?page=0&size=20"),
    ]);

    state.rows = (scheduleData?.items || []).map(mapScheduleRow);
    state.analytics = analytics;
    state.notifications = notifications?.items || [];
    applyFiltersAndSorting();
  } catch (error) {
    console.error(error);
    showToast("Unable to load dashboard data.");
  }
}

async function loadProfile() {
  try {
    const login = await apiRequest("/notifications?page=0&size=1");
    if (login) {
      refs.whoami.textContent = "Signed in session active";
    }
  } catch {
    refs.whoami.textContent = "Session active";
  }
}

async function generateSchedule() {
  refs.generateBtn.disabled = true;
  showToast("Generating schedule...");
  try {
    await apiRequest("/schedules/generate", { method: "POST" });
    await refreshDashboard();
    showToast("Schedule generated successfully.");
  } catch (error) {
    console.error(error);
    if (error.status === 403) {
      showToast(`Generate blocked: active role is ${state.auth.roles.join(", ") || "unknown"}. Use admin account.`);
    } else {
      showToast("Generate failed. Check backend/session.");
    }
  } finally {
    refs.generateBtn.disabled = false;
  }
}

function download(path, filename) {
  fetch(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${state.auth.token}` },
  }).then(async (res) => {
    if (!res.ok) throw new Error(await res.text());
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }).catch((error) => {
    console.error(error);
    showToast("Download failed.");
  });
}

refs.searchInput.addEventListener("input", applyFiltersAndSorting);
refs.statusFilter.addEventListener("change", applyFiltersAndSorting);
refs.sortBy.addEventListener("change", applyFiltersAndSorting);
refs.generateBtn.addEventListener("click", generateSchedule);
refs.refreshBtn.addEventListener("click", refreshDashboard);
refs.downloadCsvBtn.addEventListener("click", () => download("/schedules/export/csv", "schedule.csv"));
refs.downloadPdfBtn.addEventListener("click", () => download("/schedules/export/pdf", "schedule.pdf"));
refs.logoutBtn.addEventListener("click", () => {
  localStorage.removeItem(TOKEN_KEY);
  window.location.href = "auth.html";
});

(async function bootstrap() {
  const payload = decodeJwtPayload(state.auth.token);
  state.auth.username = payload?.sub || "unknown";
  state.auth.roles = Array.isArray(payload?.roles) ? payload.roles : [];
  refs.whoami.textContent = `User: ${state.auth.username} | Roles: ${state.auth.roles.join(", ") || "-"}`;
  updateRoleUi();

  await loadProfile();
  await refreshDashboard();
})();
