package com.appname.paymentservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentSumResponse {
  private BigDecimal total;
  private LocalDateTime from;
  private LocalDateTime to;
  private String userId;

}
