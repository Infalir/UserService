package com.appname.gateway.exception;

import org.springframework.http.HttpStatus;

public class GatewayAuthException extends RuntimeException {
  private final HttpStatus status;

  public GatewayAuthException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() { return status; }
}
