package com.appname.orderservice.dto.response;

import lombok.Data;

@Data
public class OrderItemResponse {
  private Long id;
  private ItemResponse item;
  private Integer quantity;

}
