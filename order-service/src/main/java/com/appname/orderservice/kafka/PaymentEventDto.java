package com.appname.orderservice.kafka;

import com.appname.orderservice.kafka.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDto {
    private String paymentId;
    private String orderId;
    private String userId;
    private PaymentStatus status;

}
