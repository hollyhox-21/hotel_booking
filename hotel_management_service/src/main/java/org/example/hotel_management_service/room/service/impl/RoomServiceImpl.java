package org.example.hotel_management_service.room.service.impl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.example.common.ApiException;
import org.example.hotel_management_service.reservation.entity.Reservation;
import org.example.hotel_management_service.reservation.repository.AllocationLockRepository;
import org.example.hotel_management_service.reservation.repository.ReservationRepository;
import org.example.hotel_management_service.room.dto.request.RoomInput;
import org.example.hotel_management_service.room.dto.response.RoomView;
import org.example.hotel_management_service.room.entity.Room;
import org.example.hotel_management_service.room.mapper.RoomMapper;
import org.example.hotel_management_service.room.repository.RoomRepository;
import org.example.hotel_management_service.room.service.RoomAvailability;
import org.example.hotel_management_service.room.service.RoomCatalog;
import org.example.hotel_management_service.room.service.RoomService;
import org.example.hotel_management_service.shared.validation.DateRangeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {
  private final RoomRepository rooms;
  private final ReservationRepository reservations;
  private final AllocationLockRepository locks;
  private final RoomCatalog catalog;
  private final RoomMapper mapper;

  public RoomServiceImpl(
      RoomRepository rooms,
      ReservationRepository reservations,
      AllocationLockRepository locks,
      RoomCatalog catalog,
      RoomMapper mapper) {
    this.rooms = rooms;
    this.reservations = reservations;
    this.locks = locks;
    this.catalog = catalog;
    this.mapper = mapper;
  }

  @Override
  public RoomView getRoom(Long id) {
    return mapper.toView(catalog.room(id));
  }

  @Override
  public RoomView saveRoom(Long id, RoomInput input) {
    locks.acquire();
    Room r =
        id == null ? new Room(catalog.hotel(input.hotelId()), input.number()) : catalog.room(id);
    if (id != null
        && !r.getHotel().getId().equals(input.hotelId())
        && reservations.existsByRoomId(id))
      throw new ApiException(409, "Нельзя переносить номер с историей резервов в другой отель");
    r.setHotel(catalog.hotel(input.hotelId()));
    r.setNumber(input.number());
    r.setAvailable(input.available());
    return mapper.toView(rooms.saveAndFlush(r));
  }

  @Override
  public void deleteRoom(Long id) {
    locks.acquire();
    Room r = catalog.room(id);
    if (reservations.existsByRoomId(id))
      throw new ApiException(409, "Номер имеет историю резервов: выведите его из эксплуатации");
    rooms.delete(r);
  }

  @Transactional(readOnly = true)
  @Override
  public List<RoomView> available(
      LocalDate start, LocalDate end, Long hotelId, boolean recommended, boolean all, String sort) {
    if (!all) DateRangeValidator.validate(start, end, true);
    Comparator<Room> cmp =
        recommended
            ? Comparator.comparingLong((Room r) -> r.getTimesBooked()).thenComparing(r -> r.getId())
            : switch (sort) {
              case "id" -> Comparator.comparing(r -> r.getId());
              case "number" ->
                  Comparator.comparing((Room r) -> r.getNumber()).thenComparing(r -> r.getId());
              case "timesBooked" ->
                  Comparator.comparingLong((Room r) -> r.getTimesBooked())
                      .thenComparing(r -> r.getId());
              default -> throw new ApiException(400, "Сортировка: id, number, timesBooked");
            };
    List<Reservation> active = all ? List.of() : reservations.findByActiveTrue();
    return catalog.allRooms().stream()
        .filter(r -> hotelId == null || r.getHotel().getId().equals(hotelId))
        .filter(r -> all || RoomAvailability.isFree(r, start, end, null, active))
        .sorted(cmp)
        .map(mapper::toView)
        .toList();
  }
}
