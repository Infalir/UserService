package com.appname.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
  @NotNull(message = "User ID is required")
  private Long userId;

  @NotEmpty(message = "Order must contain at least one item")
  @Valid
  private List<OrderItemRequest> items;

}