package com.appname.orderservice.exception;

public class InvalidOrderStatusException extends RuntimeException {
  public InvalidOrderStatusException(String message) {
    super(message);
  }

}
