package com.appname.userservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserRequest {
  @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
  private String name;

  @Size(min = 1, max = 100, message = "Surname must be between 1 and 100 characters")
  private String surname;

  @Past(message = "Birth date must be in the past")
  private LocalDate birthDate;

  @Email(message = "Email must be valid")
  @Size(max = 255, message = "Email must not exceed 255 characters")
  private String email;

}
