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

function Hotels({ api }) {
  const [hotels, setHotels] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [error, setError] = useState("");
  const [modal, setModal] = useState(null);
  const [filter, setFilter] = useState("");
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState("id");
  const [notice, setNotice] = useState("");
  async function load() {
    try {
      const [hs, rs] = await Promise.all([
        api("/hotels"),
        api(`/rooms/all?sort=${sort}${filter ? "&hotelId=" + filter : ""}`),
      ]);
      setHotels(hs);
      setRooms(rs);
    } catch (e) {
      setError(errorText(e));
    }
  }
  useEffect(() => {
    load();
  }, [filter, sort]);
  async function remove(type, item) {
    setError("");
    try {
      await api(`/${type}/${item.id}`, { method: "DELETE" });
      setModal(null);
      setNotice("Запись удалена");
      await load();
    } catch (e) {
      setError(errorText(e));
    }
  }
  return (
    <>
      <PageHeading
        eyebrow="АДМИНИСТРИРОВАНИЕ"
        title="Отели и номера"
        text="Управляйте отелями и операционной доступностью номеров."
      >
        <button className="button" onClick={() => setModal({ type: "hotel" })}>
          <Icon name="plus" size={18} /> Добавить отель
        </button>
      </PageHeading>
      <Alert>{error}</Alert>
      <Alert success>{notice}</Alert>
      <div className="toolbar">
        <input
          aria-label="Поиск отелей"
          placeholder="Поиск по названию или адресу"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      <div className="hotel-grid">
        {hotels
          .filter((h) =>
            (h.name + " " + h.address)
              .toLowerCase()
              .includes(search.toLowerCase()),
          )
          .map((h) => (
            <article className="panel hotel-panel" key={h.id}>
              <div className="hotel-icon">
                <Icon name="hotel" size={26} />
              </div>
              <div className="eyebrow">ОТЕЛЬ ID {h.id}</div>
              <h3>{h.name}</h3>
              <p className="muted">{h.address}</p>
              <div className="inline-actions">
                <button
                  className="text-button"
                  onClick={() => setModal({ type: "hotel", item: h })}
                >
                  Изменить
                </button>
                <button
                  className="text-button danger"
                  onClick={() => setModal({ type: "delete-hotel", item: h })}
                >
                  Удалить
                </button>
              </div>
            </article>
          ))}
      </div>
      <div className="section-title">
        <h2>
          Номерной фонд <span className="count">{rooms.length}</span>
        </h2>
        <button
          className="button secondary"
          onClick={() => setModal({ type: "room" })}
        >
          <Icon name="plus" size={18} /> Добавить номер
        </button>
      </div>
      <div className="toolbar">
        <select
          aria-label="Фильтр отеля"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          <option value="">Все отели</option>
          {hotels.map((h) => (
            <option value={h.id} key={h.id}>
              {h.name}
            </option>
          ))}
        </select>
        <select
          aria-label="Сортировка номеров"
          value={sort}
          onChange={(e) => setSort(e.target.value)}
        >
          <option value="id">По ID</option>
          <option value="number">По номеру</option>
          <option value="timesBooked">По числу бронирований</option>
        </select>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID / Номер</th>
              <th>Отель</th>
              <th>Доступность</th>
              <th>Бронирования</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {rooms.map((r) => (
              <tr key={r.id}>
                <td>
                  <strong>{r.number}</strong>
                  <small>ID {r.id}</small>
                </td>
                <td>{r.hotelName}</td>
                <td>
                  <span
                    className={`badge ${r.available ? "confirmed" : "cancelled"}`}
                  >
                    {r.available ? "В эксплуатации" : "Недоступен"}
                  </span>
                </td>
                <td>{r.timesBooked}</td>
                <td>
                  <div className="inline-actions">
                    <button
                      className="text-button"
                      onClick={() => setModal({ type: "room", item: r })}
                    >
                      Изменить
                    </button>
                    <button
                      className="text-button danger"
                      onClick={() => setModal({ type: "delete-room", item: r })}
                    >
                      Удалить
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!rooms.length && <p className="empty">Номера не найдены</p>}
      </div>
      {modal && (
        <Modal
          title={
            modal.type.startsWith("delete")
              ? "Удалить запись?"
              : `${modal.item ? "Изменить" : "Добавить"} ${modal.type === "hotel" ? "отель" : "номер"}`
          }
          close={() => {
            setModal(null);
            setError("");
          }}
        >
          {modal.type.startsWith("delete") ? (
            <>
              <p>
                История резервов сохраняется. Удаление записи с зависимостями
                будет отклонено.
              </p>
              <Alert>{error}</Alert>
              <button
                className="button full"
                onClick={() =>
                  remove(
                    modal.type === "delete-hotel" ? "hotels" : "rooms",
                    modal.item,
                  )
                }
              >
                Удалить
              </button>
            </>
          ) : (
            <EntityForm
              api={api}
              kind={modal.type}
              item={modal.item}
              hotels={hotels}
              done={() => {
                setModal(null);
                setNotice("Изменения сохранены");
                load();
              }}
            />
          )}
        </Modal>
      )}
    </>
  );
}

function EntityForm({ api, kind, item, hotels, done }) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    const data = Object.fromEntries(new FormData(e.currentTarget));
    if (kind === "room") {
      data.hotelId = Number(data.hotelId);
      data.available = data.available === "on";
    }
    try {
      await api(
        `/${kind === "hotel" ? "hotels" : "rooms"}${item ? "/" + item.id : ""}`,
        { method: item ? "PUT" : "POST", body: data },
      );
      done();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      {kind === "hotel" ? (
        <>
          <Field label="Название">
            <input
              name="name"
              required
              maxLength={150}
              defaultValue={item?.name}
            />
          </Field>
          <Field label="Адрес">
            <input
              name="address"
              required
              maxLength={300}
              defaultValue={item?.address}
            />
          </Field>
        </>
      ) : (
        <>
          <Field label="Отель">
            <select name="hotelId" required defaultValue={item?.hotelId || ""}>
              <option disabled value="">
                Выберите отель
              </option>
              {hotels.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Номер">
            <input
              name="number"
              required
              maxLength={30}
              defaultValue={item?.number}
            />
          </Field>
          <label className="checkbox">
            <input
              name="available"
              type="checkbox"
              defaultChecked={item ? item.available : true}
            />{" "}
            В эксплуатации
          </label>
        </>
      )}
      <Alert>{error}</Alert>
      <button className="button full" disabled={busy}>
        {busy ? "Сохраняем…" : "Сохранить"}
      </button>
    </form>
  );
}

export default Hotels;
