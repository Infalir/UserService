package com.appname.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCredentialsRequest {
  @NotNull(message = "User ID is required")
  private Long userId;

  @NotBlank(message = "Login is required")
  @Size(min = 3, max = 255, message = "Login must be between 3 and 255 characters")
  private String login;

  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String password;

  private String role;

}