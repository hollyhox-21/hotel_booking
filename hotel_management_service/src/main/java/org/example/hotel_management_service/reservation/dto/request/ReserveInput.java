package org.example.hotel_management_service.reservation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ReserveInput(
    @NotNull UUID requestId,
    @NotNull Long bookingId,
    Long roomId,
    Long hotelId,
    boolean autoSelect,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    UUID replaces) {}
