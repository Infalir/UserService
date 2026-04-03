package com.appname.orderservice.mapper;

import com.appname.orderservice.dto.response.OrderResponse;
import com.appname.orderservice.dto.response.UserResponse;
import com.appname.orderservice.entity.Order;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class}, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrderMapper {
  OrderResponse toResponse(Order order);

  @AfterMapping
  default void setUser(@MappingTarget OrderResponse response, UserResponse user) {
    if (user != null) {
      response.setUser(user);
    }
  }

}
