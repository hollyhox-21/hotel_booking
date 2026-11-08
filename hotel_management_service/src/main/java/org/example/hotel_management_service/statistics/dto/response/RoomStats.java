package org.example.hotel_management_service.statistics.dto.response;

public record RoomStats(
    Long roomId,
    String hotelName,
    String number,
    long timesBooked,
    long reservedNights,
    long totalNights,
    double occupancyPercent) {}
