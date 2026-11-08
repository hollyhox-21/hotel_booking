package org.example.hotel_management_service.reservation.repository;

import java.util.List;
import java.util.UUID;
import org.example.hotel_management_service.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
  public List<Reservation> findByActiveTrue();

  public boolean existsByRoomId(Long id);

  public boolean existsByBookingIdAndRoomIdAndCountedTrue(Long bookingId, Long roomId);
}
