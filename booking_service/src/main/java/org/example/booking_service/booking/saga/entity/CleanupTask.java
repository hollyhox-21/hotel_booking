package org.example.booking_service.booking.saga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "cleanup_tasks",
    indexes = @Index(name = "idx_cleanup_due", columnList = "done,next_attempt_at"))
public class CleanupTask {
  @Id private UUID id;
  private Long bookingId;
  private boolean compensation;
  private String traceId;
  private boolean done;
  private int attempts;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt = Instant.now();

  @Version private Long version;

  protected CleanupTask() {}

  public CleanupTask(UUID id, Long bookingId, boolean compensation) {
    this.id = id;
    this.bookingId = bookingId;
    this.compensation = compensation;
    this.traceId = org.slf4j.MDC.get("traceId");
    if (this.traceId == null) this.traceId = id.toString();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Long getBookingId() {
    return bookingId;
  }

  public void setBookingId(Long bookingId) {
    this.bookingId = bookingId;
  }

  public boolean isCompensation() {
    return compensation;
  }

  public void setCompensation(boolean compensation) {
    this.compensation = compensation;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public boolean isDone() {
    return done;
  }

  public void setDone(boolean done) {
    this.done = done;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public void setNextAttemptAt(Instant nextAttemptAt) {
    this.nextAttemptAt = nextAttemptAt;
  }

  public Long getVersion() {
    return version;
  }
}
