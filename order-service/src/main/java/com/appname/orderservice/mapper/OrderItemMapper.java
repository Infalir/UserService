package com.appname.orderservice.mapper;

import com.appname.orderservice.dto.response.OrderItemResponse;
import com.appname.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {ItemMapper.class}, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrderItemMapper {
  OrderItemResponse toResponse(OrderItem orderItem);

}