package org.example.booking_service.booking.repository;

import org.example.booking_service.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
  public Page<Booking> findByUserId(Long userId, Pageable page);
}
