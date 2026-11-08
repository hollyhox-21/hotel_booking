package org.example.hotel_management_service.statistics.controller;

import java.time.LocalDate;
import java.util.List;
import org.example.hotel_management_service.statistics.dto.response.RoomStats;
import org.example.hotel_management_service.statistics.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatisticsController {
  private final StatisticsService service;

  public StatisticsController(StatisticsService service) {
    this.service = service;
  }

  @GetMapping("/hotels/statistics")
  @PreAuthorize("hasRole('ADMIN')")
  public List<RoomStats> statistics(
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate,
      @RequestParam(required = false) Long hotelId) {
    return service.stats(startDate, endDate, hotelId);
  }
}
