package org.example.exceptionmapper;

public class BusinessException extends RuntimeException {

  public BusinessException(String message) {
    super(message);
  }
}