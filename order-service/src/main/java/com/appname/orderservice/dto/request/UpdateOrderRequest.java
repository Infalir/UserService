package com.appname.orderservice.dto.request;

import com.appname.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderRequest {
  @NotNull(message = "Status is required")
  private OrderStatus status;

}
