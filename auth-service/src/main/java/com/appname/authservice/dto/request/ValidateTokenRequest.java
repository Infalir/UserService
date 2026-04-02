package com.appname.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateTokenRequest {
  @NotBlank(message = "Token is required")
  private String token;

}