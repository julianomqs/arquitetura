package org.example.exceptionmapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class ExceptionExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOG = Logger.getLogger(ExceptionExceptionMapper.class);

  @Override
  public Response toResponse(Exception exception) {
    if (exception instanceof WebApplicationException webException) {
      var status = webException.getResponse().getStatus();

      if (status < 500) {
        return Response.fromResponse(webException.getResponse())
            .type(MediaType.APPLICATION_JSON)
            .entity(new ErrorMessage(webException.getMessage()))
            .build();
      }
    }

    LOG.error("Erro interno ao processar a requisição", exception);
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(new ErrorMessage("Erro interno do servidor"))
        .build();
  }
}
