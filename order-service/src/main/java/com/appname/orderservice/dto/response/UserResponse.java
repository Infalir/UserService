package com.appname.orderservice.dto.response;

import lombok.Data;

import java.time.LocalDate;

// Represents the user info returned by User Service
@Data
public class UserResponse {
  private Long id;
  private String name;
  private String surname;
  private LocalDate birthDate;
  private String email;
  private Boolean active;

}
