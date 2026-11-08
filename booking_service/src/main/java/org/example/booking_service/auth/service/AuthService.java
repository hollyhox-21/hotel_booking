package org.example.booking_service.auth.service;

import org.example.booking_service.auth.dto.request.Credentials;
import org.example.booking_service.auth.dto.response.AuthView;

public interface AuthService {
  AuthView register(Credentials input);

  AuthView login(Credentials input);
}
