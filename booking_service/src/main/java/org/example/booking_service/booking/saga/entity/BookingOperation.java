package org.example.booking_service.booking.saga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.example.booking_service.booking.entity.Booking;
import org.example.booking_service.user.entity.UserAccount;

@Entity
@Table(
    name = "booking_operations",
    indexes = @Index(name = "idx_operation_recovery", columnList = "state,created_at"))
public class BookingOperation {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id")
  private Booking booking;

  @Column(nullable = false)
  private String kind;

  @Column(nullable = false, length = 1000)
  private String fingerprint;

  private String traceId;

  @Column(nullable = false)
  private String state = "PENDING";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  private UUID previousReservation;
  private Long resultRoomId;
  private LocalDate resultStartDate;
  private LocalDate resultEndDate;
  private String resultStatus;
  private int failureStatus;
  private String failureMessage;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UserAccount getUser() {
    return user;
  }

  public void setUser(UserAccount user) {
    this.user = user;
  }

  public Booking getBooking() {
    return booking;
  }

  public void setBooking(Booking booking) {
    this.booking = booking;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public void setFingerprint(String fingerprint) {
    this.fingerprint = fingerprint;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public UUID getPreviousReservation() {
    return previousReservation;
  }

  public void setPreviousReservation(UUID previousReservation) {
    this.previousReservation = previousReservation;
  }

  public Long getResultRoomId() {
    return resultRoomId;
  }

  public void setResultRoomId(Long resultRoomId) {
    this.resultRoomId = resultRoomId;
  }

  public LocalDate getResultStartDate() {
    return resultStartDate;
  }

  public void setResultStartDate(LocalDate resultStartDate) {
    this.resultStartDate = resultStartDate;
  }

  public LocalDate getResultEndDate() {
    return resultEndDate;
  }

  public void setResultEndDate(LocalDate resultEndDate) {
    this.resultEndDate = resultEndDate;
  }

  public String getResultStatus() {
    return resultStatus;
  }

  public void setResultStatus(String resultStatus) {
    this.resultStatus = resultStatus;
  }

  public int getFailureStatus() {
    return failureStatus;
  }

  public void setFailureStatus(int failureStatus) {
    this.failureStatus = failureStatus;
  }

  public String getFailureMessage() {
    return failureMessage;
  }

  public void setFailureMessage(String failureMessage) {
    this.failureMessage = failureMessage;
  }

  public BookingOperation() {}
}
