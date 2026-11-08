export const date = (offset = 0) => {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
export const fmt = (value) =>
  new Date(`${value}T12:00:00`).toLocaleDateString("ru", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
export const nights = (start, end) =>
  Math.round((new Date(end) - new Date(start)) / 86400000);
export const statusName = {
  CONFIRMED: "Подтверждено",
  CANCELLED: "Отменено",
  PENDING: "Ожидает подтверждения",
};
export const errorText = (e) =>
  `${e.message}${e.traceId ? ` · Код обращения: ${e.traceId}` : ""}`;
