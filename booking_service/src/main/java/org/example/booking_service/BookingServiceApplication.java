package org.example.booking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"org.example.booking_service", "org.example.common"})
@EnableScheduling
public class BookingServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(BookingServiceApplication.class, args);
  }
}
