package com.appname.paymentservice.service;

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
import com.appname.paymentservice.service.impl.PaymentServiceImpl;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private RandomNumberClient randomNumberClient;
    @Mock private PaymentKafkaProducer kafkaProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;
    private PaymentResponse paymentResponse;
    private CreatePaymentRequest createRequest;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id("pay-001")
                .orderId("order-001")
                .userId("user-001")
                .status(PaymentStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("99.99"))
                .build();

        paymentResponse = new PaymentResponse();
        paymentResponse.setId("pay-001");
        paymentResponse.setOrderId("order-001");
        paymentResponse.setUserId("user-001");
        paymentResponse.setStatus(PaymentStatus.SUCCESS);
        paymentResponse.setPaymentAmount(new BigDecimal("99.99"));

        createRequest = new CreatePaymentRequest();
        createRequest.setOrderId("order-001");
        createRequest.setUserId("user-001");
        createRequest.setPaymentAmount(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("createPayment - SUCCESS: even random number → SUCCESS status")
    void createPayment_EvenNumber_SuccessStatus() {
        when(randomNumberClient.determinePaymentStatus()).thenReturn(PaymentStatus.SUCCESS);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        PaymentResponse result = paymentService.createPayment(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).save(any(Payment.class));
        verify(kafkaProducer).sendPaymentEvent(any(PaymentEventDto.class));
    }

    @Test
    @DisplayName("createPayment - FAILED: odd random number → FAILED status")
    void createPayment_OddNumber_FailedStatus() {
        payment.setStatus(PaymentStatus.FAILED);
        paymentResponse.setStatus(PaymentStatus.FAILED);

        when(randomNumberClient.determinePaymentStatus()).thenReturn(PaymentStatus.FAILED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        PaymentResponse result = paymentService.createPayment(createRequest);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("createPayment - Kafka event contains correct orderId as message key")
    void createPayment_PublishesKafkaEventWithCorrectOrderId() {
        when(randomNumberClient.determinePaymentStatus()).thenReturn(PaymentStatus.SUCCESS);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        paymentService.createPayment(createRequest);

        ArgumentCaptor<PaymentEventDto> captor = ArgumentCaptor.forClass(PaymentEventDto.class);
        verify(kafkaProducer).sendPaymentEvent(captor.capture());

        PaymentEventDto event = captor.getValue();
        assertThat(event.getOrderId()).isEqualTo("order-001");
        assertThat(event.getUserId()).isEqualTo("user-001");
        assertThat(event.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("createPayment - Kafka event published even when status is FAILED")
    void createPayment_AlwaysPublishesKafkaEvent() {
        when(randomNumberClient.determinePaymentStatus()).thenReturn(PaymentStatus.FAILED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        paymentService.createPayment(createRequest);

        verify(kafkaProducer, times(1)).sendPaymentEvent(any(PaymentEventDto.class));
    }

    @Test
    @DisplayName("getPayments - filters by userId when provided")
    void getPayments_ByUserId() {
        when(paymentRepository.findByUserId("user-001")).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        List<PaymentResponse> result = paymentService.getPayments("user-001", null, null);

        assertThat(result).hasSize(1);
        verify(paymentRepository).findByUserId("user-001");
        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("getPayments - filters by orderId when provided")
    void getPayments_ByOrderId() {
        when(paymentRepository.findByOrderId("order-001")).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        List<PaymentResponse> result = paymentService.getPayments(null, "order-001", null);

        assertThat(result).hasSize(1);
        verify(paymentRepository).findByOrderId("order-001");
    }

    @Test
    @DisplayName("getPayments - filters by status when provided")
    void getPayments_ByStatus() {
        when(paymentRepository.findByStatus(PaymentStatus.SUCCESS))
                .thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        List<PaymentResponse> result = paymentService.getPayments(
                null, null, PaymentStatus.SUCCESS);

        assertThat(result).hasSize(1);
        verify(paymentRepository).findByStatus(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("getPayments - returns all when no filter provided")
    void getPayments_NoFilter_ReturnsAll() {
        when(paymentRepository.findAll()).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        List<PaymentResponse> result = paymentService.getPayments(null, null, null);

        assertThat(result).hasSize(1);
        verify(paymentRepository).findAll();
    }

    @Test
    @DisplayName("getPayments - userId filter takes priority over orderId")
    void getPayments_UserIdPriorityOverOrderId() {
        when(paymentRepository.findByUserId("user-001")).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        paymentService.getPayments("user-001", "order-001", null);

        verify(paymentRepository).findByUserId("user-001");
        verify(paymentRepository, never()).findByOrderId(any());
    }

    @Test
    @DisplayName("getUserPaymentSum - returns correct total for date range")
    void getUserPaymentSum_ReturnsTotal() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to   = LocalDateTime.now();

        AggregationResult mockResult = new AggregationResult();
        mockResult.setTotal(new Decimal128( new BigDecimal("199.98")));

        when(paymentRepository.sumPaymentAmountByUserIdAndTimestampBetween(
                "user-001", from, to)).thenReturn(mockResult);

        PaymentSumResponse result = paymentService.getUserPaymentSum("user-001", from, to);

        assertThat(result.getTotal()).isEqualByComparingTo("199.98");
        assertThat(result.getUserId()).isEqualTo("user-001");
        assertThat(result.getFrom()).isEqualTo(from);
        assertThat(result.getTo()).isEqualTo(to);
    }

    @Test
    @DisplayName("getUserPaymentSum - returns zero when no payments in range")
    void getUserPaymentSum_NoPayments_ReturnsZero() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to   = LocalDateTime.now();

        when(paymentRepository.sumPaymentAmountByUserIdAndTimestampBetween(
                "user-001", from, to)).thenReturn(null);

        PaymentSumResponse result = paymentService.getUserPaymentSum("user-001", from, to);

        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getAllUsersPaymentSum - returns total across all users")
    void getAllUsersPaymentSum_ReturnsTotal() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to   = LocalDateTime.now();

        AggregationResult mockResult = new AggregationResult();
        mockResult.setTotal(new Decimal128(new BigDecimal("5000.00")));

        when(paymentRepository.sumPaymentAmountByTimestampBetween(from, to))
                .thenReturn(mockResult);

        PaymentSumResponse result = paymentService.getAllUsersPaymentSum(from, to);

        assertThat(result.getTotal()).isEqualByComparingTo("5000.00");
        assertThat(result.getUserId()).isNull(); // null for admin endpoint
    }

    @Test
    @DisplayName("getAllUsersPaymentSum - returns zero when no payments in range")
    void getAllUsersPaymentSum_NoPayments_ReturnsZero() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to   = LocalDateTime.now();

        when(paymentRepository.sumPaymentAmountByTimestampBetween(from, to))
                .thenReturn(null);

        PaymentSumResponse result = paymentService.getAllUsersPaymentSum(from, to);

        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

}
