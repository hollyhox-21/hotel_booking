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
import BookingForm from "../components/BookingForm.jsx";
import { mutationKey } from "../api.js";

function Bookings({ api }) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [edit, setEdit] = useState(null);
  const [cancel, setCancel] = useState(null);
  const [busy, setBusy] = useState(false);
  async function load() {
    try {
      setData(await api(`/bookings?page=${page}&size=8`));
      setError("");
    } catch (e) {
      setError(errorText(e));
    }
  }
  useEffect(() => {
    load();
  }, [page]);
  async function remove() {
    setBusy(true);
    const key = mutationKey(sessionStorage, `cancel:${cancel.id}`);
    try {
      await api(`/bookings/${cancel.id}`, { method: "DELETE", key });
      sessionStorage.removeItem(`booking-mutation:cancel:${cancel.id}`);
      setCancel(null);
      await load();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <PageHeading
        eyebrow="ВАШИ ПЛАНЫ"
        title="Мои бронирования"
        text="Все поездки, даты и подтверждения — в одном месте."
      />
      <Alert>{error}</Alert>
      {!data ? (
        <p className="muted">Загружаем бронирования…</p>
      ) : !data.content.length ? (
        <Empty
          title="Ваша следующая поездка ещё впереди"
          text="Найдите номер во вкладке поиска и создайте первое бронирование."
        />
      ) : (
        <>
          <div className="booking-list">
            {data.content.map((b) => (
              <article className="booking-card" key={b.id}>
                <div className="booking-symbol">
                  <Icon name="hotel" size={30} />
                </div>
                <div className="booking-main">
                  <div className="eyebrow">БРОНИРОВАНИЕ № {b.id}</div>
                  <h3>Номер ID {b.roomId || "—"}</h3>
                  <p>
                    {fmt(b.startDate)} — {fmt(b.endDate)}{" "}
                    <span>· {nights(b.startDate, b.endDate)} ночей</span>
                  </p>
                  {b.cleanupPending && (
                    <small className="muted">
                      Синхронизация с отелем выполняется…
                    </small>
                  )}
                </div>
                <div className="booking-actions">
                  <span className={`badge ${b.status.toLowerCase()}`}>
                    {statusName[b.status]}
                  </span>
                  {b.status === "CONFIRMED" && (
                    <div>
                      <button
                        className="text-button"
                        onClick={() => setEdit(b)}
                      >
                        Изменить
                      </button>
                      <button
                        className="text-button danger"
                        onClick={() => setCancel(b)}
                      >
                        Отменить
                      </button>
                    </div>
                  )}
                </div>
              </article>
            ))}
          </div>
          <Pager data={data} setPage={setPage} />
        </>
      )}
      {edit && (
        <Modal
          title={`Изменить бронирование № ${edit.id}`}
          close={() => setEdit(null)}
        >
          <BookingForm
            api={api}
            booking={edit}
            initial={edit}
            done={() => {
              setEdit(null);
              load();
            }}
          />
        </Modal>
      )}
      {cancel && (
        <Modal title="Отменить бронирование?" close={() => setCancel(null)}>
          <p>
            Бронирование № {cancel.id} будет отменено, а номер освобождён.
            История поездки сохранится.
          </p>
          <Alert>{error}</Alert>
          <button className="button full" disabled={busy} onClick={remove}>
            {busy ? "Отменяем…" : "Да, отменить бронирование"}
          </button>
        </Modal>
      )}
    </>
  );
}

export default Bookings;
