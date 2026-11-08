import React, { useEffect, useState } from "react";
import { Icon, Alert, Field } from "./UI.jsx";
import { date, fmt, nights, errorText } from "../utils.js";
import { mutationKey } from "../api.js";

function BookingForm({ api, initial, booking, done }) {
  const [form, setForm] = useState({
    startDate: initial.startDate,
    endDate: initial.endDate,
    autoSelect: !!initial.autoSelect,
    roomId: initial.roomId || "",
    hotelId: initial.hotelId || "",
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [hotels, setHotels] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [originalRoom, setOriginalRoom] = useState(null);
  useEffect(() => {
    const controller = new AbortController();
    api("/hotels", { signal: controller.signal })
      .then(setHotels)
      .catch((e) => {
        if (e.name !== "AbortError") setError(errorText(e));
      });
    if (booking?.roomId)
      api(`/rooms/${booking.roomId}`, { signal: controller.signal })
        .then(setOriginalRoom)
        .catch(() => {});
    return () => controller.abort();
  }, []);
  useEffect(() => {
    if (
      form.autoSelect ||
      !form.startDate ||
      !form.endDate ||
      form.endDate <= form.startDate
    )
      return;
    const controller = new AbortController();
    api(
      `/rooms?startDate=${form.startDate}&endDate=${form.endDate}${form.hotelId ? "&hotelId=" + form.hotelId : ""}`,
      { signal: controller.signal },
    )
      .then(setRooms)
      .catch((e) => {
        if (e.name !== "AbortError") setError(errorText(e));
      });
    return () => controller.abort();
  }, [form.startDate, form.endDate, form.hotelId, form.autoSelect]);
  const choices = [
    ...(originalRoom &&
    (!form.hotelId || String(originalRoom.hotelId) === String(form.hotelId)) &&
    !rooms.some((r) => r.id === originalRoom.id)
      ? [originalRoom]
      : []),
    ...rooms,
  ];
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    const body = {
      ...form,
      roomId: form.roomId ? Number(form.roomId) : null,
      hotelId: form.hotelId ? Number(form.hotelId) : null,
    };
    const path = booking ? `/bookings/${booking.id}` : "/bookings";
    const signature = JSON.stringify([path, body]);
    const key = mutationKey(sessionStorage, signature);
    try {
      await api(path, { method: booking ? "PUT" : "POST", body, key });
      sessionStorage.removeItem(`booking-mutation:${signature}`);
      done();
    } catch (e) {
      if (
        e.status &&
        !(e.status === 409 && e.message.includes("ещё выполняется"))
      )
        sessionStorage.removeItem(`booking-mutation:${signature}`);
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      <p className="muted">
        {booking
          ? "До подтверждения новых дат ваша текущая бронь сохраняется."
          : "Номер резервируется после подтверждения доступности."}
      </p>
      <div className="form-row">
        <Field label="Заезд">
          <input
            type="date"
            min={date()}
            required
            value={form.startDate}
            onChange={(e) => setForm({ ...form, startDate: e.target.value })}
          />
        </Field>
        <Field label="Выезд">
          <input
            type="date"
            min={form.startDate}
            required
            value={form.endDate}
            onChange={(e) => setForm({ ...form, endDate: e.target.value })}
          />
        </Field>
      </div>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={form.autoSelect}
          onChange={(e) => setForm({ ...form, autoSelect: e.target.checked })}
        />{" "}
        Подобрать свободный номер автоматически
      </label>
      <Field label="Отель">
        <select
          value={form.hotelId}
          onChange={(e) =>
            setForm({ ...form, hotelId: e.target.value, roomId: "" })
          }
        >
          <option value="">Все отели</option>
          {hotels.map((h) => (
            <option key={h.id} value={h.id}>
              {h.name}
            </option>
          ))}
        </select>
      </Field>
      {!form.autoSelect && (
        <Field label="Номер">
          <select
            required
            value={form.roomId}
            onChange={(e) => setForm({ ...form, roomId: e.target.value })}
          >
            <option value="" disabled>
              Выберите номер
            </option>
            {choices.map((r) => (
              <option key={r.id} value={r.id}>
                {r.hotelName} · номер {r.number}
                {booking?.roomId === r.id ? " (текущий)" : ""}
              </option>
            ))}
          </select>
        </Field>
      )}
      <div className="booking-summary">
        <Icon name="calendar" />
        <span>
          {fmt(form.startDate)} — {fmt(form.endDate)}
          <small>
            {nights(form.startDate, form.endDate) > 0
              ? `${nights(form.startDate, form.endDate)} ночей`
              : "Выберите корректные даты"}
          </small>
        </span>
      </div>
      <Alert>{error}</Alert>
      <button className="button full" disabled={busy}>
        {busy
          ? "Проверяем доступность…"
          : booking
            ? "Сохранить изменения"
            : "Подтвердить бронирование"}
        <Icon name="arrow" size={18} />
      </button>
    </form>
  );
}

export default BookingForm;
