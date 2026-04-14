package com.appname.orderservice.mapper;

import com.appname.orderservice.dto.response.ItemResponse;
import com.appname.orderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ItemMapper {
  ItemResponse toResponse(Item item);

}
