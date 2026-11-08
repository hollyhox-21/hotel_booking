# API

Внешняя точка входа: `http://localhost:8080`. JWT: `Authorization: Bearer <token>`. Все даты `YYYY-MM-DD`. Для создания/переноса/отмены бронирования обязателен `Idempotency-Key: <UUID>`. POST создания возвращают 201, удаление справочников/пользователя — 204, отмена брони — 200 с результатом. PUT — полная замена полей, PATCH пользователя — частичная.

| Метод и путь | Доступ | Назначение |
|---|---|---|
| POST /api/auth/register | публичный | `{username,password}` → token, tokenType, expiresIn=3600, user; всегда USER |
| POST /api/auth/login | публичный | `{username,password}` → JWT |
| GET /api/auth/me | USER/ADMIN | текущая учётная запись |
| GET /api/users?page=0&size=20 | ADMIN | пользователи, максимум size=100 |
| GET /api/users/{id} | ADMIN | пользователь без пароля |
| POST /api/users | ADMIN | `{username,password,role}` |
| PATCH /api/users/{id} | ADMIN | необязательные username, password, role, enabled |
| DELETE /api/users/{id} | ADMIN | отключить с сохранением истории |
| POST /api/bookings | USER | создать, Idempotency-Key |
| GET /api/bookings?page=0&size=10 | USER | только свои, новые первыми |
| GET /api/bookings/{id} | USER | только своё |
| PUT /api/bookings/{id} | USER | изменить даты/номер, Idempotency-Key |
| DELETE /api/bookings/{id} | USER | отменить, Idempotency-Key |
| GET /api/bookings/recommendations?startDate=…&endDate=…&hotelId=… | USER | предложения Hotel через Booking |
| GET /api/hotels?query=…&sort=id | USER/ADMIN | поиск по названию/адресу; sort=id/name |
| GET /api/hotels/{id} | USER/ADMIN | отель |
| POST /api/hotels | ADMIN | `{name,address}` |
| PUT /api/hotels/{id} | ADMIN | `{name,address}` |
| DELETE /api/hotels/{id} | ADMIN | удалить пустой отель |
| GET /api/rooms?startDate=…&endDate=…&hotelId=…&sort=id | USER/ADMIN | свободные, sort=id/number/timesBooked |
| GET /api/rooms/recommend?startDate=…&endDate=…&hotelId=… | USER | свободные, times_booked ASC, id ASC |
| GET /api/rooms/all?hotelId=…&sort=id | ADMIN | все, включая ремонт |
| GET /api/rooms/{id} | USER/ADMIN | номер |
| POST /api/rooms | ADMIN | `{hotelId,number,available}` |
| PUT /api/rooms/{id} | ADMIN | `{hotelId,number,available}` |
| DELETE /api/rooms/{id} | ADMIN | удалить без истории резервов |
| GET /api/hotels/statistics?startDate=…&endDate=…&hotelId=… | ADMIN | по номерам: timesBooked, reservedNights, totalNights, occupancyPercent |

`hotelId` в фильтрах необязателен. Страница: `{content,page,size,totalElements,totalPages}`. Аналитика считает пересечение активных резервов с периодом; проценты — занятые ночи / календарные ночи, округление до сотых. Период статистики допускает прошлые даты.

Тело создания/переноса:

```json
{"roomId":1,"hotelId":null,"autoSelect":false,"startDate":"2027-02-10","endDate":"2027-02-13"}
```

При `autoSelect=true` roomId игнорируется, hotelId ограничивает подбор отелем. В ручном режиме нужен roomId. Ответ: `{id,userId,roomId,startDate,endDate,status,createdAt,requestId,cleanupPending}`. `cleanupPending=true` означает отложенное освобождение в Hotel; новая бронь уже отменена/изменена локально. Одинаковый Idempotency-Key воспроизводит снимок результата исходной операции, даже если бронь впоследствии была изменена.

## Внутренний протокол

Только прямой доступ к Hotel с JWT `roles=[INTERNAL]`, Gateway закрывает эти пути:

* POST `/api/internal/reservations`: `{requestId,bookingId,roomId,hotelId,autoSelect,startDate,endDate,replaces}`. `replaces` — ключ старого резерва при переносе; разрешено пересечение только с ним и только для той же брони.
* POST `/api/internal/reservations/{requestId}/release?compensation=true|false`. Допускает освобождение ещё неизвестного резерва; оставляет метку.
* POST `/api/rooms/{id}/confirm-availability`: тот же запрос для конкретного roomId.
* POST `/api/rooms/{id}/release`: `{requestId,compensation}` — проверяется соответствие roomId.

Ошибки: 400 валидация/даты/параметры; 401 нет/просрочен/неверен JWT или неверный логин; 403 роль; 404 нет ресурса или чужая бронь; 409 занятость/конфликт/зависимости/ключ; 503 недоступность Hotel. Единый JSON ErrorDTO и заголовок X-Trace-Id.
