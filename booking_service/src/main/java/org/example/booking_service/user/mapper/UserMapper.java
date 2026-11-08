package org.example.booking_service.user.mapper;

import org.example.booking_service.user.dto.response.UserView;
import org.example.booking_service.user.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserView toView(UserAccount user) {
    return new UserView(user.getId(), user.getUsername(), user.getRole(), user.isEnabled());
  }
}
