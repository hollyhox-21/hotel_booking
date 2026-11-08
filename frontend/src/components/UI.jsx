import React, { useEffect } from "react";

function Icon({ name = "hotel", size = 22 }) {
  const shapes = {
    hotel: (
      <>
        <path d="M4 21V7l8-4 8 4v14M2 21h20M9 21v-5h6v5" />
        <path d="M8 9h1m6 0h1M8 12h1m6 0h1" />
      </>
    ),
    search: (
      <>
        <circle cx="10" cy="10" r="6" />
        <path d="m15 15 6 6" />
      </>
    ),
    calendar: (
      <>
        <rect x="3" y="5" width="18" height="16" rx="3" />
        <path d="M7 3v4m10-4v4M3 11h18" />
      </>
    ),
    arrow: <path d="M5 12h14m-5-5 5 5-5 5" />,
    chart: <path d="M4 20V4m0 16h17M8 16v-4m5 4V7m5 9v-6" />,
    user: (
      <>
        <circle cx="12" cy="8" r="4" />
        <path d="M4 22v-3a8 8 0 0 1 16 0v3" />
      </>
    ),
    check: <path d="m5 12 4 4L19 6" />,
    plus: <path d="M12 4v16M4 12h16" />,
    exit: <path d="M9 4H4v16h5m5-14 6 6-6 6m-6-6h12" />,
  };
  return (
    <svg
      aria-hidden="true"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {shapes[name]}
    </svg>
  );
}

function Alert({ children, success = false }) {
  return children ? (
    <div
      role={success ? "status" : "alert"}
      className={`alert ${success ? "success" : ""}`}
    >
      {children}
    </div>
  ) : null;
}

function Field({ label, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  );
}

function Modal({ title, children, close }) {
  useEffect(() => {
    const f = (e) => {
      if (e.key === "Escape") close();
    };
    window.addEventListener("keydown", f);
    const prev = document.activeElement;
    const dialog = document.querySelector("dialog");
    if (dialog && !dialog.open) dialog.showModal();
    return () => {
      window.removeEventListener("keydown", f);
      prev?.focus();
    };
  }, []);
  return (
    <dialog
      className="modal"
      onCancel={(e) => {
        e.preventDefault();
        close();
      }}
    >
      <div className="section-title">
        <h2>{title}</h2>
        <button
          type="button"
          className="icon-button"
          aria-label="Закрыть"
          onClick={close}
        >
          ×
        </button>
      </div>
      {children}
    </dialog>
  );
}

function Empty({ title, text }) {
  return (
    <div className="empty">
      <Icon name="calendar" size={38} />
      <h3>{title}</h3>
      <p>{text}</p>
    </div>
  );
}

function PageHeading({ eyebrow, title, text, children }) {
  return (
    <div className="page-heading section-title">
      <div>
        <div className="eyebrow">{eyebrow}</div>
        <h1>{title}</h1>
        <p className="muted">{text}</p>
      </div>
      {children}
    </div>
  );
}

function Pager({ data, setPage }) {
  return data.totalPages > 1 ? (
    <div className="pager">
      <button
        disabled={data.page === 0}
        className="button secondary"
        onClick={() => setPage(data.page - 1)}
      >
        Назад
      </button>
      <span>
        {data.page + 1} / {data.totalPages}
      </span>
      <button
        disabled={data.page + 1 >= data.totalPages}
        className="button secondary"
        onClick={() => setPage(data.page + 1)}
      >
        Далее
      </button>
    </div>
  ) : null;
}

export { Icon, Alert, Field, Modal, Empty, PageHeading, Pager };
