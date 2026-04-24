package com.appname.authservice.orchestrator;

import com.appname.authservice.dto.request.RegisterRequest;
import com.appname.authservice.dto.request.SaveCredentialsRequest;
import com.appname.authservice.dto.request.UserCreateRequest;
import com.appname.authservice.dto.response.RegisterResponse;
import com.appname.authservice.dto.response.UserServiceResponse;
import com.appname.authservice.exception.RegistrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Orchestrates the two-step user registration saga.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationOrchestrator {

  private final WebClient.Builder webClientBuilder;
  private final com.appname.authservice.service.AuthService authService;

  @Value("${services.user-service.base-url}")
  private String userServiceUrl;

  /**
   * Executes the full registration saga reactively.
   *
   * @param request the registration payload with profile and credential data
   * @return a {@link RegisterResponse} with userId, login, and success message
   */
  public Mono<RegisterResponse> register(RegisterRequest request) {
    return createUserInUserService(request)
            .flatMap(user -> saveCredentials(request, user.getId())
                            .thenReturn(user)
                            .onErrorResume(ex -> {
                              log.error("Credential save failed for userId: {}. Rolling back.", user.getId());
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

  private Mono<UserServiceResponse> createUserInUserService(RegisterRequest request) {
    UserCreateRequest userCreateRequest = UserCreateRequest.builder().name(request.getName())
            .surname(request.getSurname()).birthDate(request.getBirthDate())
            .email(request.getEmail()).build();

    return webClientBuilder.baseUrl(userServiceUrl).build()
            .post().uri("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON).bodyValue(userCreateRequest)
            .retrieve()
            .onStatus(status -> status.isError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RegistrationException(
                                    "User Service failed: " + body, HttpStatus.valueOf(clientResponse.statusCode().value()))))
            )
            .bodyToMono(UserServiceResponse.class)
            .doOnSuccess(u -> log.info("Created user id={} in User Service", u.getId()));
  }

  private Mono<Void> saveCredentials(RegisterRequest request, Long userId) {
    SaveCredentialsRequest credRequest = new SaveCredentialsRequest();
    credRequest.setUserId(userId);
    credRequest.setLogin(request.getLogin());
    credRequest.setPassword(request.getPassword());
    credRequest.setRole(request.getRole());

    return Mono.fromRunnable(() -> authService.saveCredentials(credRequest))
            .doOnSuccess(v -> log.info("Saved credentials for userId={}", userId)).then();
  }

  private Mono<Void> rollbackUserCreation(Long userId) {
    return webClientBuilder.baseUrl(userServiceUrl).build()
            .delete().uri("/api/v1/users/{id}", userId)
            .retrieve().bodyToMono(Void.class)
            .doOnSuccess(v -> log.info("Rollback: deleted userId={}", userId))
            .onErrorResume(ex -> {
              log.error("CRITICAL: Rollback failed for userId={}. Manual cleanup required. Cause: {}", userId, ex.getMessage());
              return Mono.empty();
            });
  }

}
