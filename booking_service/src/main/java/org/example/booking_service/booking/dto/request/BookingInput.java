package org.example.booking_service.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookingInput(
    Long roomId,
    Long hotelId,
    boolean autoSelect,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate) {}
