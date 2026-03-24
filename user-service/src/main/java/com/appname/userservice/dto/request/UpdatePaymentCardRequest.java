package com.appname.userservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePaymentCardRequest {
  @Size(min = 1, max = 100, message = "Holder name must be between 1 and 100 characters")
  private String holder;

  @Future(message = "Expiration date must be in the future")
  private LocalDate expirationDate;

}
