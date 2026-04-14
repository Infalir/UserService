package com.appname.gateway.controller;

import com.appname.gateway.dto.request.RegisterRequest;
import com.appname.gateway.dto.response.RegisterResponse;
import com.appname.gateway.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Handles the custom user registration flow at the gateway level.
 *
 * @apiNote {@code POST /api/v1/gateway/register}
 */
@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class RegistrationController {
  private final RegistrationService registrationService;

  /**
   * Registers a new user by creating their profile and credentials atomically.
   *
   * @param request registration payload with user profile data and login/password
   * @return {@code 201 Created} with userId, login, and success message
   */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<RegisterResponse> register(@RequestBody RegisterRequest request) {
    return registrationService.register(request);
  }

}
