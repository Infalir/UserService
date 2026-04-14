package com.appname.gateway.service;

import com.appname.gateway.dto.request.RegisterRequest;
import com.appname.gateway.dto.response.RegisterResponse;
import com.appname.gateway.dto.response.UserResponse;
import com.appname.gateway.exception.RegistrationException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;

class RegistrationServiceTest {
  private MockWebServer userServiceMock;
  private MockWebServer authServiceMock;
  private RegistrationService registrationService;

  @BeforeEach
  void setUp() throws IOException {
    userServiceMock = new MockWebServer();
    authServiceMock = new MockWebServer();
    userServiceMock.start();
    authServiceMock.start();

    registrationService = new RegistrationService(WebClient.builder());
    ReflectionTestUtils.setField(registrationService, "userServiceUrl",
            userServiceMock.url("/").toString().replaceAll("/$", ""));
    ReflectionTestUtils.setField(registrationService, "authServiceUrl",
            authServiceMock.url("/").toString().replaceAll("/$", ""));
  }

  @AfterEach
  void tearDown() throws IOException {
    userServiceMock.shutdown();
    authServiceMock.shutdown();
  }

  private RegisterRequest buildRequest() {
    RegisterRequest req = new RegisterRequest();
    req.setName("John");
    req.setSurname("Doe");
    req.setEmail("john@example.com");
    req.setBirthDate(LocalDate.of(1990, 1, 1));
    req.setLogin("john");
    req.setPassword("password123");
    req.setRole("USER");
    return req;
  }

  @Test
  @DisplayName("register - success: creates user then saves credentials")
  void register_Success() {
    userServiceMock.enqueue(new MockResponse()
            .setResponseCode(201).setHeader("Content-Type", "application/json")
            .setBody("{\"id\":1,\"name\":\"John\",\"email\":\"john@example.com\"}"));

    authServiceMock.enqueue(new MockResponse().setResponseCode(201));

    StepVerifier.create(registrationService.register(buildRequest()))
            .assertNext(response -> {
              assert response.getUserId().equals(1L);
              assert response.getLogin().equals("john");
              assert response.getMessage().equals("Registration successful");
            }).verifyComplete();
  }

  @Test
  @DisplayName("register - rolls back user when Auth Service fails")
  void register_AuthServiceFails_RollsBackUser() {
    userServiceMock.enqueue(new MockResponse()
            .setResponseCode(201).setHeader("Content-Type", "application/json")
            .setBody("{\"id\":1,\"name\":\"John\",\"email\":\"john@example.com\"}"));

    authServiceMock.enqueue(new MockResponse().setResponseCode(500)
            .setBody("Internal Server Error"));

    userServiceMock.enqueue(new MockResponse().setResponseCode(200));

    StepVerifier.create(registrationService.register(buildRequest()))
            .expectErrorMatches(ex -> ex instanceof RegistrationException && ex.getMessage().contains("rolled back"))
            .verify();
  }

  @Test
  @DisplayName("register - fails with RegistrationException when User Service fails")
  void register_UserServiceFails_ReturnsError() {
    userServiceMock.enqueue(new MockResponse().setResponseCode(409)
            .setBody("Email already exists"));

    StepVerifier.create(registrationService.register(buildRequest()))
            .expectErrorMatches(ex -> ex instanceof RegistrationException && ex.getMessage().contains("User Service failed"))
            .verify();
  }

  @Test
  @DisplayName("register - proceeds even when rollback itself fails")
  void register_RollbackFails_DoesNotThrowRollbackError() {
    userServiceMock.enqueue(new MockResponse()
            .setResponseCode(201).setHeader("Content-Type", "application/json")
            .setBody("{\"id\":1,\"name\":\"John\",\"email\":\"john@example.com\"}"));

    authServiceMock.enqueue(new MockResponse().setResponseCode(500).setBody("Auth failed"));

    userServiceMock.enqueue(new MockResponse().setResponseCode(500));

    StepVerifier.create(registrationService.register(buildRequest()))
            .expectErrorMatches(ex -> ex instanceof RegistrationException && ex.getMessage().contains("rolled back"))
            .verify();
  }

}
