package org.example.hotel_management_service.reservation.mapper;

import org.example.hotel_management_service.reservation.dto.response.ReservationView;
import org.example.hotel_management_service.reservation.entity.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

  public ReservationView toView(Reservation r) {
    return new ReservationView(
        r.getId(),
        r.getRoom() == null ? null : r.getRoom().getId(),
        r.getBookingId(),
        r.getStartDate(),
        r.getEndDate(),
        r.isActive());
  }
}
