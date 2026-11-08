package org.example.hotel_management_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {"org.example.hotel_management_service", "org.example.common"})
@EnableScheduling
public class HotelManagementServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(HotelManagementServiceApplication.class, args);
  }
}
