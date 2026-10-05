package org.example.exceptionmapper;

import java.sql.SQLException;

import org.hibernate.StaleObjectStateException;
import org.jboss.logging.Logger;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

  private static final Logger LOG = Logger.getLogger(PersistenceExceptionMapper.class);

  private static final int MYSQL_DUPLICATE_ENTRY = 1062;
  private static final int MYSQL_ROW_IS_REFERENCED = 1451;
  private static final int MYSQL_NO_REFERENCED_ROW = 1452;
  private static final int MYSQL_LOCK_WAIT_TIMEOUT = 1205;
  private static final int MYSQL_DEADLOCK = 1213;

  @Override
  public Response toResponse(PersistenceException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
      if (cause instanceof OptimisticLockException || cause instanceof StaleObjectStateException
          || cause instanceof PessimisticLockException || cause instanceof LockTimeoutException) {
        return error(Response.Status.CONFLICT, "O registro foi alterado por outra requisição. Tente novamente.");
      }

      if (cause instanceof SQLException sql) {
        switch (sql.getErrorCode()) {
        case MYSQL_DUPLICATE_ENTRY:
          return error(Response.Status.CONFLICT, "Já existe um registro com os valores informados.");
        case MYSQL_ROW_IS_REFERENCED:
          return error(Response.Status.CONFLICT, "O registro está em uso e não pode ser removido.");
        case MYSQL_DEADLOCK:
        case MYSQL_LOCK_WAIT_TIMEOUT:
          return error(Response.Status.CONFLICT, "O registro foi alterado por outra requisição. Tente novamente.");
        case MYSQL_NO_REFERENCED_ROW:
          return error(Response.Status.BAD_REQUEST, "O registro referenciado não existe.");
        default:
          if (sql.getSQLState() != null && sql.getSQLState().startsWith("22")) {
            return error(Response.Status.BAD_REQUEST, "Valor fora da faixa permitida.");
          }
        }
      }
    }

    LOG.error("Erro de persistência ao processar a requisição", exception);
    return error(Response.Status.INTERNAL_SERVER_ERROR, "Erro interno do servidor");
  }

  private static Response error(Response.Status status, String message) {
    return Response.status(status).entity(new ErrorMessage(message)).build();
  }
}
