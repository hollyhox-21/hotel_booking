package org.example.hotel_management_service.room.service;

import java.util.List;
import org.example.common.ApiException;
import org.example.hotel_management_service.hotel.entity.Hotel;
import org.example.hotel_management_service.hotel.repository.HotelRepository;
import org.example.hotel_management_service.room.entity.Room;
import org.example.hotel_management_service.room.repository.RoomRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class RoomCatalog {
  private final HotelRepository hotels;
  private final RoomRepository rooms;

  public RoomCatalog(HotelRepository hotels, RoomRepository rooms) {
    this.hotels = hotels;
    this.rooms = rooms;
  }

  public Hotel hotel(Long id) {
    return hotels.findById(id).orElseThrow(() -> new ApiException(404, "Отель не найден"));
  }

  public Room room(Long id) {
    return org.hibernate.Hibernate.unproxy(
        rooms.findById(id).orElseThrow(() -> new ApiException(404, "Номер не найден")), Room.class);
  }

  public List<Room> allRooms() {
    return rooms.findAll().stream()
        .map(r -> org.hibernate.Hibernate.unproxy(r, Room.class))
        .toList();
  }
}
