package org.example.exceptionmapper;

/** Referência (cliente, produto) informada no corpo da requisição que não existe. */
public class InvalidReferenceException extends RuntimeException {

  public InvalidReferenceException(String message) {
    super(message);
  }
}
