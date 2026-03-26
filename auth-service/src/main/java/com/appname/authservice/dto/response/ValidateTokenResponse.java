package com.appname.authservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidateTokenResponse {
  private boolean valid;
  private Long userId;
  private String login;
  private String role;

}
