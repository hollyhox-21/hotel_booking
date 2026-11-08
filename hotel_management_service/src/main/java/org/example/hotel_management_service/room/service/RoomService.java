package org.example.hotel_management_service.room.service;

import java.time.LocalDate;
import java.util.List;
import org.example.hotel_management_service.room.dto.request.RoomInput;
import org.example.hotel_management_service.room.dto.response.RoomView;

public interface RoomService {
  RoomView getRoom(Long id);

  RoomView saveRoom(Long id, RoomInput input);

  void deleteRoom(Long id);

  List<RoomView> available(
      LocalDate start, LocalDate end, Long hotelId, boolean recommended, boolean all, String sort);
}
