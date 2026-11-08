package org.example.hotel_management_service.hotel.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.hotel_management_service.hotel.dto.request.HotelInput;
import org.example.hotel_management_service.hotel.dto.response.HotelView;
import org.example.hotel_management_service.hotel.service.HotelService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HotelController {
  private final HotelService service;

  public HotelController(HotelService service) {
    this.service = service;
  }

  @GetMapping("/hotels")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<HotelView> hotels(
      @RequestParam(required = false) String query,
      @RequestParam(defaultValue = "id") String sort) {
    return service.hotels(query, sort);
  }

  @GetMapping("/hotels/{id}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public HotelView hotel(@PathVariable Long id) {
    return service.getHotel(id);
  }

  @PostMapping("/hotels")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public HotelView createHotel(@Valid @RequestBody HotelInput input) {
    return service.saveHotel(null, input);
  }

  @PutMapping("/hotels/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public HotelView updateHotel(@PathVariable Long id, @Valid @RequestBody HotelInput input) {
    return service.saveHotel(id, input);
  }

  @DeleteMapping("/hotels/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteHotel(@PathVariable Long id) {
    service.deleteHotel(id);
  }
}
