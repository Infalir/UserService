package com.appname.userservice.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
  private Long id;
  private String name;
  private String surname;
  private LocalDate birthDate;
  private String email;
  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<PaymentCardResponse> paymentCards;

}
