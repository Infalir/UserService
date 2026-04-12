package com.appname.orderservice.client;

import com.appname.orderservice.dto.response.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HTTP client for communicating with User Service.
 *
 * <p>Uses WebClient for REST calls protected by a Resilience4j circuit breaker.
 * When the circuit is open, fallback methods return empty results so order
 * endpoints can still respond with partial data.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient webClient;

    private static final String CIRCUIT_BREAKER_NAME = "userService";

    /**
     * Fetches a single user by ID from User Service.
     * Used for single-order endpoints (create, getById, update).
     *
     * @param userId the user's ID
     * @return Optional containing the user, or empty if not found or unavailable
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserByIdFallback")
    public Optional<UserResponse> getUserById(Long userId) {
        log.debug("Calling User Service for userId: {}", userId);
        UserResponse response = webClient.get()
                .uri("/api/v1/users/{id}", userId).retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        clientResponse -> clientResponse.releaseBody()
                                .then(reactor.core.publisher.Mono.empty()))
                .bodyToMono(UserResponse.class).block();
        return Optional.ofNullable(response);
    }

    /**
     * Fetches multiple users by their IDs in a single HTTP request.
     *
     * <p>This is the batch variant used by {@code getAllOrders} to avoid
     * making one HTTP call per unique userId on the page. A single GET request
     * with all IDs as query parameters returns all matching users at once.</p>
     *
     * @param userIds the set of user IDs to fetch
     * @return a map of userId to UserResponse for all found users;
     *         empty map if User Service is unavailable
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUsersByIdsFallback")
    public Map<Long, UserResponse> getUsersByIds(java.util.Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String ids = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        log.debug("Batch-fetching users for ids: {}", ids);

        List<UserResponse> users = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/users/batch").queryParam("ids", ids).build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        clientResponse -> clientResponse.releaseBody().then(reactor.core.publisher.Mono.empty()))
                .bodyToFlux(UserResponse.class).collectList().block();

        if (users == null) {
            return Collections.emptyMap();
        }

        return users.stream().collect(Collectors.toMap(UserResponse::getId, Function.identity()));
    }

    /**
     * Fetches a user by their email address from User Service.
     *
     * @param email the user's email address
     * @return Optional containing the user, or empty if not found or unavailable
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserFallback")
    public Optional<UserResponse> getUserByEmail(String email) {
        log.debug("Calling User Service for email: {}", email);
        UserResponse response = webClient.get()
                .uri("/api/v1/users/email/{email}", email).retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        clientResponse -> clientResponse.releaseBody().then(reactor.core.publisher.Mono.empty()))
                .bodyToMono(UserResponse.class).block();
        return Optional.ofNullable(response);
    }

    private Optional<UserResponse> getUserByIdFallback(Long userId, Throwable t) {
        log.warn("User Service unavailable for userId: {}. Cause: {}", userId, t.getMessage());
        return Optional.empty();
    }

    private Map<Long, UserResponse> getUsersByIdsFallback(java.util.Set<Long> userIds, Throwable t) {
        log.warn("User Service batch call unavailable for ids: {}. Cause: {}", userIds, t.getMessage());
        return Collections.emptyMap();
    }

    private Optional<UserResponse> getUserFallback(String email, Throwable t) {
        log.warn("User Service unavailable for email: {}. Cause: {}", email, t.getMessage());
        return Optional.empty();
    }
}
