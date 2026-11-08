package org.example.hotel_management_service.room.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.hotel_management_service.reservation.entity.Reservation;
import org.example.hotel_management_service.room.entity.Room;

public final class RoomAvailability {

  private RoomAvailability() {}

  public static boolean isFree(
      Room room, LocalDate start, LocalDate end, UUID excluded, List<Reservation> active) {
    return room.isAvailable()
        && active.stream()
            .noneMatch(
                r ->
                    r.getRoom().getId().equals(room.getId())
                        && !r.getId().equals(excluded)
                        && r.getStartDate().isBefore(end)
                        && r.getEndDate().isAfter(start));
  }
}
