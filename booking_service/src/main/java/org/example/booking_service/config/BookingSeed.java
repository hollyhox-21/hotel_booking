package org.example.booking_service.config;

import org.example.booking_service.user.entity.UserAccount;
import org.example.booking_service.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingSeed implements CommandLineRunner {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final boolean enabled;

  public BookingSeed(
      UserRepository users,
      PasswordEncoder passwords,
      @Value("${app.seed.enabled}") boolean enabled) {
    this.users = users;
    this.passwords = passwords;
    this.enabled = enabled;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled || users.count() > 0) return;
    users.save(new UserAccount("admin", passwords.encode("Admin123!"), "ADMIN"));
    users.save(new UserAccount("alice", passwords.encode("User12345!"), "USER"));
    users.save(new UserAccount("bob", passwords.encode("User12345!"), "USER"));
  }
}
