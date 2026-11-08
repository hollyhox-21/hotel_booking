package org.example.hotel_management_service.statistics.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.example.hotel_management_service.reservation.entity.Reservation;
import org.example.hotel_management_service.reservation.repository.ReservationRepository;
import org.example.hotel_management_service.room.service.RoomCatalog;
import org.example.hotel_management_service.shared.validation.DateRangeValidator;
import org.example.hotel_management_service.statistics.dto.response.RoomStats;
import org.example.hotel_management_service.statistics.service.StatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {
  private final ReservationRepository reservations;
  private final RoomCatalog catalog;

  public StatisticsServiceImpl(ReservationRepository reservations, RoomCatalog catalog) {
    this.reservations = reservations;
    this.catalog = catalog;
  }

  @Transactional(readOnly = true)
  @Override
  public List<RoomStats> stats(LocalDate start, LocalDate end, Long hotelId) {
    DateRangeValidator.validate(start, end, false);
    long total = ChronoUnit.DAYS.between(start, end);
    var active = reservations.findByActiveTrue();
    return catalog.allRooms().stream()
        .filter(r -> hotelId == null || r.getHotel().getId().equals(hotelId))
        .sorted(Comparator.comparing(r -> r.getId()))
        .map(
            r -> {
              long nights = occupiedNights(active, r.getId(), start, end);
              return new RoomStats(
                  r.getId(),
                  r.getHotel().getName(),
                  r.getNumber(),
                  r.getTimesBooked(),
                  nights,
                  total,
                  Math.round(10000.0 * nights / total) / 100.0);
            })
        .toList();
  }

  private long occupiedNights(
      List<Reservation> active, Long roomId, LocalDate start, LocalDate end) {
    var intervals =
        active.stream()
            .filter(
                a ->
                    a.getRoom().getId().equals(roomId)
                        && a.getStartDate().isBefore(end)
                        && a.getEndDate().isAfter(start))
            .sorted(Comparator.comparing(a -> a.getStartDate()))
            .toList();
    LocalDate cursor = start;
    long occupied = 0;
    for (var interval : intervals) {
      LocalDate from = interval.getStartDate().isBefore(cursor) ? cursor : interval.getStartDate();
      LocalDate to = interval.getEndDate().isAfter(end) ? end : interval.getEndDate();
      if (to.isAfter(from)) {
        occupied += ChronoUnit.DAYS.between(from, to);
        cursor = to;
      }
    }
    return occupied;
  }
}
