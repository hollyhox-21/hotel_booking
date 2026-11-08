package org.example.hotel_management_service.room.repository;

import org.example.hotel_management_service.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
  public boolean existsByHotelId(Long id);
}
