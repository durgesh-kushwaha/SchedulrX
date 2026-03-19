const API_BASE = "http://localhost:8080/api/v1";
const TOKEN_KEY = "ses_token";

const tabLogin = document.getElementById("tabLogin");
const tabSignup = document.getElementById("tabSignup");
const loginForm = document.getElementById("loginForm");
const signupForm = document.getElementById("signupForm");
const authMessage = document.getElementById("authMessage");
const continueSessionBtn = document.getElementById("continueSessionBtn");

function decodeJwtPayload(token) {
  try {
    const payload = token.split(".")[1];
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function setMode(mode) {
  if (mode === "signup") {
    tabSignup.classList.add("active");
    tabLogin.classList.remove("active");
    signupForm.classList.remove("hidden");
    loginForm.classList.add("hidden");
  } else {
    tabLogin.classList.add("active");
    tabSignup.classList.remove("active");
    loginForm.classList.remove("hidden");
    signupForm.classList.add("hidden");
  }
  authMessage.textContent = "";
}

function showMessage(message, isError = false) {
  authMessage.textContent = message;
  authMessage.className = isError ? "auth-message error" : "auth-message success";
}

async function request(path, body) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json();
}

async function doLogin(username, password) {
  const data = await request("/auth/login", { username, password });
  localStorage.setItem(TOKEN_KEY, data.token);
  window.location.href = "dashboard.html";
}

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const username = document.getElementById("loginUsername").value.trim();
  const password = document.getElementById("loginPassword").value;

  try {
    await doLogin(username, password);
  } catch (error) {
    console.error(error);
    showMessage("Sign in failed. Check credentials or backend status.", true);
  }
});

signupForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const username = document.getElementById("signupUsername").value.trim();
  const password = document.getElementById("signupPassword").value;
  const confirm = document.getElementById("signupConfirm").value;
  const role = document.getElementById("signupRole").value;

  if (password !== confirm) {
    showMessage("Passwords do not match.", true);
    return;
  }

  try {
    await request("/auth/signup", { username, password, role });
    showMessage("Account created. Signing you in...");
    await doLogin(username, password);
  } catch (error) {
    console.error(error);
    showMessage("Sign up failed. Try a different username.", true);
  }
});

tabLogin.addEventListener("click", () => setMode("login"));
tabSignup.addEventListener("click", () => setMode("signup"));

const existingToken = localStorage.getItem(TOKEN_KEY);
if (existingToken) {
  const payload = decodeJwtPayload(existingToken);
  const who = payload?.sub || "current user";
  showMessage(`You already have an active session as ${who}. You can continue or sign in with another account.`);
  continueSessionBtn.classList.remove("hidden");
}

continueSessionBtn.addEventListener("click", () => {
  window.location.href = "dashboard.html";
});
