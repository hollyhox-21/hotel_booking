package org.example.hotel_management_service.reservation.service;

import java.util.UUID;
import org.example.hotel_management_service.reservation.dto.request.ReserveInput;
import org.example.hotel_management_service.reservation.dto.response.ReservationView;

public interface ReservationService {
  ReservationView reserve(ReserveInput input);

  void release(UUID key, boolean compensation, Long expectedRoomId);
}
