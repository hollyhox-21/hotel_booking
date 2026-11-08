package org.example.booking_service.booking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.booking_service.booking.dto.request.BookingInput;
import org.example.booking_service.booking.dto.response.BookingView;
import org.example.booking_service.integration.hotel.dto.response.RoomView;
import org.example.booking_service.shared.dto.response.PageView;

public interface BookingService {
  BookingView save(Long userId, Long bookingId, UUID key, BookingInput input);

  BookingView cancel(Long userId, Long id, UUID key);

  PageView<BookingView> list(Long userId, int page, int size);

  BookingView get(Long userId, Long id);

  List<RoomView> recommendations(LocalDate start, LocalDate end, Long hotelId);
}
