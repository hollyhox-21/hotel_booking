package org.example.hotel_management_service.reservation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "allocation_lock")
public class AllocationLock {
  @Id private Long id;

  protected AllocationLock() {}

  public AllocationLock(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }
}
