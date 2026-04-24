package com.appname.paymentservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {
  @NotBlank(message = "Order ID is required")
  private String orderId;

  @NotBlank(message = "User ID is required")
  private String userId;

  @NotNull(message = "Payment amount is required")
  @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
  private BigDecimal paymentAmount;

}
