package org.example.booking_service.auth.service.impl;

import org.example.booking_service.auth.dto.request.Credentials;
import org.example.booking_service.auth.dto.response.AuthView;
import org.example.booking_service.auth.service.AuthService;
import org.example.booking_service.user.dto.request.UserInput;
import org.example.booking_service.user.dto.response.UserView;
import org.example.booking_service.user.mapper.UserMapper;
import org.example.booking_service.user.repository.UserRepository;
import org.example.booking_service.user.service.UserService;
import org.example.common.ApiException;
import org.example.common.JwtSupport;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
  private final UserRepository users;
  private final UserService userService;
  private final UserMapper mapper;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;

  public AuthServiceImpl(
      UserRepository users,
      UserService userService,
      UserMapper mapper,
      PasswordEncoder passwords,
      JwtEncoder encoder) {
    this.users = users;
    this.userService = userService;
    this.mapper = mapper;
    this.passwords = passwords;
    this.encoder = encoder;
  }

  @Override
  public AuthView register(Credentials input) {
    return issue(userService.create(new UserInput(input.username(), input.password(), "USER")));
  }

  @Override
  public AuthView login(Credentials input) {
    var user =
        users
            .findByUsername(input.username())
            .orElseThrow(() -> new ApiException(401, "Неверный логин или пароль"));
    if (!user.isEnabled() || !passwords.matches(input.password(), user.getPassword()))
      throw new ApiException(401, "Неверный логин или пароль");
    return issue(mapper.toView(user));
  }

  private AuthView issue(UserView user) {
    return new AuthView(
        JwtSupport.token(encoder, user.id().toString(), user.role()), "Bearer", 3600, user);
  }
}
