# Структура исходников

Код организован по функциональности (package by feature), а внутри каждой функциональности — по слоям. Контроллеры, бизнес-сервисы, сущности и REST DTO размещены в отдельных Java-файлах; служебные записи состояния саги остаются вложенными records. Контроллеры зависят от интерфейсов бизнес-сервисов; JPA-сущности не являются DTO REST API.

## Booking Service

```text
org.example.booking_service
├── BookingServiceApplication
├── auth
│   ├── controller/AuthController
│   ├── dto/request/Credentials
│   ├── dto/response/AuthView
│   └── service/{AuthService, impl/AuthServiceImpl}
├── user
│   ├── controller/UserController
│   ├── dto/{request, response}
│   ├── entity/UserAccount
│   ├── repository/UserRepository
│   ├── mapper/UserMapper
│   └── service/{UserService, impl/UserServiceImpl}
├── booking
│   ├── controller/BookingController
│   ├── dto/{request, response}
│   ├── entity/Booking
│   ├── repository/BookingRepository
│   ├── mapper/BookingMapper
│   ├── validation/BookingValidator
│   ├── service/{BookingService, impl/BookingServiceImpl}
│   └── saga
│       ├── BookingSagaOrchestrator
│       ├── BookingTransactions
│       ├── CompensationWorker
│       ├── entity/{BookingOperation, CleanupTask}
│       └── repository/{OperationRepository, CleanupRepository}
├── integration/hotel
│   ├── HotelPort
│   ├── HttpHotelClient
│   └── dto/{request, response}
├── shared/dto/response/PageView
└── config/BookingSeed
```

`AuthService` регистрирует пользователей и выдаёт JWT. `UserService` управляет учётными записями. `BookingService` предоставляет операции приложения контроллеру и делегирует изменение бронирований оркестратору.

`BookingSagaOrchestrator` выполняет удалённые вызовы между отдельными локальными транзакциями `BookingTransactions`. `CompensationWorker` обрабатывает очередь освобождения резервов и восстанавливает зависшие операции. Worker и транзакционный компонент не требуют отдельных интерфейсов: это внутренние механизмы саги. `HotelPort` изолирует бизнес-код от HTTP и discovery.

## Hotel Management Service

```text
org.example.hotel_management_service
├── HotelManagementServiceApplication
├── hotel
│   ├── controller/HotelController
│   ├── dto/{request, response}
│   ├── entity/Hotel
│   ├── repository/HotelRepository
│   ├── mapper/HotelMapper
│   └── service/{HotelService, impl/HotelServiceImpl}
├── room
│   ├── controller/RoomController
│   ├── dto/{request, response}
│   ├── entity/Room
│   ├── repository/RoomRepository
│   ├── mapper/RoomMapper
│   └── service
│       ├── RoomService
│       ├── impl/RoomServiceImpl
│       ├── RoomCatalog
│       └── RoomAvailability
├── reservation
│   ├── controller/ReservationController
│   ├── dto/{request, response}
│   ├── entity/{Reservation, AllocationLock}
│   ├── repository/{ReservationRepository, AllocationLockRepository}
│   ├── mapper/ReservationMapper
│   └── service/{ReservationService, impl/ReservationServiceImpl}
├── statistics
│   ├── controller/StatisticsController
│   ├── dto/response/RoomStats
│   └── service/{StatisticsService, impl/StatisticsServiceImpl}
├── shared/validation/DateRangeValidator
└── config/HotelSeed
```

CRUD отелей, работа с номерами, атомарное резервирование и аналитика имеют самостоятельные интерфейсы сервисов. `RoomCatalog` загружает сущности для внутренних операций. `RoomAvailability` содержит общий предикат пересечения дат. Мьютекс БД используется теми же операциями, что и до разделения классов; границы локальных транзакций сохранены.

## Остальные компоненты

В Gateway конфигурация размещена в `config`, единая обработка ошибок — в `exception`, фильтр корреляции — в `tracing`. Eureka остаётся минимальным приложением регистрации сервисов.

Общая библиотека `common` содержит JWT/Spring Security, ErrorDTO, обработчик исключений, фильтр traceId и OpenAPI. Бизнес-сущности и DTO отдельных микросервисов в неё не переносятся.

## Правила расширения

* `controller` — HTTP-маршруты, привязка и проверка входных данных, роли, вызов интерфейса сервиса.
* `dto/request` и `dto/response` — отдельные records для входа и выхода; без ссылок на JPA-сущности.
* `entity` — закрытые поля JPA-сущностей и методы доступа. Не возвращать сущности из контроллеров.
* `repository` — интерфейсы Spring Data и запросы к БД.
* `service` — контракты бизнес-операций; `service/impl` — Spring-реализации.
* `mapper` — преобразования сущностей в DTO; без запросов к БД и удалённых вызовов.
* `saga` / `integration` — согласованность и внешнее взаимодействие.

Тесты используют те же функциональные пакеты. Тесты устойчивости HTTP-клиента расположены в `integration.hotel`; проверки конкурентного резервирования — в `reservation`. Сквозные тесты остаются в независимом модуле `integration_tests` и обращаются только к HTTP API.
