package com.appname.gateway.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserResponse {
  private Long id;
  private String name;
  private String surname;
  private LocalDate birthDate;
  private String email;
  private Boolean active;

}
