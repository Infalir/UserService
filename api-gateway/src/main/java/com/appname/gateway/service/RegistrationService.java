package com.appname.gateway.service;

import com.appname.gateway.dto.request.RegisterRequest;
import com.appname.gateway.dto.response.RegisterResponse;
import com.appname.gateway.dto.response.UserResponse;
import com.appname.gateway.exception.RegistrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Orchestrates the two-step user registration flow.
 *
 * <p>Step 1 — Create user in User Service (stores profile data).
 * Step 2 — Save credentials in Auth Service (stores hashed password and role).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

  private final WebClient.Builder webClientBuilder;

  @Value("${services.user-service.base-url}")
  private String userServiceUrl;

  @Value("${services.auth-service.base-url}")
  private String authServiceUrl;

  /**
   * Executes the full registration saga:
   * <ol>
   *   <li>POST to User Service — creates the user profile</li>
   *   <li>POST to Auth Service — saves hashed credentials</li>
   *   <li>On Auth Service failure — DELETE user from User Service (rollback)</li>
   * </ol>
   *
   * @param request the registration request containing both profile and credential data
   * @return a {@link RegisterResponse} with the created userId and login
   */
  public Mono<RegisterResponse> register(RegisterRequest request) {
    return createUserInUserService(request)
            .flatMap(user -> saveCredentialsInAuthService(request, user.getId())
                    .thenReturn(user)
                    .onErrorResume(ex -> {
                      log.error("Auth Service failed for userId: {}. Rolling back user creation.", user.getId());
                      return rollbackUserCreation(user.getId())
                              .then(Mono.error(new RegistrationException(
                                      "Registration failed: could not save credentials. " +
                                              "User creation has been rolled back. Reason: " +
                                              ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR)));
                    })
            )
            .map(user -> RegisterResponse.builder().userId(user.getId())
                    .login(request.getLogin()).message("Registration successful")
                    .build());
  }

  private Mono<UserResponse> createUserInUserService(RegisterRequest request) {
    Map<String, Object> userBody = Map.of(
            "name",      request.getName(),
            "surname",   request.getSurname(),
            "birthDate", request.getBirthDate().toString(),
            "email",     request.getEmail()
    );

    return webClientBuilder.baseUrl(userServiceUrl).build()
            .post().uri("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON).bodyValue(userBody)
            .retrieve()
            .onStatus(status -> status.isError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RegistrationException(
                                    "User Service failed: " + body, HttpStatus.valueOf(clientResponse.statusCode().value()))))
            )
            .bodyToMono(UserResponse.class)
            .doOnSuccess(u -> log.info("Created user id={} in User Service", u.getId()));
  }

  private Mono<Void> saveCredentialsInAuthService(RegisterRequest request, Long userId) {
    Map<String, Object> credBody = Map.of(
            "userId",   userId,
            "login",    request.getLogin(),
            "password", request.getPassword(),
            "role",     request.getRole() != null ? request.getRole() : "USER"
    );

    return webClientBuilder.baseUrl(authServiceUrl).build()
            .post().uri("/api/v1/auth/credentials")
            .contentType(MediaType.APPLICATION_JSON).bodyValue(credBody)
            .retrieve()
            .onStatus(status -> status.isError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RegistrationException(
                                    "Auth Service failed: " + body, HttpStatus.valueOf(clientResponse.statusCode().value()))))
            )
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.info("Saved credentials for userId={} in Auth Service", userId));
  }

  private Mono<Void> rollbackUserCreation(Long userId) {
    return webClientBuilder.baseUrl(userServiceUrl).build()
            .delete().uri("/api/v1/users/{id}", userId)
            .retrieve().bodyToMono(Void.class)
            .doOnSuccess(v -> log.info("Rollback: deleted userId={} from User Service", userId))
            .onErrorResume(ex -> {
              log.error("CRITICAL: Rollback failed for userId={}. Manual cleanup required. Cause: {}", userId, ex.getMessage());
              return Mono.empty();
            });
  }

}