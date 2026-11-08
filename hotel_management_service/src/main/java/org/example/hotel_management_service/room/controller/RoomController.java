package org.example.hotel_management_service.room.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.example.hotel_management_service.room.dto.request.RoomInput;
import org.example.hotel_management_service.room.dto.response.RoomView;
import org.example.hotel_management_service.room.service.RoomService;
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
public class RoomController {
  private final RoomService service;

  public RoomController(RoomService service) {
    this.service = service;
  }

  @GetMapping("/rooms")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<RoomView> rooms(
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate,
      @RequestParam(required = false) Long hotelId,
      @RequestParam(defaultValue = "id") String sort) {
    return service.available(startDate, endDate, hotelId, false, false, sort);
  }

  @GetMapping("/rooms/recommend")
  @PreAuthorize("hasAnyRole('USER','INTERNAL')")
  public List<RoomView> recommend(
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate,
      @RequestParam(required = false) Long hotelId) {
    return service.available(startDate, endDate, hotelId, true, false, "id");
  }

  @GetMapping("/rooms/all")
  @PreAuthorize("hasRole('ADMIN')")
  public List<RoomView> all(
      @RequestParam(required = false) Long hotelId,
      @RequestParam(defaultValue = "id") String sort) {
    return service.available(null, null, hotelId, false, true, sort);
  }

  @GetMapping("/rooms/{id}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public RoomView room(@PathVariable Long id) {
    return service.getRoom(id);
  }

  @PostMapping("/rooms")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public RoomView createRoom(@Valid @RequestBody RoomInput input) {
    return service.saveRoom(null, input);
  }

  @PutMapping("/rooms/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public RoomView updateRoom(@PathVariable Long id, @Valid @RequestBody RoomInput input) {
    return service.saveRoom(id, input);
  }

  @DeleteMapping("/rooms/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteRoom(@PathVariable Long id) {
    service.deleteRoom(id);
  }
}
