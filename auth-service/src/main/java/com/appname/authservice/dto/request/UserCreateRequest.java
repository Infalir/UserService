package com.appname.authservice.dto.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserCreateRequest {
  private String name;
  private String surname;
  private LocalDate birthDate;
  private String email;

}