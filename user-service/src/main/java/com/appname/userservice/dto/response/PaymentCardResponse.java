package com.appname.userservice.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentCardResponse {
  private Long id;
  private Long userId;
  private String number;
  private String holder;
  private LocalDate expirationDate;
  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

}
