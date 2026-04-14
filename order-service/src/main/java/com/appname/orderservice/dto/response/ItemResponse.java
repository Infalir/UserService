package com.appname.orderservice.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ItemResponse {
  private Long id;
  private String name;
  private BigDecimal price;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

}
