package com.appname.paymentservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@CompoundIndex(name = "idx_user_timestamp", def = "{'user_id': 1, 'timestamp': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    private String id;

    @Field("order_id")
    @Indexed
    private String orderId;

    @Field("user_id")
    @Indexed
    private String userId;

    @Field("status")
    @Indexed
    private PaymentStatus status;

    @Field("timestamp")
    @Indexed
    private LocalDateTime timestamp;

    @Field("payment_amount")
    private BigDecimal paymentAmount;

}
