package com.appname.authservice.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserServiceResponse {
  private Long id;
  private String name;
  private String surname;
  private LocalDate birthDate;
  private String email;
  private Boolean active;

}
