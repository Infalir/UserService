package com.appname.orderservice.dto.response;

import com.appname.orderservice.entity.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
  private Long id;
  private OrderStatus status;
  private BigDecimal totalPrice;
  private Boolean deleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<OrderItemResponse> orderItems;
  private UserResponse user;

}