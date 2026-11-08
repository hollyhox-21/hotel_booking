package org.example.hotel_management_service.hotel.mapper;

import org.example.hotel_management_service.hotel.dto.response.HotelView;
import org.example.hotel_management_service.hotel.entity.Hotel;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

  public HotelView toView(Hotel h) {
    return new HotelView(h.getId(), h.getName(), h.getAddress());
  }
}
