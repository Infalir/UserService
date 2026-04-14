package com.appname.gateway.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
  static final WireMockServer authServiceMock;
  static final WireMockServer userServiceMock;
  static final WireMockServer orderServiceMock;

  static {
    authServiceMock  = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    userServiceMock  = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    orderServiceMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    authServiceMock.start();
    userServiceMock.start();
    orderServiceMock.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("services.auth-service.base-url",
            () -> "http://localhost:" + authServiceMock.port());
    registry.add("services.user-service.base-url",
            () -> "http://localhost:" + userServiceMock.port());
    registry.add("services.order-service.base-url",
            () -> "http://localhost:" + orderServiceMock.port());
    registry.add("jwt.secret",
            () -> "test-secret-key-that-is-at-least-64-characters-long-for-hmac-sha256!!");
  }

}
