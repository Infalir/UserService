package com.appname.gateway.exception;

import com.appname.gateway.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Order(-1)
@Component
public class GlobalErrorHandler implements ErrorWebExceptionHandler {
  private final ObjectMapper objectMapper;

  public GlobalErrorHandler() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    log.error("Gateway error on {}: {}", exchange.getRequest().getURI(), ex.getMessage());

    HttpStatus status;
    String message;

    if (ex instanceof RegistrationException re) {
      status  = re.getStatus();
      message = re.getMessage();
    } else if (ex instanceof GatewayAuthException ae) {
      status  = ae.getStatus();
      message = ae.getMessage();
    } else if (ex instanceof ResponseStatusException rse) {
      status  = HttpStatus.valueOf(rse.getStatusCode().value());
      message = rse.getReason() != null ? rse.getReason() : ex.getMessage();
    } else {
      status  = HttpStatus.INTERNAL_SERVER_ERROR;
      message = "An unexpected gateway error occurred";
    }

    ErrorResponse body = ErrorResponse.builder().status(status.value()).error(status.getReasonPhrase())
            .message(message).path(exchange.getRequest().getURI().getPath()).timestamp(LocalDateTime.now())
            .build();

    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(body);
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

}
