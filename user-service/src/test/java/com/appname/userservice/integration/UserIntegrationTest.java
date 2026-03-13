package com.appname.userservice.integration;

import com.appname.userservice.dto.request.CreatePaymentCardRequest;
import com.appname.userservice.dto.request.CreateUserRequest;
import com.appname.userservice.dto.request.UpdateUserRequest;
import com.appname.userservice.dto.response.PaymentCardResponse;
import com.appname.userservice.dto.response.UserResponse;
import com.appname.userservice.repository.PaymentCardRepository;
import com.appname.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserIntegrationTest extends BaseIntegrationTest {
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PaymentCardRepository cardRepository;
  @Autowired private ObjectMapper objectMapper;

  private static final String USERS_URL = "/api/v1/users";

  @BeforeEach
  void cleanUp() {
    cardRepository.deleteAll();
    userRepository.deleteAll();
  }

  private CreateUserRequest buildCreateRequest(String email) {
    CreateUserRequest req = new CreateUserRequest();
    req.setName("John");
    req.setSurname("Doe");
    req.setEmail(email);
    req.setBirthDate(LocalDate.of(1990, 5, 15));
    return req;
  }

  @Test
  @DisplayName("POST /api/v1/users - creates user successfully")
  void createUser_Returns201() {
    CreateUserRequest request = buildCreateRequest("integration@test.com");

    ResponseEntity<UserResponse> response = restTemplate.postForEntity(USERS_URL, request, UserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getEmail()).isEqualTo("integration@test.com");
    assertThat(response.getBody().getId()).isNotNull();
  }

  @Test
  @DisplayName("POST /api/v1/users - returns 409 on duplicate email")
  void createUser_DuplicateEmail_Returns409() {
    CreateUserRequest request = buildCreateRequest("duplicate@test.com");
    restTemplate.postForEntity(USERS_URL, request, UserResponse.class);

    ResponseEntity<String> response = restTemplate.postForEntity(USERS_URL, request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  @DisplayName("GET /api/v1/users/{id} - returns user")
  void getUserById_Returns200() {
    CreateUserRequest request = buildCreateRequest("get@test.com");
    UserResponse created = restTemplate.postForEntity(USERS_URL, request, UserResponse.class).getBody();

    ResponseEntity<UserResponse> response = restTemplate.getForEntity(USERS_URL + "/" + created.getId(), UserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getId()).isEqualTo(created.getId());
  }

  @Test
  @DisplayName("GET /api/v1/users/{id} - returns 404 when not found")
  void getUserById_NotFound_Returns404() {
    ResponseEntity<String> response = restTemplate.getForEntity(USERS_URL + "/999999", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("PUT /api/v1/users/{id} - updates user")
  void updateUser_Returns200() {
    UserResponse created = restTemplate.postForEntity(USERS_URL, buildCreateRequest("update@test.com"), UserResponse.class).getBody();

    UpdateUserRequest updateRequest = new UpdateUserRequest();
    updateRequest.setName("UpdatedName");

    HttpEntity<UpdateUserRequest> entity = new HttpEntity<>(updateRequest);
    ResponseEntity<UserResponse> response = restTemplate.exchange(USERS_URL + "/" + created.getId(), HttpMethod.PUT, entity, UserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getName()).isEqualTo("UpdatedName");
  }

  @Test
  @DisplayName("PATCH /api/v1/users/{id}/deactivate - deactivates user")
  void deactivateUser_Returns204() {
    UserResponse created = restTemplate.postForEntity(USERS_URL, buildCreateRequest("deactivate@test.com"), UserResponse.class).getBody();

    ResponseEntity<Void> response = restTemplate.exchange(USERS_URL + "/" + created.getId() + "/deactivate",
            HttpMethod.PATCH, HttpEntity.EMPTY, Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  @DisplayName("POST /api/v1/users/{userId}/cards - creates card for user")
  void createCard_Returns201() {
    UserResponse user = restTemplate.postForEntity(USERS_URL, buildCreateRequest("cardowner@test.com"), UserResponse.class).getBody();

    CreatePaymentCardRequest cardRequest = new CreatePaymentCardRequest();
    cardRequest.setNumber("1234567890123456");
    cardRequest.setHolder("JOHN DOE");
    cardRequest.setExpirationDate(LocalDate.of(2028, 12, 31));

    ResponseEntity<PaymentCardResponse> response = restTemplate.postForEntity(USERS_URL + "/" + user.getId() + "/cards", cardRequest, PaymentCardResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getNumber()).isEqualTo("1234567890123456");
  }

  @Test
  @DisplayName("POST /api/v1/users/{userId}/cards - returns 422 when card limit exceeded")
  void createCard_LimitExceeded_Returns422() {
    UserResponse user = restTemplate.postForEntity(USERS_URL, buildCreateRequest("limit@test.com"), UserResponse.class).getBody();

    for (int i = 0; i < 5; i++) {
      CreatePaymentCardRequest cardRequest = new CreatePaymentCardRequest();
      cardRequest.setNumber("123456789012345" + i);
      cardRequest.setHolder("JOHN DOE");
      cardRequest.setExpirationDate(LocalDate.of(2028, 12, 31));
      restTemplate.postForEntity(
              USERS_URL + "/" + user.getId() + "/cards", cardRequest, PaymentCardResponse.class);
    }

    CreatePaymentCardRequest sixthCard = new CreatePaymentCardRequest();
    sixthCard.setNumber("6666666666666666");
    sixthCard.setHolder("JOHN DOE");
    sixthCard.setExpirationDate(LocalDate.of(2028, 12, 31));

    ResponseEntity<String> response = restTemplate.postForEntity(USERS_URL + "/" + user.getId() + "/cards", sixthCard, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @Test
  @DisplayName("DELETE /api/v1/users/{id} - deletes user and cascades to cards")
  void deleteUser_CascadesCards_Returns204() {
    UserResponse user = restTemplate.postForEntity(USERS_URL, buildCreateRequest("cascade@test.com"), UserResponse.class).getBody();

    CreatePaymentCardRequest cardRequest = new CreatePaymentCardRequest();
    cardRequest.setNumber("9999888877776666");
    cardRequest.setHolder("CASCADE TEST");
    cardRequest.setExpirationDate(LocalDate.of(2028, 12, 31));
    restTemplate.postForEntity(USERS_URL + "/" + user.getId() + "/cards", cardRequest, PaymentCardResponse.class);

    ResponseEntity<Void> deleteResponse = restTemplate.exchange(USERS_URL + "/" + user.getId(), HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(cardRepository.findByUserId(user.getId())).isEmpty();
  }

}
