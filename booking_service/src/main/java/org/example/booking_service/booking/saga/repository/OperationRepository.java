package org.example.booking_service.booking.saga.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.booking_service.booking.saga.entity.BookingOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationRepository extends JpaRepository<BookingOperation, UUID> {
  public List<BookingOperation> findTop50ByStateAndCreatedAtBeforeOrderByCreatedAt(
      String state, Instant cutoff);
}
