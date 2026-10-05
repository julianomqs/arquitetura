package org.example.exceptionmapper;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

  public static record ViolationResponse(String path, String message) {
  }

  public static record ValidationErrorResponse(String message, java.util.List<ViolationResponse> violations) {
  }

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    var violations = exception.getConstraintViolations().stream().toList();

    var violationResponses = violations.stream()
        .map(violation -> new ViolationResponse(violation.getPropertyPath().toString(), violation.getMessage()))
        .toList();

    return Response.status(Response.Status.BAD_REQUEST)
        .entity(new ValidationErrorResponse("Dados inválidos.", violationResponses))
        .build();
  }
}
