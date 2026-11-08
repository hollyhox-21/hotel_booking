package org.example.hotel_management_service.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelInput(
    @NotBlank @Size(max = 150) String name, @NotBlank @Size(max = 300) String address) {}
