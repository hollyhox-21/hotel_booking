package org.example.booking_service.booking.saga;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.booking_service.booking.dto.request.BookingInput;
import org.example.booking_service.booking.dto.response.BookingView;
import org.example.booking_service.booking.entity.Booking;
import org.example.booking_service.booking.mapper.BookingMapper;
import org.example.booking_service.booking.repository.BookingRepository;
import org.example.booking_service.booking.saga.entity.BookingOperation;
import org.example.booking_service.booking.saga.entity.CleanupTask;
import org.example.booking_service.booking.saga.repository.CleanupRepository;
import org.example.booking_service.booking.saga.repository.OperationRepository;
import org.example.booking_service.booking.validation.BookingValidator;
import org.example.booking_service.shared.dto.response.PageView;
import org.example.booking_service.user.entity.UserAccount;
import org.example.booking_service.user.repository.UserRepository;
import org.example.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookingTransactions {
  private static final Logger log = LoggerFactory.getLogger(BookingTransactions.class);
  private final UserRepository users;
  private final BookingRepository bookings;
  private final OperationRepository operations;
  private final CleanupRepository cleanup;
  private final BookingMapper mapper;

  public BookingTransactions(
      UserRepository users,
      BookingRepository bookings,
      OperationRepository operations,
      CleanupRepository cleanup,
      BookingMapper mapper) {
    this.users = users;
    this.bookings = bookings;
    this.operations = operations;
    this.cleanup = cleanup;
    this.mapper = mapper;
  }

  public record Work(UUID id, Long bookingId, UUID previous, BookingView replay, boolean execute) {}

  private UserAccount user(Long id) {
    var u = users.locked(id).orElseThrow(() -> new ApiException(401, "Учётная запись недоступна"));
    if (!u.isEnabled()) throw new ApiException(401, "Учётная запись отключена");
    if (!u.getRole().equals("USER")) throw new ApiException(403, "Операция доступна только USER");
    return u;
  }

  private Booking owned(Long id, Long userId) {
    return bookings
        .findById(id)
        .filter(b -> b.getUser().getId().equals(userId))
        .orElseThrow(() -> new ApiException(404, "Бронирование не найдено"));
  }

  private BookingView view(Booking booking, UUID key) {
    return mapper.toView(booking, key, cleanup.existsByBookingIdAndDoneFalse(booking.getId()));
  }

  private BookingView snapshot(BookingOperation operation) {
    return mapper.toView(
        operation, cleanup.existsByBookingIdAndDoneFalse(operation.getBooking().getId()));
  }

  public Work begin(Long userId, Long bookingId, UUID key, BookingInput input, String kind) {
    UserAccount u = user(userId);
    String fingerprint =
        kind
            + "|"
            + bookingId
            + "|"
            + (input == null
                ? ""
                : input.autoSelect()
                    + "|"
                    + (input.autoSelect() ? null : input.roomId())
                    + "|"
                    + input.hotelId()
                    + "|"
                    + input.startDate()
                    + "|"
                    + input.endDate());
    var old = operations.findById(key);
    if (old.isPresent()) {
      BookingOperation o = old.get();
      if (!o.getUser().getId().equals(userId) || !o.getFingerprint().equals(fingerprint))
        throw new ApiException(409, "Idempotency-Key уже использован с другим запросом");
      if (o.getState().equals("PENDING")) throw new ApiException(409, "Запрос ещё выполняется");
      if (o.getState().equals("FAILED"))
        throw new ApiException(o.getFailureStatus(), o.getFailureMessage());
      return new Work(key, o.getBooking().getId(), o.getPreviousReservation(), snapshot(o), false);
    }
    if (input != null) BookingValidator.validate(input);
    Booking b;
    if (kind.equals("CREATE")) {
      b = new Booking();
      b.setUser(u);
      b.setStartDate(input.startDate());
      b.setEndDate(input.endDate());
      b.setRoomId(input.autoSelect() ? null : input.roomId());
      bookings.saveAndFlush(b);
    } else {
      b = owned(bookingId, userId);
      if (b.getActiveOperation() != null)
        throw new ApiException(409, "Другая операция с бронью ещё выполняется");
      if (!b.getStatus().equals("CONFIRMED"))
        throw new ApiException(409, "Операция доступна только для подтверждённой брони");
    }
    BookingOperation o = new BookingOperation();
    o.setId(key);
    o.setUser(u);
    o.setBooking(b);
    o.setKind(kind);
    o.setFingerprint(fingerprint);
    o.setTraceId(MDC.get("traceId"));
    if (o.getTraceId() == null) o.setTraceId(key.toString());
    o.setPreviousReservation(b.getReservationId());
    b.setActiveOperation(key);
    operations.saveAndFlush(o);
    log.info("Booking operation PENDING bookingId={} requestId={} kind={}", b.getId(), key, kind);
    return new Work(key, b.getId(), o.getPreviousReservation(), null, true);
  }

  public BookingView confirm(Long userId, UUID key, BookingInput input, Long roomId) {
    user(userId);
    BookingOperation o = operations.findById(key).orElseThrow();
    if (!o.getState().equals("PENDING"))
      throw new ApiException(409, "Операция уже завершена восстановлением");
    Booking b = org.hibernate.Hibernate.unproxy(o.getBooking(), Booking.class);
    b.setRoomId(roomId);
    b.setStartDate(input.startDate());
    b.setEndDate(input.endDate());
    b.setReservationId(key);
    b.setStatus("CONFIRMED");
    b.setActiveOperation(null);
    b.setUpdatedAt(Instant.now());
    if (o.getPreviousReservation() != null) enqueue(o.getPreviousReservation(), b.getId(), false);
    succeed(o, b);
    log.info("Booking CONFIRMED bookingId={} requestId={}", b.getId(), key);
    return snapshot(o);
  }

  public BookingView cancel(Long userId, UUID key) {
    user(userId);
    BookingOperation o = operations.findById(key).orElseThrow();
    if (!o.getState().equals("PENDING"))
      throw new ApiException(409, "Операция уже завершена восстановлением");
    Booking b = org.hibernate.Hibernate.unproxy(o.getBooking(), Booking.class);
    enqueue(b.getReservationId(), b.getId(), false);
    b.setStatus("CANCELLED");
    b.setActiveOperation(null);
    b.setUpdatedAt(Instant.now());
    succeed(o, b);
    log.info("Booking CANCELLED bookingId={} requestId={}", b.getId(), key);
    return snapshot(o);
  }

  private void succeed(BookingOperation o, Booking b) {
    o.setState("SUCCEEDED");
    o.setResultRoomId(b.getRoomId());
    o.setResultStartDate(b.getStartDate());
    o.setResultEndDate(b.getEndDate());
    o.setResultStatus(b.getStatus());
  }

  public void fail(Long userId, UUID key, int code, String message) {
    // Lock order is always user, then booking/operation, including recovery.
    users.locked(userId);
    BookingOperation o = operations.findById(key).orElseThrow();
    if (!o.getState().equals("PENDING")) return;
    o.setState("FAILED");
    o.setFailureStatus(code);
    o.setFailureMessage(message);
    Booking b = org.hibernate.Hibernate.unproxy(o.getBooking(), Booking.class);
    if (o.getKind().equals("CREATE")) b.setStatus("CANCELLED");
    b.setActiveOperation(null);
    b.setUpdatedAt(Instant.now());
    if (!o.getKind().equals("CANCEL")) enqueue(o.getId(), b.getId(), true);
    log.warn(
        "Booking operation FAILED bookingId={} requestId={} status={} compensationQueued={}",
        b.getId(),
        key,
        b.getStatus(),
        !o.getKind().equals("CANCEL"));
  }

  private void enqueue(UUID id, Long bookingId, boolean compensation) {
    if (id != null && !cleanup.existsById(id))
      cleanup.save(new CleanupTask(id, bookingId, compensation));
  }

  public BookingView result(Long userId, UUID key) {
    user(userId);
    return snapshot(operations.findById(key).orElseThrow());
  }

  public PageView<BookingView> list(Long userId, int page, int size) {
    user(userId);
    var result =
        bookings.findByUserId(
            userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    return new PageView<>(
        result.map(b -> view(b, b.getReservationId())).getContent(),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  public BookingView get(Long userId, Long id) {
    user(userId);
    return view(owned(id, userId), null);
  }

  public List<UUID> dueCleanup() {
    return cleanup
        .findTop50ByDoneFalseAndNextAttemptAtBeforeOrderByNextAttemptAt(Instant.now())
        .stream()
        .map(t -> t.getId())
        .toList();
  }

  public record Task(UUID id, Long bookingId, boolean compensation, boolean done, String traceId) {}

  public Task task(UUID id) {
    var t = cleanup.findById(id).orElseThrow();
    return new Task(id, t.getBookingId(), t.isCompensation(), t.isDone(), t.getTraceId());
  }

  public void cleanupResult(UUID id, boolean success) {
    var t = cleanup.locked(id).orElseThrow();
    if (t.isDone()) return;
    t.setAttempts(t.getAttempts() + 1);
    t.setDone(success);
    t.setNextAttemptAt(Instant.now().plusSeconds(Math.min(60, 1L << Math.min(t.getAttempts(), 6))));
  }

  public record Stale(UUID id, Long userId, String traceId) {}

  public List<Stale> stale(Instant cutoff) {
    return operations.findTop50ByStateAndCreatedAtBeforeOrderByCreatedAt("PENDING", cutoff).stream()
        .map(o -> new Stale(o.getId(), o.getUser().getId(), o.getTraceId()))
        .toList();
  }
}
