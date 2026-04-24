package com.appname.paymentservice.client;

import com.appname.paymentservice.entity.PaymentStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Client for the external random number API used to determine payment status.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RandomNumberClient {
    private final WebClient webClient;

    @Value("${external.random-api.url}")
    private String randomApiUrl;

    private static final String CIRCUIT_BREAKER_NAME = "randomApi";

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackStatus")
    public PaymentStatus determinePaymentStatus() {
        String response = webClient.get().uri(randomApiUrl)
                .retrieve().bodyToMono(String.class).block();

        int number = Integer.parseInt(response.trim());
        log.debug("Random number from API: {}", number);
        PaymentStatus status = (number % 2 == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        log.info("Payment status determined: {} (number={})", status, number);
        return status;
    }

    private PaymentStatus fallbackStatus(Throwable t) {
        log.warn("Random API unavailable, falling back to FAILED. Cause: {}", t.getMessage());
        return PaymentStatus.FAILED;
    }
}
