package com.appname.paymentservice.repository;

import com.appname.paymentservice.entity.Payment;
import com.appname.paymentservice.entity.PaymentStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByUserId(String userId);
    List<Payment> findByOrderId(String orderId);
    List<Payment> findByStatus(PaymentStatus status);

    @Aggregation(pipeline = {
        "{ $match: { user_id: ?0, timestamp: { $gte: ?1, $lte: ?2 } } }",
        "{ $group: { _id: null, total:{ $sum: { $toDecimal: '$payment_amount' } } } }"
    })
    AggregationResult sumPaymentAmountByUserIdAndTimestampBetween(String userId, LocalDateTime from, LocalDateTime to);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0, $lte: ?1 } } }",
        "{ $group: { _id: null, total: { $sum: { $toDecimal: '$payment_amount' } } } }"
    })
    AggregationResult sumPaymentAmountByTimestampBetween(LocalDateTime from, LocalDateTime to);

}
