package com.appname.paymentservice.integration;

import com.appname.paymentservice.dto.request.CreatePaymentRequest;
import com.appname.paymentservice.dto.response.PaymentResponse;
import com.appname.paymentservice.dto.response.PaymentSumResponse;
import com.appname.paymentservice.entity.PaymentStatus;
import com.appname.paymentservice.repository.PaymentRepository;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentIntegrationTest extends BaseIntegrationTest {
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PaymentRepository paymentRepository;

    private static final String BASE_URL = "/api/v1/payments";

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        randomApiMock.resetAll();
    }

    private CreatePaymentRequest buildRequest(String orderId, String userId, double amount) {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setOrderId(orderId);
        req.setUserId(userId);
        req.setPaymentAmount(BigDecimal.valueOf(amount));
        return req;
    }

    private void stubRandomApi(int returnNumber) {
        randomApiMock.stubFor(WireMock.get(urlPathEqualTo("/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(String.valueOf(returnNumber))));
    }

    @Test
    @DisplayName("POST /payments - even number from API → SUCCESS status")
    void createPayment_EvenNumber_ReturnsSuccess() {
        stubRandomApi(42);

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(BASE_URL, buildRequest("order-1", "user-1", 99.99), PaymentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getBody().getPaymentAmount()).isEqualByComparingTo("99.99");
        assertThat(response.getBody().getId()).isNotBlank();
    }

    @Test
    @DisplayName("POST /payments - odd number from API → FAILED status")
    void createPayment_OddNumber_ReturnsFailed() {
        stubRandomApi(77);

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(BASE_URL, buildRequest("order-1", "user-1", 50.00), PaymentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("POST /payments - payment saved to MongoDB")
    void createPayment_SavedToMongoDB() {
        stubRandomApi(2);

        restTemplate.postForEntity(BASE_URL, buildRequest("order-1", "user-1", 100.00), PaymentResponse.class);

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("order-1")).hasSize(1);
    }

    @Test
    @DisplayName("POST /payments - returns FAILED when random API is unavailable (circuit fallback)")
    void createPayment_RandomApiUnavailable_ReturnsFailed() {
        randomApiMock.stubFor(WireMock.get(urlPathEqualTo("/random")).willReturn(aResponse().withStatus(500)));

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(BASE_URL, buildRequest("order-1", "user-1", 50.00), PaymentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("POST /payments - returns 400 for missing orderId")
    void createPayment_MissingOrderId_Returns400() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setUserId("user-1");
        req.setPaymentAmount(BigDecimal.TEN);

        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL, req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /payments?userId - returns payments for user")
    void getPayments_ByUserId() {
        stubRandomApi(4);
        restTemplate.postForEntity(BASE_URL, buildRequest("order-1", "user-1", 10.00), PaymentResponse.class);
        restTemplate.postForEntity(BASE_URL, buildRequest("order-2", "user-2", 20.00), PaymentResponse.class);

        ResponseEntity<PaymentResponse[]> response = restTemplate.getForEntity(BASE_URL + "?userId=user-1", PaymentResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getUserId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("GET /payments?orderId - returns payments for order")
    void getPayments_ByOrderId() {
        stubRandomApi(6);
        restTemplate.postForEntity(BASE_URL, buildRequest("order-99", "user-1", 10.00), PaymentResponse.class);

        ResponseEntity<PaymentResponse[]> response = restTemplate.getForEntity(BASE_URL + "?orderId=order-99", PaymentResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("GET /payments?status=SUCCESS - returns only successful payments")
    void getPayments_ByStatus() {
        stubRandomApi(2);
        restTemplate.postForEntity(BASE_URL, buildRequest("order-1", "user-1", 10.00), PaymentResponse.class);

        randomApiMock.resetAll();
        stubRandomApi(3);
        restTemplate.postForEntity(BASE_URL, buildRequest("order-2", "user-1", 20.00), PaymentResponse.class);

        ResponseEntity<PaymentResponse[]> response = restTemplate.getForEntity(BASE_URL + "?status=SUCCESS", PaymentResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("GET /payments/sum/user/{userId} - returns correct total for user")
    void getUserPaymentSum_ReturnsCorrectTotal() {
        stubRandomApi(4);
        restTemplate.postForEntity(BASE_URL, buildRequest("order-1", "user-1", 100.00), PaymentResponse.class);
        restTemplate.postForEntity(BASE_URL, buildRequest("order-2", "user-1", 50.00), PaymentResponse.class);

        String from = LocalDateTime.now().minusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String to = LocalDateTime.now().plusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);


        ResponseEntity<PaymentSumResponse> response = restTemplate.getForEntity(BASE_URL + "/sum/user/user-1?from=" + from + "&to=" + to,
                PaymentSumResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotal()).isEqualByComparingTo("150.00");
        assertThat(response.getBody().getUserId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("GET /payments/sum/all - returns total across all users")
    void getAllUsersPaymentSum_ReturnsTotal() {
        stubRandomApi(2);
        restTemplate.postForEntity(BASE_URL, buildRequest("order-1", "user-1", 100.00), PaymentResponse.class);
        restTemplate.postForEntity(BASE_URL, buildRequest("order-2", "user-2", 200.00), PaymentResponse.class);

        String from = LocalDateTime.now().minusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String to = LocalDateTime.now().plusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        ResponseEntity<PaymentSumResponse> response = restTemplate.getForEntity(BASE_URL + "/sum/all?from=" + from + "&to=" + to,
                PaymentSumResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotal()).isEqualByComparingTo("300.00");
        assertThat(response.getBody().getUserId()).isNull();
    }

}
