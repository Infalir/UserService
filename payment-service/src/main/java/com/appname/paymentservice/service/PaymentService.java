package com.appname.paymentservice.service;

import com.appname.paymentservice.dto.request.CreatePaymentRequest;
import com.appname.paymentservice.dto.response.PaymentResponse;
import com.appname.paymentservice.dto.response.PaymentSumResponse;
import com.appname.paymentservice.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface defining the Payment Service business contract.
 */
public interface PaymentService {

    /**
     * Creates a payment, determines its status via the random number API,
     * and publishes a CREATE_PAYMENT Kafka event.
     *
     * @param request the payment creation request
     * @return the created payment with its final status
     */
    PaymentResponse createPayment(CreatePaymentRequest request);

    /**
     * Returns payments filtered by userId, orderId, or status.
     * Exactly one filter parameter should be provided.
     *
     * @param userId  filter by user ID (optional)
     * @param orderId filter by order ID (optional)
     * @param status  filter by payment status (optional)
     * @return list of matching payments
     */
    List<PaymentResponse> getPayments(String userId, String orderId, PaymentStatus status);

    /**
     * Returns the total payment amount for the current user within a date range.
     *
     * @param userId the user's ID
     * @param from   start of date range (inclusive)
     * @param to     end of date range (inclusive)
     * @return total sum and range details
     */
    PaymentSumResponse getUserPaymentSum(String userId, LocalDateTime from, LocalDateTime to);

    /**
     * Returns the total payment amount across all users within a date range.
     * Admin-only endpoint.
     *
     * @param from start of date range (inclusive)
     * @param to   end of date range (inclusive)
     * @return total sum and range details
     */
    PaymentSumResponse getAllUsersPaymentSum(LocalDateTime from, LocalDateTime to);

}
