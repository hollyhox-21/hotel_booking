package org.example.booking_service.user.service.impl;

import org.example.booking_service.shared.dto.response.PageView;
import org.example.booking_service.user.dto.request.UserInput;
import org.example.booking_service.user.dto.request.UserPatch;
import org.example.booking_service.user.dto.response.UserView;
import org.example.booking_service.user.entity.UserAccount;
import org.example.booking_service.user.mapper.UserMapper;
import org.example.booking_service.user.repository.UserRepository;
import org.example.booking_service.user.service.UserService;
import org.example.common.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final UserMapper mapper;

  public UserServiceImpl(UserRepository users, PasswordEncoder passwords, UserMapper mapper) {
    this.users = users;
    this.passwords = passwords;
    this.mapper = mapper;
  }

  @Override
  public UserView create(UserInput i) {
    if (users.existsByUsername(i.username()))
      throw new ApiException(409, "Имя пользователя занято");
    return mapper.toView(
        users.saveAndFlush(
            new UserAccount(i.username(), passwords.encode(i.password()), i.role())));
  }

  @Override
  public PageView<UserView> list(int page, int size) {
    var result = users.findAll(PageRequest.of(page, size, Sort.by("id")));
    return new PageView<>(
        result.map(mapper::toView).getContent(),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public UserView get(Long id) {
    return mapper.toView(
        users.findById(id).orElseThrow(() -> new ApiException(404, "Пользователь не найден")));
  }

  @Override
  public UserView update(Long id, UserPatch i, Long adminId) {
    UserAccount u =
        users.locked(id).orElseThrow(() -> new ApiException(404, "Пользователь не найден"));
    if (id.equals(adminId)
        && (Boolean.FALSE.equals(i.enabled()) || (i.role() != null && !i.role().equals("ADMIN"))))
      throw new ApiException(409, "Нельзя отключить собственную административную учётную запись");
    if (i.username() != null) u.setUsername(i.username());
    if (i.password() != null) u.setPassword(passwords.encode(i.password()));
    if (i.role() != null) u.setRole(i.role());
    if (i.enabled() != null) u.setEnabled(i.enabled());
    users.flush();
    return mapper.toView(u);
  }

  @Override
  public void delete(Long id, Long adminId) {
    update(id, new UserPatch(null, null, null, false), adminId);
  }

  @Override
  public UserView me(Long id) {
    var u =
        users.findById(id).orElseThrow(() -> new ApiException(401, "Учётная запись недоступна"));
    if (!u.isEnabled()) throw new ApiException(401, "Учётная запись отключена");
    return mapper.toView(u);
  }
}
