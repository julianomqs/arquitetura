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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Bordas de contrato, lote, ordenação, concorrência e unicidade, contra o banco real. */
@QuarkusTest
class RobustnessIT {

  private int clientId;
  private int productA;
  private int productB;
  private int productC;

  @BeforeEach
  void seed() {
    var token = unique();
    clientId = create("/clients", Map.of("name", "rob-client-" + token));
    productA = create("/products", Map.of("name", "rob-a-" + token));
    productB = create("/products", Map.of("name", "rob-b-" + token));
    productC = create("/products", Map.of("name", "rob-c-" + token));
  }

  private static Map<String, Object> item(int product) {
    return Map.of("quantity", 1, "unitValue", 2.5, "product", product);
  }

  private int invoice(String dateTime, int... products) {
    var items = new ArrayList<Map<String, Object>>();
    for (var p : products) {
      items.add(item(p));
    }
    return create("/invoices", Map.of("number", "ROB-" + unique(), "dateTime", dateTime, "client", clientId,
        "items", items));
  }

  private static List<Integer> runInParallel(int n, Callable<Integer> call) throws Exception {
    var pool = Executors.newFixedThreadPool(n);
    try {
      var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
      var start = new java.util.concurrent.CountDownLatch(1);
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

  // --- corpo e valores ---------------------------------------------------------------

  @Test
  void emptyBodyIsBadRequestEverywhere() {
    int id = invoice("2030-01-01T10:00:00", productA);
    int cid = create("/clients", Map.of("name", "rob-empty-" + unique()));

    for (var path : List.of("/clients", "/products", "/invoices", "/invoices/" + id + "/items")) {
      given().contentType(ContentType.JSON).post(path).then().statusCode(400);
    }
    given().contentType(ContentType.JSON).patch("/invoices/" + id).then().statusCode(400);
    given().contentType(ContentType.JSON).put("/clients/" + cid).then().statusCode(400);
    given().contentType(ContentType.JSON).put("/invoices/" + id + "/items/1").then().statusCode(400);
  }

  @Test
  void numbersOutsideColumnRangeAreBadRequest() {
    var number = "ROB-" + unique();
    json().body(Map.of("number", number, "dateTime", "2030-01-01T10:00:00", "client", clientId,
        "items", List.of(Map.of("quantity", 1e20, "unitValue", 1, "product", productA))))
        .post("/invoices").then().statusCode(400);
    json().body("{\"number\":\"" + number + "\",\"dateTime\":\"2030-01-01T10:00:00\",\"client\":" + clientId
        + ",\"items\":[{\"quantity\":1.12345,\"unitValue\":1,\"product\":" + productA + "}]}")
        .post("/invoices").then().statusCode(400);
  }

  @Test
  void patchWithBlankNameIsBadRequest() {
    int cid = create("/clients", Map.of("name", "rob-blank-" + unique()));

    json().body(Map.of("name", "   ")).patch("/clients/" + cid).then().statusCode(400);
    json().body(Map.of("name", "   ")).patch("/products/" + productA).then().statusCode(400);
  }

  @Test
  void frameworkErrorsKeepStatusAndHeaders() {
    given().get("/rota-inexistente").then().statusCode(404).body("message", notNullValue());
    given().put("/clients").then().statusCode(405);
    given().contentType(ContentType.TEXT).body("x").post("/clients").then().statusCode(415);
  }

  @Test
  void limitZeroReturnsNoRowsAndNonNumericLimitIsClientError() {
    given().queryParam("limit", 0).get("/clients").then().statusCode(200).body("result", hasSize(0));
    int status = given().queryParam("limit", "abc").get("/clients").statusCode();
    assertTrue(status >= 400 && status < 500, "limit=abc retornou " + status);
  }

  @Test
  void integerFieldsRejectFractionsAndOverflow() {
    given().queryParam("clientId", "eq|1.9").get("/invoices").then().statusCode(400);
    given().queryParam("clientId", "eq|99999999999").get("/invoices").then().statusCode(400);
    given().queryParam("clientId", "in|1,2.5").get("/invoices").then().statusCode(400);
  }

  // --- subrecurso de itens -----------------------------------------------------------

  @Test
  void itemEndpointsReturnNotFoundForUnknownOrForeignItems() {
    int mine = invoice("2030-01-01T10:00:00", productA);
    int other = invoice("2030-01-01T10:00:00", productB);
    int foreignItem = given().get("/invoices/" + other + "/items").then().extract().path("[0].id");
    var body = Map.of("quantity", 1, "unitValue", 1, "product", productA);

    for (var itemId : List.of(999999, foreignItem)) {
      given().get("/invoices/" + mine + "/items/" + itemId).then().statusCode(404);
      json().body(body).put("/invoices/" + mine + "/items/" + itemId).then().statusCode(404);
      json().body(Map.of("quantity", 2)).patch("/invoices/" + mine + "/items/" + itemId).then().statusCode(404);
      given().delete("/invoices/" + mine + "/items/" + itemId).then().statusCode(404);
    }
    given().get("/invoices/999999/items").then().statusCode(404);
    json().body(item(productC)).post("/invoices/999999/items").then().statusCode(404);
    given().get("/invoices/" + other + "/items").then().body("", hasSize(1));
  }

  @Test
  void patchingItemToAnAlreadyUsedProductIsRejected() {
    int id = invoice("2030-01-01T10:00:00", productA, productB);
    int firstItem = given().get("/invoices/" + id + "/items").then().extract().path("[0].id");

    json().body(Map.of("product", productB)).patch("/invoices/" + id + "/items/" + firstItem)
        .then().statusCode(400);
  }

  @Test
  void itemsAreReturnedInIdOrder() {
    int id = invoice("2030-01-01T10:00:00", productC, productA, productB);

    given().get("/invoices/" + id).then().statusCode(200)
        .body("items.product.id", contains(productC, productA, productB));
  }

  // --- semântica do lote -------------------------------------------------------------

  @Test
  void batchRejectsSameIdInUpdateAndRemoveAndRepeatedUpdateIds() {
    int id = invoice("2030-01-01T10:00:00", productA, productB);
    int itemId = given().get("/invoices/" + id + "/items").then().extract().path("[0].id");
    var update = Map.of("id", itemId, "quantity", 3, "unitValue", 1, "product", productA);

    json().body(Map.of("items", Map.of("update", List.of(update), "remove", List.of(itemId))))
        .patch("/invoices/" + id).then().statusCode(400).body("message", notNullValue());
    json().body(Map.of("items", Map.of("update", List.of(update, Map.of("id", itemId, "quantity", 4,
        "unitValue", 1, "product", productC))))).patch("/invoices/" + id).then().statusCode(400);
    given().get("/invoices/" + id + "/items").then().body("", hasSize(2));
  }

  @Test
  void batchCanRemoveAProductAndCreateItAgainInTheSameRequest() {
    int id = invoice("2030-01-01T10:00:00", productA);
    int itemId = given().get("/invoices/" + id + "/items").then().extract().path("[0].id");

    json().body(Map.of("items", Map.of("remove", List.of(itemId), "create", List.of(item(productA)))))
        .patch("/invoices/" + id)
        .then().statusCode(200).body("items", hasSize(1)).body("items[0].product.id", equalTo(productA));
  }

  // --- ordenação e paginação estáveis -----------------------------------------------

  @Test
  void paginationIsStableWhenTheSortKeyTies() {
    var ids = List.of(invoice("2033-01-01T10:00:00", productA), invoice("2033-01-01T10:00:00", productA),
        invoice("2033-01-01T10:00:00", productA));

    for (int i = 0; i < 3; i++) {
      given().queryParam("clientId", clientId).queryParam("sort", "dateTime|ASC")
          .queryParam("limit", 1).queryParam("offset", i)
          .get("/invoices").then().statusCode(200).body("result[0].id", equalTo(ids.get(i)));
    }
  }

  // --- concorrência e unicidade -------------------------------------------------------

  @Test
  void concurrentUpdatesNeverProduceServerErrors() throws Exception {
    int id = invoice("2030-01-01T10:00:00", productA);
    var counter = new java.util.concurrent.atomic.AtomicInteger();

    var statuses = runInParallel(16, () -> json()
        .body(Map.of("dateTime", "2031-01-" + String.format("%02d", 1 + counter.incrementAndGet() % 28) + "T10:00:00"))
        .patch("/invoices/" + id).statusCode());

    for (var status : statuses) {
      assertTrue(status == 200 || status == 409, "status inesperado: " + statuses);
    }
    assertTrue(statuses.contains(200), "nenhuma atualização teve sucesso: " + statuses);
  }

  @Test
  void concurrentCreatesWithTheSameNameYieldExactlyOneRecord() throws Exception {
    var name = "rob-race-" + unique();

    var statuses = runInParallel(10, () -> json().body(Map.of("name", name)).post("/clients").statusCode());

    assertEquals(1, statuses.stream().filter(s -> s == 201).count(), "statuses: " + statuses);
    assertFalse(statuses.stream().anyMatch(s -> s >= 500), "statuses: " + statuses);
    given().queryParam("name", name).get("/clients").then().body("total", equalTo(1));
  }

  @Test
  void concurrentCreatesWithTheSameInvoiceNumberYieldExactlyOneInvoice() throws Exception {
    var number = "ROB-RACE-" + unique();
    var body = Map.of("number", number, "dateTime", "2030-01-01T10:00:00", "client", clientId,
        "items", List.of(item(productA)));

    var statuses = runInParallel(10, () -> json().body(body).post("/invoices").statusCode());

    assertEquals(1, statuses.stream().filter(s -> s == 201).count(), "statuses: " + statuses);
    assertFalse(statuses.stream().anyMatch(s -> s >= 500), "statuses: " + statuses);
  }
}
