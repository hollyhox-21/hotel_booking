package org.example.hotel_management_service.room.mapper;

import org.example.hotel_management_service.room.dto.response.RoomView;
import org.example.hotel_management_service.room.entity.Room;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

  public RoomView toView(Room r) {
    return new RoomView(
        r.getId(),
        r.getHotel().getId(),
        r.getHotel().getName(),
        r.getNumber(),
        r.isAvailable(),
        r.getTimesBooked());
  }
}
