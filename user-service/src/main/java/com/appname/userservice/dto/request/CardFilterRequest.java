package com.appname.userservice.dto.request;

import lombok.Data;

@Data
public class CardFilterRequest {
  private String holder;
  private Boolean active;

}
