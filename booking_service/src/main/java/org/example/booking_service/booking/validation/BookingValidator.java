package org.example.booking_service.booking.validation;

import java.time.LocalDate;
import org.example.booking_service.booking.dto.request.BookingInput;
import org.example.common.ApiException;

public final class BookingValidator {

  private BookingValidator() {}

  public static void validate(BookingInput i) {
    if (!i.autoSelect() && i.roomId() == null)
      throw new ApiException(400, "Укажите roomId или autoSelect=true");
    if (i.startDate() == null
        || i.endDate() == null
        || i.startDate().isBefore(LocalDate.now())
        || !i.endDate().isAfter(i.startDate()))
      throw new ApiException(400, "Дата выезда должна быть позже заезда; заезд не в прошлом");
  }
}
