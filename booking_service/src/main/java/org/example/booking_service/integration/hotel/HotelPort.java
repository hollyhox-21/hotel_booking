package org.example.booking_service.integration.hotel;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.booking_service.integration.hotel.dto.request.ReserveInput;
import org.example.booking_service.integration.hotel.dto.response.ReservationView;
import org.example.booking_service.integration.hotel.dto.response.RoomView;

public interface HotelPort {
  public ReservationView reserve(ReserveInput input);

  public void release(UUID reservationId, boolean compensation);

  public List<RoomView> recommend(LocalDate start, LocalDate end, Long hotelId);
}
