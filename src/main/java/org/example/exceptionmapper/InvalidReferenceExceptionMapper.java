package org.example.exceptionmapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidReferenceExceptionMapper implements ExceptionMapper<InvalidReferenceException> {

  @Override
  public Response toResponse(InvalidReferenceException exception) {
    return Response.status(422).entity(new ErrorMessage(exception.getMessage())).build();
  }
}
