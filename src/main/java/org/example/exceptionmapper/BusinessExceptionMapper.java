package org.example.exceptionmapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

  @Override
  public Response toResponse(BusinessException exception) {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(new ErrorMessage(exception.getMessage()))
        .build();
  }
}