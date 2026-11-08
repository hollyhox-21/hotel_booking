export class ApiError extends Error {
  constructor(message, status, traceId) {
    super(message);
    this.status = status;
    this.traceId = traceId;
  }
}
export async function request(
  path,
  { token, method = "GET", body, key, signal } = {},
) {
  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      signal,
      headers: {
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(key ? { "Idempotency-Key": key } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
  } catch (e) {
    if (e.name === "AbortError") throw e;
    throw new ApiError(
      "Нет соединения. Проверьте запуск приложения и повторите запрос.",
      0,
    );
  }
  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    throw new ApiError("Сервис вернул некорректный ответ", response.status);
  }
  if (!response.ok)
    throw new ApiError(
      data?.message || `Ошибка запроса (${response.status})`,
      response.status,
      data?.traceId || response.headers.get("X-Trace-Id"),
    );
  return data;
}
// Retain the key after a lost response so a deliberate retry cannot create a duplicate.
export function mutationKey(storage, signature) {
  const storageKey = `booking-mutation:${signature}`;
  const item = storage.getItem(storageKey);
  if (item) {
    try {
      const previous = JSON.parse(item);
      if (previous.signature === signature) return previous.key;
    } catch {}
  }
  const key = crypto.randomUUID();
  storage.setItem(storageKey, JSON.stringify({ signature, key }));
  return key;
}
