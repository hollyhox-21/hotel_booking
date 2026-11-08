import React, { useEffect, useState } from "react";
import { request } from "./api.js";
import { Icon, Alert, Modal } from "./components/UI.jsx";
import Auth from "./components/Auth.jsx";
import Search from "./pages/Search.jsx";
import Bookings from "./pages/Bookings.jsx";
import Hotels from "./pages/Hotels.jsx";
import Users from "./pages/Users.jsx";
import Statistics from "./pages/Statistics.jsx";

function App() {
  const [session, setSession] = useState(() => {
    try {
      return JSON.parse(sessionStorage.getItem("session")) || null;
    } catch {
      return null;
    }
  });
  const [tab, setTab] = useState("search");
  const [authMode, setAuthMode] = useState(null);
  const [notice, setNotice] = useState("");
  const admin = session?.user.role === "ADMIN";
  function logout() {
    sessionStorage.clear();
    setSession(null);
    setTab("search");
    setNotice("");
  }
  useEffect(() => {
    if (!session) return;
    request("/auth/me", { token: session.token })
      .then((user) => {
        if (user.role !== session.user.role) logout();
      })
      .catch((e) => {
        if (e.status === 401) logout();
      });
  }, [session?.token]);
  const api = (path, options = {}) =>
    request(path, { token: session?.token, ...options }).catch((e) => {
      if (e.status === 401 && session) {
        logout();
        setNotice("Сессия завершена. Войдите снова.");
      }
      throw e;
    });
  function signedIn(value) {
    sessionStorage.setItem("session", JSON.stringify(value));
    setSession(value);
    setAuthMode(null);
    setTab(value.user.role === "ADMIN" ? "hotels" : "search");
    setNotice("");
  }
  const selectedTab = admin && tab === "search" ? "hotels" : tab;
  return (
    <div className="app">
      <aside className="sidebar">
        <a
          href="#"
          className="brand"
          onClick={(e) => {
            e.preventDefault();
            setTab(admin ? "hotels" : "search");
          }}
        >
          <span className="brand-icon">
            <Icon />
          </span>
          <span>
            Тихие места<small>СЕРВИС БРОНИРОВАНИЯ</small>
          </span>
        </a>
        <div className="nav-label">
          {admin ? "УПРАВЛЕНИЕ" : "ВАШЕ ПУТЕШЕСТВИЕ"}
        </div>
        <nav>
          {(admin
            ? [
                ["hotels", "hotel", "Отели и номера"],
                ["users", "user", "Пользователи"],
                ["stats", "chart", "Статистика"],
              ]
            : [
                ["search", "search", "Найти номер"],
                ["bookings", "calendar", "Мои бронирования"],
              ]
          ).map(([key, icon, label]) => (
            <button
              key={key}
              className={selectedTab === key ? "active" : ""}
              onClick={() => {
                if (!session && key === "bookings") setAuthMode("login");
                else setTab(key);
              }}
            >
              <Icon name={icon} />
              {label}
              {selectedTab === key && <span className="nav-dot" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-note">
          <span className="tiny-star">✳</span>
          <p>
            Место, куда
            <br />
            хочется вернуться.
          </p>
          <small>Отели для ваших планов.</small>
        </div>
        <div className="profile">
          {session ? (
            <>
              <span className="avatar">
                {session.user.username[0].toUpperCase()}
              </span>
              <span>
                <strong>{session.user.username}</strong>
                <small>{admin ? "Администратор" : "Путешественник"}</small>
              </span>
              <button
                className="icon-button"
                onClick={logout}
                aria-label="Выйти"
              >
                <Icon name="exit" size={19} />
              </button>
            </>
          ) : (
            <button
              className="button full"
              onClick={() => setAuthMode("login")}
            >
              Войти в аккаунт <Icon name="arrow" size={18} />
            </button>
          )}
        </div>
      </aside>
      <main>
        <header className="topbar">
          <span>
            {admin
              ? "Личный кабинет администратора"
              : "Хорошие поездки начинаются здесь"}
          </span>
          <span className="topbar-tag">
            <span /> {admin ? "Управление отелями" : "Бронирование онлайн"}
          </span>
        </header>
        <div className="content">
          <Alert>{notice}</Alert>
          {admin ? (
            selectedTab === "users" ? (
              <Users api={api} currentId={session.user.id} />
            ) : selectedTab === "stats" ? (
              <Statistics api={api} />
            ) : (
              <Hotels api={api} />
            )
          ) : selectedTab === "bookings" && session ? (
            <Bookings api={api} />
          ) : (
            <Search
              api={api}
              session={session}
              login={() => setAuthMode("login")}
              booked={() => {
                setTab("bookings");
              }}
            />
          )}
        </div>
        <footer>
          Тихие места <span>Планируйте поездку в своём ритме.</span>
        </footer>
      </main>
      {authMode && (
        <Modal
          title={authMode === "login" ? "С возвращением" : "Создайте аккаунт"}
          close={() => setAuthMode(null)}
        >
          <Auth
            mode={authMode}
            switchMode={() =>
              setAuthMode(authMode === "login" ? "register" : "login")
            }
            done={signedIn}
          />
        </Modal>
      )}
    </div>
  );
}

export default App;
