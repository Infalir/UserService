package com.appname.gateway.controller;

import com.appname.gateway.dto.request.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Thin proxy controller for the registration endpoint.
 *
 * @apiNote {@code POST /api/v1/gateway/register} → forwarded to auth-service
 *          {@code POST /api/v1/auth/register}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class RegistrationController {

  private final WebClient.Builder webClientBuilder;

  @Value("${services.auth-service.base-url}")
  private String authServiceUrl;

  /**
   * Forwards the registration request to auth-service.
   */
  @PostMapping("/register")
  public Mono<ResponseEntity<Object>> register(@RequestBody RegisterRequest request) {
    log.debug("Forwarding registration request to auth-service");
    return webClientBuilder.baseUrl(authServiceUrl).build().post()
            .uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request).exchangeToMono(response -> response.toEntity(Object.class));
  }

}