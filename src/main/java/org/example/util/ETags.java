package org.example.util;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.EntityTag;

/** ETag baseado na versão (lock otimista) do recurso e checagem opcional de If-Match. */
public final class ETags {

  private ETags() {
  }

  public static EntityTag of(Integer version) {
    return new EntityTag(String.valueOf(version));
  }

  /** Sem If-Match não há checagem; com If-Match, a versão atual precisa estar entre os valores informados. */
  public static void check(String ifMatch, Integer currentVersion) {
    if (ifMatch == null || ifMatch.isBlank() || ifMatch.trim().equals("*")) {
      return;
    }

    var current = String.valueOf(currentVersion);

    for (var token : ifMatch.split(",")) {
      var value = token.trim();

      if (value.startsWith("W/")) {
        value = value.substring(2);
      }

      if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
        value = value.substring(1, value.length() - 1);
      }

      if (value.equals(current)) {
        return;
      }
    }

    throw new ClientErrorException("A versão do registro não confere com o cabeçalho If-Match.", 412);
  }
}
