package com.appname.userservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserRequest {
  @NotBlank(message = "Name is required")
  @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
  private String name;

  @NotBlank(message = "Surname is required")
  @Size(min = 1, max = 100, message = "Surname must be between 1 and 100 characters")
  private String surname;

  @NotNull(message = "Birth date is required")
  @Past(message = "Birth date must be in the past")
  private LocalDate birthDate;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  @Size(max = 255, message = "Email must not exceed 255 characters")
  private String email;

}
