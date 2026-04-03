package com.appname.orderservice.client;

import com.appname.orderservice.dto.response.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

/**
 * HTTP client for communicating with User Service.
 *
 * <p>Uses WebClient for non-blocking REST calls and Resilience4j circuit breaker
 * to handle User Service unavailability gracefully. When the circuit is open
 * (User Service is down or slow), the fallback returns an empty Optional so
 * order endpoints can still respond with partial data rather than failing entirely.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient webClient;

    private static final String CIRCUIT_BREAKER_NAME = "userService";

    /**
     * Fetches a user by their ID from User Service.
     *
     * <p>Returns {@code Optional.empty()} gracefully for both 4xx responses
     * (user not found) and circuit-open fallback (User Service unavailable).
     * Only 5xx responses and network errors are counted as circuit breaker failures.</p>
     *
     * @param userId the user's ID
     * @return an Optional containing the UserResponse, or empty if not found
     *         or User Service is unavailable
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserByIdFallback")
    public Optional<UserResponse> getUserById(Long userId) {
        log.debug("Calling User Service for userId: {}", userId);
        UserResponse response = webClient.get().uri("/api/v1/users/{id}", userId)
                .retrieve().onStatus(status -> status.is4xxClientError(),
                        clientResponse -> {
                            log.warn("User Service returned {} for userId {}", clientResponse.statusCode(), userId);
                            return clientResponse.releaseBody().then(reactor.core.publisher.Mono.empty());
                        }).bodyToMono(UserResponse.class).block();
        return Optional.ofNullable(response);
    }

    /**
     * Fetches a user by their email address from User Service.
     *
     * @param email the user's email address
     * @return an Optional containing the UserResponse, or empty if not found
     *         or User Service is unavailable
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserFallback")
    public Optional<UserResponse> getUserByEmail(String email) {
        log.debug("Calling User Service for email: {}", email);
        UserResponse response = webClient.get().uri("/api/v1/users/email/{email}", email)
                .retrieve().onStatus(status -> status.is4xxClientError(), clientResponse -> clientResponse.releaseBody()
                                .then(reactor.core.publisher.Mono.empty())).bodyToMono(UserResponse.class).block();
        return Optional.ofNullable(response);
    }

    private Optional<UserResponse> getUserByIdFallback(Long userId, Throwable t) {
        log.warn("User Service unavailable for userId: {}. Cause: {}", userId, t.getMessage());
        return Optional.empty();
    }

    private Optional<UserResponse> getUserFallback(String email, Throwable t) {
        log.warn("User Service unavailable for email: {}. Cause: {}", email, t.getMessage());
        return Optional.empty();
    }

}
