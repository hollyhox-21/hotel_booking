package org.example.hotel_management_service.reservation.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.example.common.ApiException;
import org.example.hotel_management_service.reservation.dto.request.ReserveInput;
import org.example.hotel_management_service.reservation.dto.response.ReservationView;
import org.example.hotel_management_service.reservation.entity.Reservation;
import org.example.hotel_management_service.reservation.mapper.ReservationMapper;
import org.example.hotel_management_service.reservation.repository.AllocationLockRepository;
import org.example.hotel_management_service.reservation.repository.ReservationRepository;
import org.example.hotel_management_service.reservation.service.ReservationService;
import org.example.hotel_management_service.room.entity.Room;
import org.example.hotel_management_service.room.service.RoomAvailability;
import org.example.hotel_management_service.room.service.RoomCatalog;
import org.example.hotel_management_service.shared.validation.DateRangeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {
  private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);
  private final ReservationRepository reservations;
  private final AllocationLockRepository locks;
  private final RoomCatalog catalog;
  private final ReservationMapper mapper;

  public ReservationServiceImpl(
      ReservationRepository reservations,
      AllocationLockRepository locks,
      RoomCatalog catalog,
      ReservationMapper mapper) {
    this.reservations = reservations;
    this.locks = locks;
    this.catalog = catalog;
    this.mapper = mapper;
  }

  @Override
  public ReservationView reserve(ReserveInput input) {
    DateRangeValidator.validate(input.startDate(), input.endDate(), true);
    locks.acquire();
    String fingerprint =
        input.bookingId()
            + "|"
            + input.roomId()
            + "|"
            + input.hotelId()
            + "|"
            + input.autoSelect()
            + "|"
            + input.startDate()
            + "|"
            + input.endDate()
            + "|"
            + input.replaces();
    var existing = reservations.findById(input.requestId());
    if (existing.isPresent()) {
      var r = existing.get();
      if (!r.isActive()) throw new ApiException(409, "Резерв уже освобождён");
      if (!fingerprint.equals(r.getFingerprint()))
        throw new ApiException(409, "requestId использован с другими данными");
      return mapper.toView(r);
    }
    if (input.replaces() != null) {
      Reservation old =
          reservations
              .findById(input.replaces())
              .orElseThrow(() -> new ApiException(409, "Исходный резерв не найден"));
      if (!old.isActive() || !old.getBookingId().equals(input.bookingId()))
        throw new ApiException(409, "Исходный резерв не соответствует брони");
    }
    List<Reservation> active = reservations.findByActiveTrue();
    Room selected;
    if (input.autoSelect())
      selected =
          catalog.allRooms().stream()
              .filter(r -> input.hotelId() == null || r.getHotel().getId().equals(input.hotelId()))
              .filter(
                  r ->
                      RoomAvailability.isFree(
                          r, input.startDate(), input.endDate(), input.replaces(), active))
              .min(
                  Comparator.comparingLong((Room r) -> r.getTimesBooked())
                      .thenComparing(r -> r.getId()))
              .orElseThrow(() -> new ApiException(409, "Свободных номеров нет"));
    else {
      if (input.roomId() == null) throw new ApiException(400, "Укажите roomId");
      selected = catalog.room(input.roomId());
      if (!RoomAvailability.isFree(
          selected, input.startDate(), input.endDate(), input.replaces(), active))
        throw new ApiException(409, "Номер занят или недоступен");
    }
    Reservation r = new Reservation(input.requestId());
    r.setBookingId(input.bookingId());
    r.setRoom(selected);
    r.setStartDate(input.startDate());
    r.setEndDate(input.endDate());
    r.setActive(true);
    r.setCounted(
        !reservations.existsByBookingIdAndRoomIdAndCountedTrue(
            input.bookingId(), selected.getId()));
    r.setFingerprint(fingerprint);
    if (r.isCounted()) selected.adjustCount(1);
    reservations.save(r);
    log.info(
        "Reservation accepted bookingId={} requestId={} roomId={}",
        r.getBookingId(),
        r.getId(),
        selected.getId());
    return mapper.toView(r);
  }

  @Override
  public void release(UUID key, boolean compensation, Long expectedRoomId) {
    locks.acquire();
    Reservation r = reservations.findById(key).orElseGet(() -> new Reservation(key));
    if (expectedRoomId != null
        && r.getRoom() != null
        && !r.getRoom().getId().equals(expectedRoomId))
      throw new ApiException(409, "Неверный номер резерва");
    if (r.isActive() && compensation && r.isCounted()) {
      r.getRoom().adjustCount(-1);
      r.setCounted(false);
    }
    r.setActive(false);
    reservations.save(r);
    log.info(
        "Reservation released bookingId={} requestId={} compensation={}",
        r.getBookingId(),
        r.getId(),
        compensation);
  }
}
