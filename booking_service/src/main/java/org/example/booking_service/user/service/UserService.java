package org.example.booking_service.user.service;

import org.example.booking_service.shared.dto.response.PageView;
import org.example.booking_service.user.dto.request.UserInput;
import org.example.booking_service.user.dto.request.UserPatch;
import org.example.booking_service.user.dto.response.UserView;

public interface UserService {
  UserView create(UserInput input);

  PageView<UserView> list(int page, int size);

  UserView get(Long id);

  UserView update(Long id, UserPatch input, Long adminId);

  void delete(Long id, Long adminId);

  UserView me(Long id);
}
