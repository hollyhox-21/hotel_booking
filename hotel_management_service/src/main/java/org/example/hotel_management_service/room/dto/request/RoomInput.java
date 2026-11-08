package org.example.hotel_management_service.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomInput(
    @NotNull Long hotelId, @NotBlank @Size(max = 30) String number, @NotNull Boolean available) {}
