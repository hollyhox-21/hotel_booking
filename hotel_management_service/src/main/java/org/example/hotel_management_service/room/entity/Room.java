package org.example.hotel_management_service.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.example.hotel_management_service.hotel.entity.Hotel;

@Entity
@Table(
    name = "rooms",
    uniqueConstraints = @UniqueConstraint(columnNames = {"hotel_id", "number"}),
    indexes = @Index(name = "idx_room_load", columnList = "times_booked,id"))
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "hotel_id")
  private Hotel hotel;

  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private boolean available = true;

  @Column(name = "times_booked", nullable = false)
  private long timesBooked;

  @Version private Long version;

  protected Room() {}

  public Room(Hotel hotel, String number) {
    this.hotel = hotel;
    this.number = number;
  }

  public Long getId() {
    return id;
  }

  public void adjustCount(long delta) {
    timesBooked += delta;
  }

  public Hotel getHotel() {
    return hotel;
  }

  public void setHotel(Hotel hotel) {
    this.hotel = hotel;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public long getTimesBooked() {
    return timesBooked;
  }

  public void setTimesBooked(long timesBooked) {
    this.timesBooked = timesBooked;
  }

  public Long getVersion() {
    return version;
  }
}
