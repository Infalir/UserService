package com.appname.userservice.dto.request;

import lombok.Data;

@Data
public class UserFilterRequest {
  private String name;
  private String surname;
  private Boolean active;

}
