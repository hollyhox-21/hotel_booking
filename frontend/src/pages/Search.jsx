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

function Search({ api, session, login, booked }) {
  const [hotels, setHotels] = useState([]);
  const [rooms, setRooms] = useState(null);
  const [query, setQuery] = useState({
    startDate: date(1),
    endDate: date(3),
    hotelId: "",
  });
  const [searched, setSearched] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [confirm, setConfirm] = useState(null);
  useEffect(() => {
    if (session)
      api("/hotels")
        .then(setHotels)
        .catch((e) => setError(errorText(e)));
  }, [session?.token]);
  async function search(e) {
    e?.preventDefault();
    if (!session) {
      login();
      return;
    }
    setBusy(true);
    setError("");
    try {
      const q = { ...query };
      setRooms(
        await api(
          `/bookings/recommendations?${new URLSearchParams(Object.fromEntries(Object.entries(q).filter(([, v]) => v)))}`,
        ),
      );
      setSearched(q);
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <section className="hero">
        <div>
          <div className="eyebrow">ПРОСТРАНСТВО ДЛЯ ВАШИХ ПЛАНОВ</div>
          <h1>
            Найдите своё
            <br />
            <em>тихое место.</em>
          </h1>
          <p>
            Выберите даты — мы найдём свободный номер.
            <br />
            Остальное время посвятите себе.
          </p>
          <span className="hero-caption">
            <Icon name="check" size={16} /> Актуальная доступность по вашим
            датам
          </span>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="sun" />
          <div className="arch">
            <div className="arch-inner" />
            <div className="horizon" />
            <div className="hill one" />
            <div className="hill two" />
            <div className="line" />
          </div>
          <span className="art-mark">ТМ / МЕСТО ДЛЯ ОТДЫХА</span>
        </div>
      </section>
      <form className="search-panel" onSubmit={search}>
        <Field label="Куда отправимся">
          <select
            value={query.hotelId}
            onChange={(e) => setQuery({ ...query, hotelId: e.target.value })}
          >
            <option value="">Все отели</option>
            {hotels.map((h) => (
              <option key={h.id} value={h.id}>
                {h.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Заезд">
          <input
            aria-label="Дата заезда"
            type="date"
            required
            min={date()}
            value={query.startDate}
            onChange={(e) => setQuery({ ...query, startDate: e.target.value })}
          />
        </Field>
        <Field label="Выезд">
          <input
            aria-label="Дата выезда"
            type="date"
            required
            min={query.startDate}
            value={query.endDate}
            onChange={(e) => setQuery({ ...query, endDate: e.target.value })}
          />
        </Field>
        <button className="button" disabled={busy}>
          <Icon name="search" size={18} />
          {busy ? "Ищем…" : "Найти номер"}
        </button>
      </form>
      <Alert>{error}</Alert>
      {rooms !== null ? (
        <section>
          <div className="section-title">
            <div>
              <div className="eyebrow">ДОСТУПНО НА ВАШИ ДАТЫ</div>
              <h2>
                Место для новой поездки{" "}
                <span className="count">{rooms.length}</span>
              </h2>
            </div>
            {rooms.length > 0 && (
              <button
                className="button secondary"
                onClick={() => setConfirm({ autoSelect: true, ...searched })}
              >
                Подобрать автоматически <Icon name="arrow" size={18} />
              </button>
            )}
          </div>
          <p className="muted">
            Номера отсортированы по числу успешных бронирований. Доступность
            повторно проверяется при подтверждении.
          </p>
          {rooms.length === 0 ? (
            <Empty
              title="На эти даты свободных номеров нет"
              text="Попробуйте другой отель или измените даты поездки."
            />
          ) : (
            <div className="room-grid">
              {rooms.map((r, i) => (
                <article className="room-card" key={r.id}>
                  <div className={`room-visual tone-${i % 3}`}>
                    <Icon name="hotel" size={64} />
                    <span>{r.hotelName}</span>
                    {i === 0 && <b>Первый в подборе</b>}
                  </div>
                  <div className="room-info">
                    <span className="eyebrow">{r.hotelName}</span>
                    <h3>Номер {r.number}</h3>
                    <div className="room-details">
                      <span>
                        <span className="green-dot" /> Доступен
                      </span>
                      <span>{r.timesBooked} бронирований</span>
                    </div>
                    <button
                      className="button secondary full"
                      onClick={() =>
                        setConfirm({
                          ...searched,
                          roomId: r.id,
                          autoSelect: false,
                          roomNumber: r.number,
                        })
                      }
                    >
                      Выбрать номер <Icon name="arrow" size={17} />
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      ) : (
        <section className="intro-grid">
          <div>
            <span className="step-number">01</span>
            <h3>Выберите даты</h3>
            <p>Укажите заезд и выезд, чтобы увидеть свободные номера.</p>
          </div>
          <div>
            <span className="step-number">02</span>
            <h3>Найдите свой номер</h3>
            <p>Выбирайте сами или доверьте подбор системе.</p>
          </div>
          <div>
            <span className="step-number">03</span>
            <h3>Всё под рукой</h3>
            <p>Просматривайте, переносите и отменяйте свои бронирования.</p>
          </div>
        </section>
      )}
      {confirm && (
        <Modal
          title={
            confirm.autoSelect
              ? "Автоматический подбор"
              : `Бронируем номер ${confirm.roomNumber}`
          }
          close={() => setConfirm(null)}
        >
          <BookingForm
            api={api}
            initial={confirm}
            done={() => {
              setConfirm(null);
              booked();
            }}
          />
        </Modal>
      )}
    </>
  );
}

export default Search;
