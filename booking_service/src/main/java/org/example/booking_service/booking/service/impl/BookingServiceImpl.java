package org.example.booking_service.booking.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.booking_service.booking.dto.request.BookingInput;
import org.example.booking_service.booking.dto.response.BookingView;
import org.example.booking_service.booking.saga.BookingSagaOrchestrator;
import org.example.booking_service.booking.saga.BookingTransactions;
import org.example.booking_service.booking.service.BookingService;
import org.example.booking_service.booking.validation.BookingValidator;
import org.example.booking_service.integration.hotel.HotelPort;
import org.example.booking_service.integration.hotel.dto.response.RoomView;
import org.example.booking_service.shared.dto.response.PageView;
import org.springframework.stereotype.Service;

@Service
public class BookingServiceImpl implements BookingService {
  private final BookingSagaOrchestrator saga;
  private final BookingTransactions tx;
  private final HotelPort hotel;

  public BookingServiceImpl(BookingSagaOrchestrator saga, BookingTransactions tx, HotelPort hotel) {
    this.saga = saga;
    this.tx = tx;
    this.hotel = hotel;
  }

  @Override
  public BookingView save(Long userId, Long bookingId, UUID key, BookingInput input) {
    return saga.save(userId, bookingId, key, input);
  }

  @Override
  public BookingView cancel(Long userId, Long id, UUID key) {
    return saga.cancel(userId, id, key);
  }

  @Override
  public PageView<BookingView> list(Long userId, int page, int size) {
    return tx.list(userId, page, size);
  }

  @Override
  public BookingView get(Long userId, Long id) {
    return tx.get(userId, id);
  }

  @Override
  public List<RoomView> recommendations(LocalDate start, LocalDate end, Long hotelId) {
    BookingValidator.validate(new BookingInput(null, hotelId, true, start, end));
    return hotel.recommend(start, end, hotelId);
  }
}
