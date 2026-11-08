package org.example.hotel_management_service.room.dto.response;

public record RoomView(
    Long id, Long hotelId, String hotelName, String number, boolean available, long timesBooked) {}
