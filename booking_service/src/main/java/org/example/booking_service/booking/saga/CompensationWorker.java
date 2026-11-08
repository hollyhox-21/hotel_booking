package org.example.booking_service.booking.saga;

import java.time.Instant;
import java.util.UUID;
import org.example.booking_service.integration.hotel.HotelPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CompensationWorker {
  private static final Logger log = LoggerFactory.getLogger(CompensationWorker.class);
  private final BookingTransactions tx;
  private final HotelPort hotel;
  private final long pendingTimeout;

  public CompensationWorker(
      BookingTransactions tx,
      HotelPort hotel,
      @Value("${app.pending-timeout-seconds}") long pendingTimeout) {
    this.tx = tx;
    this.hotel = hotel;
    this.pendingTimeout = pendingTimeout;
  }

  @Scheduled(fixedDelayString = "${app.recovery-delay-ms}")
  public void recover() {
    for (var stale : tx.stale(Instant.now().minusSeconds(pendingTimeout))) {
      try {
        MDC.put("traceId", stale.traceId());
        tx.fail(stale.userId(), stale.id(), 503, "Операция прервана; выполнена компенсация");
      } catch (Exception e) {
        log.error("Recovery failure requestId={}", stale.id(), e);
      } finally {
        MDC.remove("traceId");
      }
    }
    runCleanup();
  }

  public void runCleanup() {
    for (UUID id : tx.dueCleanup()) cleanupOne(id);
  }

  public void cleanupOne(UUID id) {
    String previous = MDC.get("traceId");
    try {
      var task = tx.task(id);
      if (task.done()) return;
      MDC.put("traceId", task.traceId());
      boolean released = false;
      try {
        hotel.release(id, task.compensation());
        released = true;
      } catch (Exception e) {
        log.warn(
            "Cleanup retry scheduled bookingId={} requestId={} reason={}",
            task.bookingId(),
            id,
            e.getMessage());
      }
      tx.cleanupResult(id, released);
      if (released)
        log.atInfo()
            .addKeyValue("bookingId", task.bookingId())
            .addKeyValue("requestId", id)
            .addKeyValue("compensation", task.compensation())
            .log("Cleanup succeeded");
    } catch (Exception e) {
      log.error("Cleanup task processing failed requestId={}", id, e);
    } finally {
      if (previous == null) MDC.remove("traceId");
      else MDC.put("traceId", previous);
    }
  }
}
