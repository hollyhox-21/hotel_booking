package org.example.hotel_management_service.reservation.repository;

import jakarta.persistence.LockModeType;
import org.example.hotel_management_service.reservation.entity.AllocationLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AllocationLockRepository extends JpaRepository<AllocationLock, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from AllocationLock l where l.id=1")
  public AllocationLock acquire();
}
