import axios from "axios";

export const TOKEN_KEY = "exam_scheduler_token";
export const SESSION_KEY = "exam_scheduler_session";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function normalizeError(error) {
  const responseMessage = error?.response?.data?.message;
  const responseError = error?.response?.data?.error;
  return responseMessage || responseError || error?.message || "Request failed";
}

export function saveSession(data) {
  const roles = Array.isArray(data?.roles)
    ? data.roles.map((role) => String(role).replace("ROLE_", "")).filter(Boolean)
    : [];
  const token = data?.token ?? "";
  const username = data?.username ?? "";
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({ token, username, roles: roles.length ? roles : ["STUDENT"] })
  );
  return { token, username, roles: roles.length ? roles : ["STUDENT"] };
}

export function readSession() {
  const raw = localStorage.getItem(SESSION_KEY);
  const token = localStorage.getItem(TOKEN_KEY);
  if (!raw || !token) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw);
    if (!parsed?.token) {
      return null;
    }
    return {
      token: parsed.token,
      username: parsed.username ?? "",
      roles: Array.isArray(parsed.roles) && parsed.roles.length ? parsed.roles : ["STUDENT"],
    };
  } catch {
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(SESSION_KEY);
}

export async function login(username, password) {
  try {
    const { data } = await api.post("/auth/login", { username, password });
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function generateSchedule() {
  try {
    const { data } = await api.post("/schedules/generate");
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function loadPlanningDataset() {
  try {
    const { data } = await api.get("/planning/dataset");
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function savePlanningDataset(payload) {
  try {
    const { data } = await api.put("/planning/dataset", payload);
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function loadPlanningTemplate() {
  try {
    const { data } = await api.get("/planning/template");
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function listSchedule(params = {}) {
  try {
    const { data } = await api.get("/schedules", { params });
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function overrideSchedule(payload) {
  try {
    const { data } = await api.post("/schedules/override", payload);
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function simulateSchedules(payload = {}) {
  try {
    const { data } = await api.post("/schedules/simulate", payload);
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function analyticsOverview() {
  try {
    const { data } = await api.get("/analytics/overview");
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function listAuditLogs(params = {}) {
  try {
    const { data } = await api.get("/audit-logs", { params });
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function listNotifications(params = {}) {
  try {
    const { data } = await api.get("/notifications", { params });
    return data;
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function markNotificationRead(id) {
  try {
    await api.patch(`/notifications/${id}/read`);
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

export async function deleteNotification(id) {
  try {
    await api.delete(`/notifications/${id}`);
  } catch (error) {
    throw new Error(normalizeError(error));
  }
}

async function downloadFile(url, filename) {
  const response = await api.get(url, { responseType: "blob" });
  const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement("a");
  link.href = blobUrl;
  link.setAttribute("download", filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}

export async function downloadCsv() {
  await downloadFile("/schedules/export/csv", "schedule.csv");
}

export async function downloadPdf() {
  await downloadFile("/schedules/export/pdf", "schedule.pdf");
}

export function sseUrl() {
  const token = localStorage.getItem(TOKEN_KEY);
  return `${API_BASE_URL}/events/schedules?token=${encodeURIComponent(token ?? "")}`;
}
