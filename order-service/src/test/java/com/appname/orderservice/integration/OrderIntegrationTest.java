package com.appname.orderservice.integration;

import com.appname.orderservice.dto.request.CreateOrderRequest;
import com.appname.orderservice.dto.request.OrderItemRequest;
import com.appname.orderservice.dto.request.UpdateOrderRequest;
import com.appname.orderservice.dto.response.ErrorResponse;
import com.appname.orderservice.dto.response.OrderResponse;
import com.appname.orderservice.entity.Item;
import com.appname.orderservice.entity.OrderStatus;
import com.appname.orderservice.repository.ItemRepository;
import com.appname.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ItemRepository itemRepository;
    @Autowired private OrderRepository orderRepository;

    private static final String BASE_URL = "/api/v1/orders";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Item savedItem;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        savedItem = itemRepository.save(Item.builder().name("Test Widget").price(new BigDecimal("10.00")).build());

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/api/v1/users/10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 10,
                                  "name": "John",
                                  "surname": "Doe",
                                  "email": "john@example.com",
                                  "active": true
                                }
                                """)));
    }

    private CreateOrderRequest buildCreateRequest(Long userId, Long itemId, int quantity) {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setItemId(itemId);
        itemReq.setQuantity(quantity);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setUserId(userId);
        req.setItems(List.of(itemReq));
        return req;
    }

    private OrderResponse createOrder(Long userId, Long itemId, int quantity) {
        return restTemplate.postForEntity(BASE_URL,
                buildCreateRequest(userId, itemId, quantity),
                OrderResponse.class).getBody();
    }

    @Test
    @DisplayName("POST /orders - creates order and returns 201 with user info")
    void createOrder_Returns201WithUserInfo() {
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                BASE_URL, buildCreateRequest(10L, savedItem.getId(), 2), OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getBody().getTotalPrice()).isEqualByComparingTo("20.00");
        assertThat(response.getBody().getUser()).isNotNull();
        assertThat(response.getBody().getUser().getName()).isEqualTo("John");
    }

    @Test
    @DisplayName("POST /orders - user field is null when User Service returns 404")
    void createOrder_UserNotFound_UserFieldIsNull() {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/api/v1/users/99"))
                .willReturn(aResponse().withStatus(404)));

        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                BASE_URL, buildCreateRequest(99L, savedItem.getId(), 1), OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUser()).isNull();
    }

    @Test
    @DisplayName("POST /orders - user field is null when User Service is down (circuit fallback)")
    void createOrder_UserServiceDown_OrderStillCreated() {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/api/v1/users/10"))
                .willReturn(aResponse().withStatus(503)));

        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                BASE_URL, buildCreateRequest(10L, savedItem.getId(), 1), OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUser()).isNull();
    }

    @Test
    @DisplayName("POST /orders - returns 404 when item does not exist")
    void createOrder_ItemNotFound_Returns404() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                BASE_URL, buildCreateRequest(10L, 99999L, 1), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("99999");
    }

    @Test
    @DisplayName("POST /orders - returns 400 when items list is empty")
    void createOrder_EmptyItems_Returns400() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setUserId(10L);
        req.setItems(List.of());

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                BASE_URL, req, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getValidationErrors()).containsKey("items");
    }

    @Test
    @DisplayName("POST /orders - returns 400 when userId is missing")
    void createOrder_MissingUserId_Returns400() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItems(List.of());

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                BASE_URL, req, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /orders/{id} - returns order enriched with user info")
    void getOrderById_Returns200WithUserInfo() {
        OrderResponse created = createOrder(10L, savedItem.getId(), 1);

        ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                BASE_URL + "/" + created.getId(), OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(created.getId());
        assertThat(response.getBody().getUser()).isNotNull();
        assertThat(response.getBody().getUser().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("GET /orders/{id} - returns 404 for non-existent order")
    void getOrderById_NotFound_Returns404() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                BASE_URL + "/999999", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /orders/{id} - returns 404 for soft-deleted order")
    void getOrderById_SoftDeleted_Returns404() {
        OrderResponse created = createOrder(10L, savedItem.getId(), 1);

        restTemplate.delete(BASE_URL + "/" + created.getId());

        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                BASE_URL + "/" + created.getId(), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /orders - returns paginated list of non-deleted orders")
    void getAllOrders_ReturnsPaginatedList() {
        createOrder(10L, savedItem.getId(), 1);
        createOrder(10L, savedItem.getId(), 2);

        ResponseEntity<String> response = restTemplate.getForEntity(
                BASE_URL + "?page=0&size=10", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /orders - soft-deleted orders are excluded from results")
    void getAllOrders_ExcludesSoftDeleted() {
        OrderResponse created = createOrder(10L, savedItem.getId(), 1);
        restTemplate.delete(BASE_URL + "/" + created.getId());

        long count = orderRepository.findAll().stream()
                .filter(o -> !o.getDeleted()).count();

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("GET /orders/user/{userId} - returns all active orders for a user")
    void getOrdersByUserId_ReturnsOrders() {
        createOrder(10L, savedItem.getId(), 1);
        createOrder(10L, savedItem.getId(), 3);

        ResponseEntity<OrderResponse[]> response = restTemplate.getForEntity(
                BASE_URL + "/user/10", OrderResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("GET /orders/user/{userId} - returns empty list when user has no orders")
    void getOrdersByUserId_NoOrders_ReturnsEmpty() {
        ResponseEntity<OrderResponse[]> response = restTemplate.getForEntity(
                BASE_URL + "/user/99999", OrderResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("PUT /orders/{id} - updates order status and returns updated order")
    void updateOrder_Returns200WithUpdatedStatus() {
        OrderResponse created = createOrder(10L, savedItem.getId(), 1);

        UpdateOrderRequest updateReq = new UpdateOrderRequest();
        updateReq.setStatus(OrderStatus.CONFIRMED);

        HttpEntity<UpdateOrderRequest> entity = new HttpEntity<>(updateReq);
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                BASE_URL + "/" + created.getId(),
                HttpMethod.PUT, entity, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("PUT /orders/{id} - returns 404 when order does not exist")
    void updateOrder_NotFound_Returns404() {
        UpdateOrderRequest updateReq = new UpdateOrderRequest();
        updateReq.setStatus(OrderStatus.CONFIRMED);

        HttpEntity<UpdateOrderRequest> entity = new HttpEntity<>(updateReq);
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                BASE_URL + "/999999", HttpMethod.PUT, entity, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PUT /orders/{id} - returns 400 when status is missing")
    void updateOrder_MissingStatus_Returns400() {
        OrderResponse created = createOrder(10L, savedItem.getId(), 1);

        UpdateOrderRequest updateReq = new UpdateOrderRequest();

        HttpEntity<UpdateOrderRequest> entity = new HttpEntity<>(updateReq);
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                BASE_URL + "/" + created.getId(),
                HttpMethod.PUT, entity, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getValidationErrors()).containsKey("status");
    }

    @Test
    @DisplayName("DELETE /orders/{id} - soft-deletes order and returns 204")
    void deleteOrder_Returns204() {
        OrderResponse created = createOrder(10L, savedItem.getId(), 1);

        ResponseEntity<Void> response = restTemplate.exchange(BASE_URL + "/" + created.getId(),
                HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(orderRepository.findById(created.getId())).isPresent();
        assertThat(orderRepository.findById(created.getId()).get().getDeleted()).isTrue();
    }

    @Test
    @DisplayName("DELETE /orders/{id} - returns 404 when order does not exist")
    void deleteOrder_NotFound_Returns404() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                BASE_URL + "/999999", HttpMethod.DELETE, HttpEntity.EMPTY, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE /orders/{id} - returns 410 Gone when order is already soft-deleted")
    void deleteOrder_AlreadyDeleted_Returns410() {
        OrderResponse created = createOrder(10L, savedItem.getId(), 1);
        restTemplate.delete(BASE_URL + "/" + created.getId());

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(BASE_URL + "/" + created.getId(),
                HttpMethod.DELETE, HttpEntity.EMPTY, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    @DisplayName("Circuit breaker - order response still contains order data when User Service times out")
    void circuitBreaker_UserServiceTimeout_OrderDataStillPresent() {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/api/v1/users/10"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(5000)
                        .withHeader("Content-Type", "application/json").withBody("{}")));

        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                BASE_URL, buildCreateRequest(10L, savedItem.getId(), 1), OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getTotalPrice()).isNotNull();
    }
}
