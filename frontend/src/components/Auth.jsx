import React, { useState } from "react";
import { Alert, Field } from "./UI.jsx";
import { errorText } from "../utils.js";
import { request } from "../api.js";

function Auth({ mode, switchMode, done }) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    const values = Object.fromEntries(new FormData(e.currentTarget));
    try {
      done(await request(`/auth/${mode}`, { method: "POST", body: values }));
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      <p className="muted">
        {mode === "login"
          ? "Войдите, чтобы управлять своими поездками."
          : "Ваши бронирования будут собраны в одном месте."}
      </p>
      <Field label="Логин">
        <input
          name="username"
          required
          pattern="[a-zA-Z0-9_.-]{3,50}"
          autoComplete="username"
          placeholder="От 3 символов латиницей"
        />
      </Field>
      <Field label="Пароль">
        <input
          name="password"
          type="password"
          required
          minLength={8}
          maxLength={72}
          autoComplete={mode === "login" ? "current-password" : "new-password"}
        />
      </Field>
      <Alert>{error}</Alert>
      <button className="button full" disabled={busy}>
        {busy
          ? "Подождите…"
          : mode === "login"
            ? "Войти"
            : "Зарегистрироваться"}
      </button>
      <button
        className="text-button full"
        type="button"
        onClick={() => {
          setError("");
          switchMode();
        }}
      >
        {mode === "login"
          ? "Ещё нет аккаунта? Зарегистрироваться"
          : "Уже есть аккаунт? Войти"}
      </button>
      <div className="demo-note">
        Демо: alice / User12345!
        <br />
        Администратор: admin / Admin123!
      </div>
    </form>
  );
}

export default Auth;
