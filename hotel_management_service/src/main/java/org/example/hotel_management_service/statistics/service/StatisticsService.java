package org.example.hotel_management_service.statistics.service;

import java.time.LocalDate;
import java.util.List;
import org.example.hotel_management_service.statistics.dto.response.RoomStats;

public interface StatisticsService {
  List<RoomStats> stats(LocalDate start, LocalDate end, Long hotelId);
}
