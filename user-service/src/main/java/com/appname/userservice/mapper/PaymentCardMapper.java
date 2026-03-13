package com.appname.userservice.mapper;

import com.appname.userservice.dto.request.CreatePaymentCardRequest;
import com.appname.userservice.dto.request.UpdatePaymentCardRequest;
import com.appname.userservice.dto.response.PaymentCardResponse;
import com.appname.userservice.entity.PaymentCard;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PaymentCardMapper {
  PaymentCard toEntity(CreatePaymentCardRequest request);

  @Mapping(source = "user.id", target = "userId")
  PaymentCardResponse toResponse(PaymentCard card);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateEntityFromRequest(UpdatePaymentCardRequest request, @MappingTarget PaymentCard card);

}
