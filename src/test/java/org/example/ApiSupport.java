package org.example;

import java.net.URI;
import java.util.regex.Pattern;

import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public final class ApiSupport {

  /** Cabeçalho de teste: impede o envio automático do If-Match (para testar o 428). */
  public static final String NO_AUTO_IF_MATCH = "X-Test-No-Auto-If-Match";

  private static final Pattern RESOURCE = Pattern.compile("^/(clients|products|invoices)/(\\d+)(/.*)?$");
  private static final Pattern ITEMS = Pattern.compile("^/invoices/\\d+/items$");

  /**
   * PUT, PATCH, DELETE (e POST de item) exigem If-Match. Para os testes que não tratam de concorrência, o filtro
   * lê o ETag atual do recurso e o envia; se o recurso não existe, envia um valor qualquer para que a API
   * responda 404, que vem antes da checagem de versão.
   */
  private static final Filter AUTO_IF_MATCH = (request, response, context) -> {
    if (request.getHeaders().hasHeaderWithName(NO_AUTO_IF_MATCH)) {
      request.removeHeader(NO_AUTO_IF_MATCH);
    } else if (!request.getHeaders().hasHeaderWithName("If-Match")) {
      var uri = URI.create(request.getURI());
      var path = uri.getPath();
      var method = request.getMethod();
      var mutates = method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE")
          || (method.equals("POST") && ITEMS.matcher(path).matches());
      var matcher = RESOURCE.matcher(path);

      if (mutates && matcher.matches()) {
        var resource = uri.getScheme() + "://" + uri.getAuthority() + "/" + matcher.group(1) + "/" + matcher.group(2);
        var etag = RestAssured.given().get(resource).header("ETag");
        request.header("If-Match", etag != null ? etag : "\"0\"");
      }
    }

    return context.next(request, response);
  };

  private ApiSupport() {
  }

  public static RequestSpecification given() {
    return RestAssured.given().filter(AUTO_IF_MATCH);
  }

  public static RequestSpecification json() {
    return given().contentType(ContentType.JSON);
  }

  public static int create(String path, Object body) {
    return json().body(body).post(path).then().statusCode(201).extract().path("id");
  }

  public static String unique() {
    return String.valueOf(System.nanoTime());
  }
}
