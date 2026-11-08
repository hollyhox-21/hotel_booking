package org.example.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class Errors {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Errors.class);

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorDTO> handle(Exception ex, HttpServletRequest request) {
    int status = 500;
    String message = "Внутренняя ошибка сервера";
    if (ex instanceof ApiException e) {
      status = e.status();
      message = e.getMessage();
    } else if (ex instanceof AccessDeniedException) {
      status = 403;
      message = "Доступ запрещён";
    } else if (ex instanceof MethodArgumentNotValidException e) {
      status = 400;
      message =
          e.getBindingResult().getFieldErrors().stream()
              .map(f -> f.getField() + ": " + f.getDefaultMessage())
              .sorted()
              .findFirst()
              .orElse("Неверные данные");
    } else if (ex instanceof ConstraintViolationException
        || ex instanceof IllegalArgumentException
        || ex instanceof MethodArgumentTypeMismatchException
        || ex instanceof HttpMessageNotReadableException) {
      status = 400;
      message = "Некорректный запрос";
    } else if (ex instanceof org.springframework.web.bind.MissingRequestValueException
        || ex
            instanceof org.springframework.web.method.annotation.HandlerMethodValidationException) {
      status = 400;
      message = "Не заданы обязательные параметры или нарушены ограничения";
    } else if (ex instanceof org.springframework.web.servlet.resource.NoResourceFoundException) {
      status = 404;
      message = "Ресурс не найден";
    } else if (ex instanceof DataIntegrityViolationException) {
      status = 409;
      message = "Конфликт данных";
    } else if (ex instanceof ResponseStatusException e) {
      status = e.getStatusCode().value();
      message = e.getReason();
    }
    if (status == 500) log.error("Unhandled request failure", ex);
    return ResponseEntity.status(status)
        .body(
            new ErrorDTO(
                Instant.now(), status, message, request.getRequestURI(), MDC.get("traceId")));
  }
}
