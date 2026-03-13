package com.appname.userservice.mapper;

import com.appname.userservice.dto.request.CreateUserRequest;
import com.appname.userservice.dto.request.UpdateUserRequest;
import com.appname.userservice.dto.response.UserResponse;
import com.appname.userservice.entity.User;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = {PaymentCardMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
  User toEntity(CreateUserRequest request);

  UserResponse toResponse(User user);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);

}
