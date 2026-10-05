package org.example.application.controller;

import static io.restassured.RestAssured.given;
import static org.example.ApiSupport.create;
import static org.example.ApiSupport.json;
import static org.example.ApiSupport.unique;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** ETag / If-Match, ordem de ordenação pedida, formato de erro e corrida em itens da mesma fatura. */
@QuarkusTest
class ConcurrencyContractIT {

  private int clientId;
  private int otherClientId;
  private int productA;
  private int productB;

  @BeforeEach
  void seed() {
    var token = unique();
    clientId = create("/clients", Map.of("name", "cc-client-" + token));
    otherClientId = create("/clients", Map.of("name", "cc-other-" + token));
    productA = create("/products", Map.of("name", "cc-a-" + token));
    productB = create("/products", Map.of("name", "cc-b-" + token));
  }

  private static Map<String, Object> item(int product) {
    return Map.of("quantity", 1, "unitValue", 2.5, "product", product);
  }

  private int invoice(int client, String dateTime, int product) {
    return create("/invoices", Map.of("number", "CC-" + unique(), "dateTime", dateTime, "client", client,
        "items", List.of(item(product))));
  }

  // --- ETag / If-Match ---------------------------------------------------------------

  @Test
  void resourcesExposeEtagAndIfMatchProtectsUpdates() {
    for (var path : List.of("/clients", "/products")) {
      var name = "cc-etag-" + unique();
      var created = json().body(Map.of("name", name)).post(path).then().statusCode(201)
          .header("ETag", notNullValue()).extract();
      int id = created.path("id");
      var etag = created.header("ETag");

      var updated = json().header("If-Match", etag).body(Map.of("name", name + "-b")).put(path + "/" + id)
          .then().statusCode(200).header("ETag", notNullValue()).extract().header("ETag");

      json().header("If-Match", etag).body(Map.of("name", name + "-c")).put(path + "/" + id)
          .then().statusCode(412);
      json().header("If-Match", etag).body(Map.of("name", name + "-c")).patch(path + "/" + id)
          .then().statusCode(412);
      given().header("If-Match", etag).delete(path + "/" + id).then().statusCode(412);
      given().get(path + "/" + id).then().header("ETag", equalTo(updated)).body("name", equalTo(name + "-b"));

      json().header("If-Match", updated).body(Map.of("name", name + "-d")).patch(path + "/" + id)
          .then().statusCode(200);
      given().header("If-Match", "*").delete(path + "/" + id).then().statusCode(204);
    }
  }

  @Test
  void invoiceEtagChangesWhenItemsChange() {
    int id = invoice(clientId, "2030-01-01T10:00:00", productA);
    var etag = given().get("/invoices/" + id).then().statusCode(200).header("ETag", notNullValue())
        .extract().header("ETag");

    json().body(item(productB)).post("/invoices/" + id + "/items").then().statusCode(201);

    var after = given().get("/invoices/" + id).then().extract().header("ETag");
    assertFalse(etag.equals(after), "o ETag deveria mudar quando um item é adicionado: " + etag);

    json().header("If-Match", etag).body(Map.of("dateTime", "2030-02-02T10:00:00")).patch("/invoices/" + id)
        .then().statusCode(412);
    json().header("If-Match", after).body(Map.of("dateTime", "2030-02-02T10:00:00")).patch("/invoices/" + id)
        .then().statusCode(200).header("ETag", notNullValue());
    given().header("If-Match", after).delete("/invoices/" + id).then().statusCode(412);
  }

  // --- ordenação pedida --------------------------------------------------------------

  @Test
  void sortFollowsTheOrderRequestedByTheClient() {
    int a1 = invoice(clientId, "2034-01-01T10:00:00", productA);
    int a2 = invoice(clientId, "2034-01-02T10:00:00", productA);
    int b1 = invoice(otherClientId, "2034-01-01T10:00:00", productA);
    int b2 = invoice(otherClientId, "2034-01-02T10:00:00", productA);
    var filter = "in|" + clientId + "," + otherClientId;

    given().queryParam("clientId", filter).queryParam("sort", "clientId|ASC,dateTime|DESC").get("/invoices")
        .then().statusCode(200).body("result.id", contains(a2, a1, b2, b1));
    given().queryParam("clientId", filter).queryParam("sort", "dateTime|DESC,clientId|ASC").get("/invoices")
        .then().body("result.id", contains(a2, b2, a1, b1));
    given().queryParam("clientId", filter).queryParam("sort", "dateTime|ASC,clientId|DESC").get("/invoices")
        .then().body("result.id", contains(b1, a1, b2, a2));
  }

  @Test
  void sortCannotTargetTheOrderBookkeepingField() {
    given().queryParam("sort", "order|ASC").get("/invoices").then().statusCode(400);
  }

  // --- formato de erro ----------------------------------------------------------------

  @Test
  void validationErrorsUseTheSameEnvelopeAsOtherErrors() {
    json().body(Map.of("name", "")).post("/clients")
        .then().statusCode(400)
        .body("message", notNullValue())
        .body("violations", hasSize(1))
        .body("violations[0].path", notNullValue())
        .body("violations[0].message", notNullValue());
    given().get("/clients/999999").then().statusCode(404).body("message", notNullValue());
  }

  // --- itens concorrentes na mesma fatura --------------------------------------------

  @Test
  void concurrentAddsOfTheSameProductToOneInvoiceYieldASingleItem() throws Exception {
    int id = invoice(clientId, "2030-01-01T10:00:00", productA);

    var statuses = runInParallel(8, () -> json().body(item(productB)).post("/invoices/" + id + "/items").statusCode());

    assertEquals(1, statuses.stream().filter(s -> s == 201).count(), "statuses: " + statuses);
    assertFalse(statuses.stream().anyMatch(s -> s >= 500), "statuses: " + statuses);
    given().get("/invoices/" + id + "/items").then().body("", hasSize(2));
  }

  private static List<Integer> runInParallel(int n, Callable<Integer> call) throws Exception {
    var pool = Executors.newFixedThreadPool(n);
    try {
      var start = new CountDownLatch(1);
      var futures = new ArrayList<Future<Integer>>();
      for (int i = 0; i < n; i++) {
        futures.add(pool.submit(() -> {
          start.await();
          return call.call();
        }));
      }
      start.countDown();
      var statuses = new ArrayList<Integer>();
      for (var f : futures) {
        statuses.add(f.get());
      }
      return statuses;
    } finally {
      pool.shutdownNow();
    }
  }
}
