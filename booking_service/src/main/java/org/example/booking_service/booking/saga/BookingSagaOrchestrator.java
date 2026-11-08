package org.example.booking_service.booking.saga;

import java.util.UUID;
import org.example.booking_service.booking.dto.request.BookingInput;
import org.example.booking_service.booking.dto.response.BookingView;
import org.example.booking_service.integration.hotel.HotelPort;
import org.example.booking_service.integration.hotel.dto.request.ReserveInput;
import org.example.common.ApiException;
import org.springframework.stereotype.Component;

@Component
public class BookingSagaOrchestrator {
  private final BookingTransactions tx;
  private final HotelPort hotel;
  private final CompensationWorker worker;

  public BookingSagaOrchestrator(
      BookingTransactions tx, HotelPort hotel, CompensationWorker worker) {
    this.tx = tx;
    this.hotel = hotel;
    this.worker = worker;
  }

  public BookingView save(Long userId, Long bookingId, UUID key, BookingInput input) {
    var work = tx.begin(userId, bookingId, key, input, bookingId == null ? "CREATE" : "UPDATE");
    if (!work.execute()) return work.replay();
    try {
      // Fetch the ordered proposal; Hotel rechecks and allocates under its database lock.
      if (input.autoSelect()) hotel.recommend(input.startDate(), input.endDate(), input.hotelId());
      var result =
          hotel.reserve(
              new ReserveInput(
                  key,
                  work.bookingId(),
                  input.autoSelect() ? null : input.roomId(),
                  input.hotelId(),
                  input.autoSelect(),
                  input.startDate(),
                  input.endDate(),
                  work.previous()));
      if (result == null || !result.active() || result.roomId() == null)
        throw new ApiException(503, "Hotel Service вернул некорректное подтверждение");
      tx.confirm(userId, key, input, result.roomId());

    } catch (Exception e) {
      int status = e instanceof ApiException a ? a.status() : 503;
      String message =
          e instanceof ApiException ? e.getMessage() : "Не удалось завершить бронирование";
      tx.fail(userId, key, status, message);
      worker.cleanupOne(key);
      throw new ApiException(status, message);
    }
    if (work.previous() != null) worker.cleanupOne(work.previous());
    return tx.result(userId, key);
  }

  public BookingView cancel(Long userId, Long id, UUID key) {
    var work = tx.begin(userId, id, key, null, "CANCEL");
    if (!work.execute()) return work.replay();
    tx.cancel(userId, key);
    if (work.previous() != null) worker.cleanupOne(work.previous());
    return tx.result(userId, key);
  }
}
