package com.appname.userservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePaymentCardRequest {
  @NotBlank(message = "Card number is required")
  @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be between 13 and 19 digits")
  private String number;

  @NotBlank(message = "Holder name is required")
  @Size(min = 1, max = 100, message = "Holder name must be between 1 and 100 characters")
  private String holder;

  @NotNull(message = "Expiration date is required")
  @Future(message = "Expiration date must be in the future")
  private LocalDate expirationDate;

}