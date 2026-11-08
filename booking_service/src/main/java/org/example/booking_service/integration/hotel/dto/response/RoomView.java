package org.example.booking_service.integration.hotel.dto.response;

public record RoomView(
    Long id, Long hotelId, String hotelName, String number, boolean available, long timesBooked) {}
