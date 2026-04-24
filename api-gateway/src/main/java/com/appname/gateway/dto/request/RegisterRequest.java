package com.appname.gateway.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
  private String name;
  private String surname;
  private LocalDate birthDate;
  private String email;
  private String login;
  private String password;
  private String role;

}