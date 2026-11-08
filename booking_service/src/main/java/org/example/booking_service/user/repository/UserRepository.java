package org.example.booking_service.user.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.example.booking_service.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
  public Optional<UserAccount> findByUsername(String username);

  public boolean existsByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserAccount u where u.id=:id")
  public Optional<UserAccount> locked(Long id);
}
