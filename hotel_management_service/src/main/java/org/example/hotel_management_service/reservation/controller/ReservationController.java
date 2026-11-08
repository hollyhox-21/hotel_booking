package org.example.hotel_management_service.reservation.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.example.hotel_management_service.reservation.dto.request.ReleaseInput;
import org.example.hotel_management_service.reservation.dto.request.ReserveInput;
import org.example.hotel_management_service.reservation.dto.response.ReservationView;
import org.example.hotel_management_service.reservation.service.ReservationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReservationController {
  private final ReservationService service;

  public ReservationController(ReservationService service) {
    this.service = service;
  }

  @PostMapping("/internal/reservations")
  @PreAuthorize("hasRole('INTERNAL')")
  public ReservationView reserve(@Valid @RequestBody ReserveInput input) {
    return service.reserve(input);
  }

  @PostMapping("/rooms/{id}/confirm-availability")
  @PreAuthorize("hasRole('INTERNAL')")
  public ReservationView confirm(@PathVariable Long id, @Valid @RequestBody ReserveInput i) {
    return service.reserve(
        new ReserveInput(
            i.requestId(),
            i.bookingId(),
            id,
            i.hotelId(),
            false,
            i.startDate(),
            i.endDate(),
            i.replaces()));
  }

  @PostMapping("/internal/reservations/{key}/release")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('INTERNAL')")
  public void release(
      @PathVariable UUID key, @RequestParam(defaultValue = "false") boolean compensation) {
    service.release(key, compensation, null);
  }

  @PostMapping("/rooms/{id}/release")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('INTERNAL')")
  public void releaseRoom(@PathVariable Long id, @Valid @RequestBody ReleaseInput input) {
    service.release(input.requestId(), input.compensation(), id);
  }
}
