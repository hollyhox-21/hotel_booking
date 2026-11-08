package org.example.booking_service.booking.mapper;

import java.util.UUID;
import org.example.booking_service.booking.dto.response.BookingView;
import org.example.booking_service.booking.entity.Booking;
import org.example.booking_service.booking.saga.entity.BookingOperation;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

  public BookingView toView(Booking b, UUID key, boolean cleanupPending) {
    return new BookingView(
        b.getId(),
        b.getUser().getId(),
        b.getRoomId(),
        b.getStartDate(),
        b.getEndDate(),
        b.getStatus(),
        b.getCreatedAt(),
        key,
        cleanupPending);
  }

  public BookingView toView(BookingOperation o, boolean cleanupPending) {
    return new BookingView(
        o.getBooking().getId(),
        o.getUser().getId(),
        o.getResultRoomId(),
        o.getResultStartDate(),
        o.getResultEndDate(),
        o.getResultStatus(),
        o.getBooking().getCreatedAt(),
        o.getId(),
        cleanupPending);
  }
}
