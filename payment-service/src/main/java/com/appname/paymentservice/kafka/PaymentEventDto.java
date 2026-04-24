package com.appname.paymentservice.kafka;

import com.appname.paymentservice.entity.PaymentStatus;
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
