package org.example.hotel_management_service.reservation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReleaseInput(@NotNull UUID requestId, boolean compensation) {}
