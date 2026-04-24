package com.appname.authservice.controller;

import com.appname.authservice.dto.request.RegisterRequest;
import com.appname.authservice.dto.response.RegisterResponse;
import com.appname.authservice.orchestrator.RegistrationOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Handles the user registration saga in auth-service.
 *
 * @apiNote {@code POST /api/v1/auth/register}
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RegistrationController {
  private final RegistrationOrchestrator registrationOrchestrator;

  /**
   * Registers a new user by orchestrating the two-step saga:
   * creates user profile in User Service, then saves credentials here.
   *
   * @param request registration payload with user profile and credential data
   * @return {@code 201 Created} with userId, login, and success message
   */
  @PostMapping("/register")
  public Mono<ResponseEntity<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
    return registrationOrchestrator.register(request).map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

}

