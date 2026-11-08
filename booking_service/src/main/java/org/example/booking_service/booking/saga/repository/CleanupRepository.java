package org.example.booking_service.booking.saga.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.booking_service.booking.saga.entity.CleanupTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupRepository extends JpaRepository<CleanupTask, UUID> {
  public List<CleanupTask> findTop50ByDoneFalseAndNextAttemptAtBeforeOrderByNextAttemptAt(
      Instant now);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select t from CleanupTask t where t.id=:id")
  public Optional<CleanupTask> locked(UUID id);

  public boolean existsByBookingIdAndDoneFalse(Long bookingId);
}
