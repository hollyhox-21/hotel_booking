import React, { useEffect, useState } from "react";
import {
  Icon,
  Alert,
  Field,
  Modal,
  Empty,
  PageHeading,
  Pager,
} from "../components/UI.jsx";
import { date, fmt, nights, statusName, errorText } from "../utils.js";

function Users({ api, currentId }) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [modal, setModal] = useState(null);
  async function load() {
    try {
      setData(await api(`/users?page=${page}&size=10`));
    } catch (e) {
      setError(errorText(e));
    }
  }
  useEffect(() => {
    load();
  }, [page]);
  async function disable() {
    try {
      await api(`/users/${modal.item.id}`, { method: "DELETE" });
      setModal(null);
      load();
    } catch (e) {
      setError(errorText(e));
    }
  }
  return (
    <>
      <PageHeading
        eyebrow="АДМИНИСТРИРОВАНИЕ"
        title="Пользователи"
        text="Учётные записи, роли и доступ к системе."
      >
        <button className="button" onClick={() => setModal({})}>
          <Icon name="plus" size={18} /> Добавить пользователя
        </button>
      </PageHeading>
      <Alert>{error}</Alert>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Логин</th>
              <th>Роль</th>
              <th>Доступ</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((u) => (
              <tr key={u.id}>
                <td>{u.id}</td>
                <td>
                  <strong>{u.username}</strong>
                </td>
                <td>{u.role}</td>
                <td>
                  <span
                    className={`badge ${u.enabled ? "confirmed" : "cancelled"}`}
                  >
                    {u.enabled ? "Активен" : "Отключён"}
                  </span>
                </td>
                <td>
                  <div className="inline-actions">
                    <button
                      className="text-button"
                      onClick={() => setModal({ item: u })}
                    >
                      Изменить
                    </button>
                    {u.id !== currentId && u.enabled && (
                      <button
                        className="text-button danger"
                        onClick={() => setModal({ item: u, disable: true })}
                      >
                        Отключить
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {data && <Pager data={data} setPage={setPage} />}{" "}
      {modal && (
        <Modal
          title={
            modal.disable
              ? "Отключить пользователя?"
              : modal.item
                ? "Изменить пользователя"
                : "Новый пользователь"
          }
          close={() => setModal(null)}
        >
          {modal.disable ? (
            <>
              <p>Вход будет запрещён. История бронирований сохранится.</p>
              <Alert>{error}</Alert>
              <button className="button full" onClick={disable}>
                Отключить
              </button>
            </>
          ) : (
            <UserForm
              api={api}
              item={modal.item}
              done={() => {
                setModal(null);
                load();
              }}
            />
          )}
        </Modal>
      )}
    </>
  );
}

function UserForm({ api, item, done }) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    const data = Object.fromEntries(new FormData(e.currentTarget));
    if (!data.password) delete data.password;
    if (item) data.enabled = data.enabled === "on";
    try {
      await api(`/users${item ? "/" + item.id : ""}`, {
        method: item ? "PATCH" : "POST",
        body: data,
      });
      done();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      <Field label="Логин">
        <input
          name="username"
          required
          pattern="[a-zA-Z0-9_.-]{3,50}"
          defaultValue={item?.username}
        />
      </Field>
      <Field label={item ? "Новый пароль (необязательно)" : "Пароль"}>
        <input
          type="password"
          name="password"
          required={!item}
          minLength={8}
          maxLength={72}
          autoComplete="new-password"
        />
      </Field>
      <Field label="Роль">
        <select name="role" defaultValue={item?.role || "USER"}>
          <option>USER</option>
          <option>ADMIN</option>
        </select>
      </Field>
      {item && (
        <label className="checkbox">
          <input type="checkbox" name="enabled" defaultChecked={item.enabled} />{" "}
          Аккаунт активен
        </label>
      )}
      <Alert>{error}</Alert>
      <button className="button full" disabled={busy}>
        {busy ? "Сохраняем…" : "Сохранить"}
      </button>
    </form>
  );
}

export default Users;
