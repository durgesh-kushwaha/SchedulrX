import { useState } from "react";

function LoginForm({ onSubmit, isPending, errorMessage }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [localError, setLocalError] = useState("");

  const submit = (event) => {
    event.preventDefault();
    if (!username.trim() || !password.trim()) {
      setLocalError("Username and password are required");
      return;
    }
    setLocalError("");
    onSubmit(username.trim(), password);
  };

  return (
    <section className="login-panel">
      <div className="login-head">
        <h2>Welcome Back</h2>
        <p>Sign in to manage live exam operations with role-based access.</p>
      </div>

      <form onSubmit={submit} className="login-form">
        <label>
          Username
          <input
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Enter your username"
          />
        </label>

        <label>
          Password
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter your password"
          />
        </label>

        {(localError || errorMessage) && <div className="inline-error">{localError || errorMessage}</div>}

        <button className="cta-btn" type="submit" disabled={isPending}>
          {isPending ? "Signing in" : "Enter Platform"}
        </button>
      </form>

      <div className="login-quick">
        <button type="button" onClick={() => { setUsername("admin"); setPassword("admin123"); }}>Use admin demo</button>
        <button type="button" onClick={() => { setUsername("teacher"); setPassword("teacher123"); }}>Use teacher demo</button>
        <button type="button" onClick={() => { setUsername("student"); setPassword("student123"); }}>Use student demo</button>
      </div>
    </section>
  );
}

export default LoginForm;
