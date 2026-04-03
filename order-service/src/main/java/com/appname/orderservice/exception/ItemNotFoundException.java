package com.appname.orderservice.exception;

public class ItemNotFoundException extends RuntimeException {
  public ItemNotFoundException(String message) {
    super(message);
  }

}
