package com.appname.paymentservice.dto.response;

import com.appname.paymentservice.entity.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
  private String id;
  private String orderId;
  private String userId;
  private PaymentStatus status;
  private LocalDateTime timestamp;
  private BigDecimal paymentAmount;

}
