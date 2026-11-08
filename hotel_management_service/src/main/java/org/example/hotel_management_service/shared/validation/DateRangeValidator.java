package org.example.hotel_management_service.shared.validation;

import java.time.LocalDate;
import org.example.common.ApiException;

public final class DateRangeValidator {

  private DateRangeValidator() {}

  public static void validate(LocalDate start, LocalDate end, boolean future) {
    if (start == null
        || end == null
        || !end.isAfter(start)
        || (future && start.isBefore(LocalDate.now())))
      throw new ApiException(400, "Дата выезда должна быть позже заезда; заезд не в прошлом");
  }
}
