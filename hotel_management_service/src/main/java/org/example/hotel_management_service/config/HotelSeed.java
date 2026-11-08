package org.example.hotel_management_service.config;

import org.example.hotel_management_service.hotel.entity.Hotel;
import org.example.hotel_management_service.hotel.repository.HotelRepository;
import org.example.hotel_management_service.reservation.entity.AllocationLock;
import org.example.hotel_management_service.reservation.repository.AllocationLockRepository;
import org.example.hotel_management_service.room.entity.Room;
import org.example.hotel_management_service.room.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HotelSeed implements CommandLineRunner {
  private final AllocationLockRepository locks;
  private final HotelRepository hotels;
  private final RoomRepository rooms;
  private final boolean enabled;

  public HotelSeed(
      AllocationLockRepository locks,
      HotelRepository hotels,
      RoomRepository rooms,
      @Value("${app.seed.enabled}") boolean enabled) {
    this.locks = locks;
    this.hotels = hotels;
    this.rooms = rooms;
    this.enabled = enabled;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!locks.existsById(1L)) locks.save(new AllocationLock(1L));
    if (!enabled || hotels.count() > 0) return;
    Hotel a = hotels.save(new Hotel("Северный берег", "Санкт-Петербург, набережная 12"));
    Hotel b = hotels.save(new Hotel("Городской сад", "Москва, Садовая улица 8"));
    for (int i = 1; i <= 4; i++) rooms.save(new Room(a, "10" + i));
    for (int i = 1; i <= 3; i++) rooms.save(new Room(b, "20" + i));
    Room repair = new Room(b, "204");
    repair.setAvailable(false);
    rooms.save(repair);
  }
}
