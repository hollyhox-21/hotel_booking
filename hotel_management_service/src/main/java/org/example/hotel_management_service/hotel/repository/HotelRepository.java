package org.example.hotel_management_service.hotel.repository;

import org.example.hotel_management_service.hotel.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {}
