package com.appname.paymentservice.mapper;

import com.appname.paymentservice.dto.response.PaymentResponse;
import com.appname.paymentservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PaymentMapper {
  PaymentResponse toResponse(Payment payment);

}
