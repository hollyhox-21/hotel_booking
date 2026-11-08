package org.example.hotel_management_service.hotel.service;

import java.util.List;
import org.example.hotel_management_service.hotel.dto.request.HotelInput;
import org.example.hotel_management_service.hotel.dto.response.HotelView;

public interface HotelService {
  List<HotelView> hotels(String query, String sort);

  HotelView saveHotel(Long id, HotelInput input);

  void deleteHotel(Long id);

  HotelView getHotel(Long id);
}
