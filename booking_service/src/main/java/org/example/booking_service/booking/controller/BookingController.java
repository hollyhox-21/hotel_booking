package org.example.booking_service.booking.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.booking_service.booking.dto.request.BookingInput;
import org.example.booking_service.booking.dto.response.BookingView;
import org.example.booking_service.booking.service.BookingService;
import org.example.booking_service.integration.hotel.dto.response.RoomView;
import org.example.booking_service.shared.dto.response.PageView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/bookings")
@PreAuthorize("hasRole('USER')")
public class BookingController {
  private final BookingService service;

  public BookingController(BookingService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public BookingView create(
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody BookingInput input) {
    return service.save(Long.valueOf(jwt.getSubject()), null, key, input);
  }

  @PutMapping("/{id}")
  public BookingView update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody BookingInput input) {
    return service.save(Long.valueOf(jwt.getSubject()), id, key, input);
  }

  @DeleteMapping("/{id}")
  public BookingView cancel(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @RequestHeader("Idempotency-Key") UUID key) {
    return service.cancel(Long.valueOf(jwt.getSubject()), id, key);
  }

  @GetMapping
  public PageView<BookingView> list(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    return service.list(Long.valueOf(jwt.getSubject()), page, size);
  }

  @GetMapping("/recommendations")
  public List<RoomView> recommendations(
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate,
      @RequestParam(required = false) Long hotelId) {
    return service.recommendations(startDate, endDate, hotelId);
  }

  @GetMapping("/{id}")
  public BookingView get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
    return service.get(Long.valueOf(jwt.getSubject()), id);
  }
}
