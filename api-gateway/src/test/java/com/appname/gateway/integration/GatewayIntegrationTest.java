package com.appname.gateway.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class GatewayIntegrationTest extends BaseIntegrationTest {
  @Autowired
  private WebTestClient webTestClient;

  @Value("${jwt.secret}")
  private String secret;

  @BeforeEach
  void resetMocks() {
    authServiceMock.resetAll();
    userServiceMock.resetAll();
    orderServiceMock.resetAll();
  }

  private String buildToken(long userId, String role, long expiryMs) {
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder().subject("testuser")
            .claims(Map.of("userId", userId, "role", role)).issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiryMs)).signWith(key)
            .compact();
  }

  @Test
  @DisplayName("Filter - returns 401 when Authorization header is missing")
  void filter_MissingToken_Returns401() {
    webTestClient.get().uri("/api/v1/users/1").exchange().expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("Filter - returns 401 for malformed Authorization header")
  void filter_MalformedHeader_Returns401() {
    webTestClient.get().uri("/api/v1/users/1")
            .header(HttpHeaders.AUTHORIZATION, "InvalidFormat token123")
            .exchange().expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("Filter - returns 401 for expired JWT token")
  void filter_ExpiredToken_Returns401() throws InterruptedException {
    String token = buildToken(1L, "ROLE_USER", 1L);
    Thread.sleep(50);

    webTestClient.get().uri("/api/v1/users/1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange().expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("Filter - forwards request with valid JWT and injects X-User headers")
  void filter_ValidToken_ForwardsRequest() {
    String token = buildToken(42L, "ROLE_USER", 60_000L);

    userServiceMock.stubFor(WireMock.get(urlPathEqualTo("/api/v1/users/42"))
            .withHeader("X-User-Id", equalTo("42"))
            .withHeader("X-User-Role", equalTo("ROLE_USER"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json").withBody("{\"id\":42,\"name\":\"John\"}")));

    webTestClient.get().uri("/api/v1/users/42")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange()
            .expectStatus().isOk();
  }

  @Test
  @DisplayName("Filter - login endpoint is public (no JWT required)")
  void filter_LoginEndpoint_IsPublic() {
    authServiceMock.stubFor(WireMock.post(urlPathEqualTo("/api/v1/auth/login"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                    .withBody("{\"accessToken\":\"token\",\"refreshToken\":\"refresh\"}")));

    webTestClient.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"login\":\"user\",\"password\":\"pass\"}").exchange()
            .expectStatus().isOk();
  }

  @Test
  @DisplayName("Filter - register endpoint is public (no JWT required)")
  void filter_RegisterEndpoint_IsPublic() {
    userServiceMock.stubFor(WireMock.post(urlPathEqualTo("/api/v1/users"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":1,\"name\":\"John\"}")));

    authServiceMock.stubFor(WireMock.post(urlPathEqualTo("/api/v1/auth/credentials"))
            .willReturn(aResponse().withStatus(201)));

    webTestClient.post().uri("/api/v1/gateway/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"name\":\"John\",\"surname\":\"Doe\",\"email\":\"j@e.com\"," +
                    "\"birthDate\":\"1990-01-01\",\"login\":\"john\",\"password\":\"pass123\"}")
            .exchange().expectStatus().isCreated();
  }

  @Test
  @DisplayName("Route - forwards /api/v1/auth/** to Auth Service")
  void route_ForwardsAuthRequests() {
    String token = buildToken(1L, "ROLE_USER", 60_000L);

    authServiceMock.stubFor(WireMock.post(urlPathEqualTo("/api/v1/auth/refresh"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"accessToken\":\"new\"}")));

    webTestClient.post().uri("/api/v1/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"refreshToken\":\"old\"}")
            .exchange().expectStatus().isOk();
  }

  @Test
  @DisplayName("Route - forwards /api/v1/orders/** to Order Service")
  void route_ForwardsOrderRequests() {
    String token = buildToken(1L, "ROLE_ADMIN", 60_000L);

    orderServiceMock.stubFor(WireMock.get(urlPathEqualTo("/api/v1/orders"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                    .withBody("{\"content\":[]}")));

    webTestClient.get().uri("/api/v1/orders")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange().expectStatus().isOk();
  }

  @Test
  @DisplayName("Register - creates user and credentials, returns 201")
  void register_Success_Returns201() {
    userServiceMock.stubFor(WireMock.post(urlPathEqualTo("/api/v1/users"))
            .willReturn(aResponse().withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":10,\"name\":\"John\",\"email\":\"j@e.com\"}")));

    authServiceMock.stubFor(WireMock.post(urlPathEqualTo("/api/v1/auth/credentials"))
            .willReturn(aResponse().withStatus(201)));

    webTestClient.post().uri("/api/v1/gateway/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"name\":\"John\",\"surname\":\"Doe\",\"email\":\"j@e.com\"," +
                    "\"birthDate\":\"1990-01-01\",\"login\":\"john\",\"password\":\"pass123\"}")
            .exchange().expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.userId").isEqualTo(10)
            .jsonPath("$.login").isEqualTo("john")
            .jsonPath("$.message").isEqualTo("Registration successful");
  }

  @Test
  @DisplayName("Register - rolls back user when Auth Service fails, returns 500")
  void register_AuthFails_RollsBackAndReturns500() {
    userServiceMock.stubFor(WireMock.post(urlPathEqualTo("/api/v1/users"))
            .willReturn(aResponse().withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":10,\"name\":\"John\",\"email\":\"j@e.com\"}")));

    authServiceMock.stubFor(WireMock.post(urlPathEqualTo("/api/v1/auth/credentials"))
            .willReturn(aResponse().withStatus(500).withBody("Auth error")));

    userServiceMock.stubFor(WireMock.delete(urlPathEqualTo("/api/v1/users/10"))
            .willReturn(aResponse().withStatus(200)));

    webTestClient.post().uri("/api/v1/gateway/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"name\":\"John\",\"surname\":\"Doe\",\"email\":\"j@e.com\"," +
                    "\"birthDate\":\"1990-01-01\",\"login\":\"john\",\"password\":\"pass123\"}")
            .exchange().expectStatus().is5xxServerError()
            .expectBody()
            .jsonPath("$.message").value(msg ->
                    org.assertj.core.api.Assertions.assertThat(msg.toString())
                            .contains("rolled back"));

    userServiceMock.verify(deleteRequestedFor(urlPathEqualTo("/api/v1/users/10")));
  }

  @Test
  @DisplayName("Register - returns error when User Service fails (no rollback needed)")
  void register_UserServiceFails_ReturnsError() {
    userServiceMock.stubFor(WireMock.post(urlPathEqualTo("/api/v1/users"))
            .willReturn(aResponse().withStatus(409).withBody("Email taken")));

    webTestClient.post().uri("/api/v1/gateway/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"name\":\"John\",\"surname\":\"Doe\",\"email\":\"j@e.com\"," +
                    "\"birthDate\":\"1990-01-01\",\"login\":\"john\",\"password\":\"pass123\"}")
            .exchange().expectStatus().is4xxClientError();

    authServiceMock.verify(0, postRequestedFor(urlPathEqualTo("/api/v1/auth/credentials")));
  }

}
