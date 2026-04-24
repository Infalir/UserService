package com.appname.paymentservice.service.impl;

import com.appname.paymentservice.client.RandomNumberClient;
import com.appname.paymentservice.dto.request.CreatePaymentRequest;
import com.appname.paymentservice.dto.response.PaymentResponse;
import com.appname.paymentservice.dto.response.PaymentSumResponse;
import com.appname.paymentservice.entity.Payment;
import com.appname.paymentservice.entity.PaymentStatus;
import com.appname.paymentservice.kafka.PaymentEventDto;
import com.appname.paymentservice.kafka.PaymentKafkaProducer;
import com.appname.paymentservice.mapper.PaymentMapper;
import com.appname.paymentservice.repository.AggregationResult;
import com.appname.paymentservice.repository.PaymentRepository;
import com.appname.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final PaymentKafkaProducer kafkaProducer;

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        PaymentStatus status = randomNumberClient.determinePaymentStatus();

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .status(status)
                .timestamp(LocalDateTime.now())
                .paymentAmount(request.getPaymentAmount())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Created payment id={} orderId={} status={}", saved.getId(), saved.getOrderId(), saved.getStatus());

        PaymentEventDto event = PaymentEventDto.builder()
                .paymentId(saved.getId())
                .orderId(saved.getOrderId())
                .userId(saved.getUserId())
                .status(saved.getStatus())
                .build();

        kafkaProducer.sendPaymentEvent(event);

        return paymentMapper.toResponse(saved);
    }

    @Override
    public List<PaymentResponse> getPayments(String userId, String orderId, PaymentStatus status) {

        List<Payment> payments;

        if (userId != null) {
            payments = paymentRepository.findByUserId(userId);
        } else if (orderId != null) {
            payments = paymentRepository.findByOrderId(orderId);
        } else if (status != null) {
            payments = paymentRepository.findByStatus(status);
        } else {
            payments = paymentRepository.findAll();
        }

        return payments.stream().map(paymentMapper::toResponse).toList();
    }

    @Override
    public PaymentSumResponse getUserPaymentSum(String userId, LocalDateTime from, LocalDateTime to) {

        AggregationResult result = paymentRepository.sumPaymentAmountByUserIdAndTimestampBetween(userId, from, to);

        BigDecimal total = extractTotal(result);

        return PaymentSumResponse.builder()
                .total(total != null ? total : BigDecimal.ZERO)
                .from(from)
                .to(to)
                .userId(userId)
                .build();
    }

    @Override
    public PaymentSumResponse getAllUsersPaymentSum(LocalDateTime from, LocalDateTime to) {

        AggregationResult result = paymentRepository.sumPaymentAmountByTimestampBetween(from, to);

        BigDecimal total = extractTotal(result);

        return PaymentSumResponse.builder()
                .total(total != null ? total : BigDecimal.ZERO)
                .from(from)
                .to(to)
                .build();
    }

    private BigDecimal extractTotal(AggregationResult result) {
        if (result == null || result.getTotal() == null) {
            return BigDecimal.ZERO;
        }
        return result.getTotal().bigDecimalValue();
    }

}
