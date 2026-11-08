package org.example.hotel_management_service.hotel.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.example.common.ApiException;
import org.example.hotel_management_service.hotel.dto.request.HotelInput;
import org.example.hotel_management_service.hotel.dto.response.HotelView;
import org.example.hotel_management_service.hotel.entity.Hotel;
import org.example.hotel_management_service.hotel.mapper.HotelMapper;
import org.example.hotel_management_service.hotel.repository.HotelRepository;
import org.example.hotel_management_service.hotel.service.HotelService;
import org.example.hotel_management_service.reservation.repository.AllocationLockRepository;
import org.example.hotel_management_service.room.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HotelServiceImpl implements HotelService {
  private final HotelRepository hotels;
  private final RoomRepository rooms;
  private final AllocationLockRepository locks;
  private final HotelMapper mapper;

  public HotelServiceImpl(
      HotelRepository hotels,
      RoomRepository rooms,
      AllocationLockRepository locks,
      HotelMapper mapper) {
    this.hotels = hotels;
    this.rooms = rooms;
    this.locks = locks;
    this.mapper = mapper;
  }

  private Hotel hotel(Long id) {
    return hotels.findById(id).orElseThrow(() -> new ApiException(404, "Отель не найден"));
  }

  @Transactional(readOnly = true)
  @Override
  public List<HotelView> hotels(String query, String sort) {
    Comparator<Hotel> cmp =
        switch (sort) {
          case "id" -> Comparator.comparing(h -> h.getId());
          case "name" -> Comparator.comparing(h -> h.getName());
          default -> throw new ApiException(400, "Сортировка: id или name");
        };
    return hotels.findAll().stream()
        .filter(
            h ->
                query == null
                    || (h.getName() + " " + h.getAddress())
                        .toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT)))
        .sorted(cmp)
        .map(mapper::toView)
        .toList();
  }

  @Override
  public HotelView saveHotel(Long id, HotelInput input) {
    locks.acquire();
    Hotel h = id == null ? new Hotel(input.name(), input.address()) : hotel(id);
    h.setName(input.name());
    h.setAddress(input.address());
    return mapper.toView(hotels.save(h));
  }

  @Override
  public void deleteHotel(Long id) {
    locks.acquire();
    Hotel h = hotel(id);
    if (rooms.existsByHotelId(id)) throw new ApiException(409, "Сначала удалите номера отеля");
    hotels.delete(h);
  }

  @Override
  public HotelView getHotel(Long id) {
    return mapper.toView(hotel(id));
  }
}
