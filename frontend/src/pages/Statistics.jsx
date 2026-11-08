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

function Statistics({ api }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [range, setRange] = useState({ startDate: date(), endDate: date(30) });
  async function load(e) {
    e?.preventDefault();
    try {
      setData(await api(`/hotels/statistics?${new URLSearchParams(range)}`));
      setError("");
    } catch (e) {
      setError(errorText(e));
    }
  }
  useEffect(() => {
    load();
  }, []);
  const total = data?.reduce((s, r) => s + r.reservedNights, 0) || 0;
  const capacity = data?.reduce((s, r) => s + r.totalNights, 0) || 0;
  return (
    <>
      <PageHeading
        eyebrow="АНАЛИТИКА"
        title="Загрузка номеров"
        text="Занятые ночи за период и историческое число бронирований."
      />
      <form className="panel stats-filter" onSubmit={load}>
        <Field label="Начало периода">
          <input
            type="date"
            required
            value={range.startDate}
            onChange={(e) => setRange({ ...range, startDate: e.target.value })}
          />
        </Field>
        <Field label="Конец периода">
          <input
            type="date"
            required
            min={range.startDate}
            value={range.endDate}
            onChange={(e) => setRange({ ...range, endDate: e.target.value })}
          />
        </Field>
        <button className="button">Показать</button>
      </form>
      <Alert>{error}</Alert>
      {data && (
        <>
          <div className="metrics">
            <div className="panel">
              <span className="eyebrow">НОМЕРОВ</span>
              <strong>{data.length}</strong>
            </div>
            <div className="panel">
              <span className="eyebrow">ЗАНЯТЫХ НОЧЕЙ</span>
              <strong>{total}</strong>
            </div>
            <div className="panel">
              <span className="eyebrow">ОБЩАЯ ЗАГРУЗКА</span>
              <strong>
                {capacity ? Math.round((total / capacity) * 100) : 0}
                <small>%</small>
              </strong>
            </div>
          </div>
          <div className="panel">
            <h2>Загрузка по номерам</h2>
            <p className="muted">
              Доля занятых ночей в календарном периоде. Недоступные для
              эксплуатации номера также входят в номерной фонд.
            </p>
            {data.map((r) => (
              <div className="stat-row" key={r.roomId}>
                <div>
                  <strong>Номер {r.number}</strong>
                  <small>{r.hotelName}</small>
                </div>
                <div
                  className="bar"
                  role="meter"
                  aria-label={`Загрузка номера ${r.number}`}
                  aria-valuenow={r.occupancyPercent}
                  aria-valuemin={0}
                  aria-valuemax={100}
                >
                  <span
                    style={{ width: `${Math.min(100, r.occupancyPercent)}%` }}
                  />
                </div>
                <strong>{r.occupancyPercent}%</strong>
                <small>{r.timesBooked} броней за всё время</small>
              </div>
            ))}
          </div>
        </>
      )}
    </>
  );
}

export default Statistics;
