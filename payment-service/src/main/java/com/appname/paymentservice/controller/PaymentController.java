package com.appname.paymentservice.controller;

import com.appname.paymentservice.dto.request.CreatePaymentRequest;
import com.appname.paymentservice.dto.response.PaymentResponse;
import com.appname.paymentservice.dto.response.PaymentSumResponse;
import com.appname.paymentservice.entity.PaymentStatus;
import com.appname.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller exposing the Payment Service API.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a payment. Status determined by external random number API.
     * Publishes a CREATE_PAYMENT Kafka event on completion.
     *
     * @apiNote {@code POST /api/v1/payments}
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    /**
     * Returns payments filtered by userId, orderId, or status.
     * Provide exactly one query parameter.
     *
     * @apiNote {@code GET /api/v1/payments?userId=...}
     *          {@code GET /api/v1/payments?orderId=...}
     *          {@code GET /api/v1/payments?status=SUCCESS}
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPayments(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) PaymentStatus status) {
        return ResponseEntity.ok(paymentService.getPayments(userId, orderId, status));
    }

    /**
     * Returns total payment sum for the current user within a date range.
     *
     * @apiNote {@code GET /api/v1/payments/sum/user/{userId}?from=...&to=...}
     */
    @GetMapping("/sum/user/{userId}")
    public ResponseEntity<PaymentSumResponse> getUserPaymentSum(@PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getUserPaymentSum(userId, from, to));
    }

    /**
     * Returns total payment sum across all users within a date range.
     * Admin-only endpoint.
     *
     * @apiNote {@code GET /api/v1/payments/sum/all?from=...&to=...}
     */
    @GetMapping("/sum/all")
    public ResponseEntity<PaymentSumResponse> getAllUsersPaymentSum(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getAllUsersPaymentSum(from, to));
    }

}
