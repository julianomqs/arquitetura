package org.example.application.controller;

import static io.restassured.RestAssured.given;
import static org.example.ApiSupport.create;
import static org.example.ApiSupport.json;
import static org.example.ApiSupport.unique;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Regras de negócio da fatura e operadores de filtro, contra o banco real. */
@QuarkusTest
class InvoiceRulesIT {

  private int clientId;
  private int otherClientId;
  private int productA;
  private int productB;

  @BeforeEach
  void seed() {
    var token = unique();
    clientId = create("/clients", Map.of("name", "rules-client-" + token));
    otherClientId = create("/clients", Map.of("name", "rules-other-" + token));
    productA = create("/products", Map.of("name", "rules-a-" + token));
    productB = create("/products", Map.of("name", "rules-b-" + token));
  }

  private static Map<String, Object> item(int product) {
    return Map.of("quantity", 1, "unitValue", 2.5, "product", product);
  }

  private static Map<String, Object> body(String number, int client, String dateTime, List<?> items) {
    return Map.of("number", number, "dateTime", dateTime, "client", client, "items", items);
  }

  private int createInvoice(String number, int client, String dateTime, int product) {
    return create("/invoices", body(number, client, dateTime, List.of(item(product))));
  }

  // --- regras do SaveInvoiceUseCase -------------------------------------------------

  @Test
  void duplicateNumberIsRejectedOnCreateAndOnUpdateOfAnotherInvoice() {
    var n1 = "R-" + unique();
    var n2 = "R-" + unique();
    int first = createInvoice(n1, clientId, "2030-01-01T10:00:00", productA);
    createInvoice(n2, clientId, "2030-01-01T10:00:00", productA);

    json().body(body(n1, clientId, "2030-01-01T10:00:00", List.of(item(productA))))
        .post("/invoices").then().statusCode(400).body("message", notNullValue());
    json().body(Map.of("number", n2, "dateTime", "2030-01-01T10:00:00", "client", clientId))
        .put("/invoices/" + first)
        .then().statusCode(400).body("message", equalTo("Já existe uma nota fiscal com o número informado."));
    json().body(Map.of("number", n2)).patch("/invoices/" + first).then().statusCode(400);
  }

  @Test
  void updatingWithItsOwnNumberIsAllowed() {
    var number = "R-" + unique();
    int id = createInvoice(number, clientId, "2030-01-01T10:00:00", productA);

    json().body(Map.of("number", number, "dateTime", "2030-02-02T10:00:00", "client", otherClientId))
        .put("/invoices/" + id)
        .then().statusCode(200)
        .body("number", equalTo(number))
        .body("client.id", equalTo(otherClientId))
        .body("items", hasSize(1));
  }

  @Test
  void unknownClientOrProductIsUnprocessable() {
    json().body(body("R-" + unique(), 999999, "2030-01-01T10:00:00", List.of(item(productA))))
        .post("/invoices").then().statusCode(422).body("message", notNullValue());
    json().body(body("R-" + unique(), clientId, "2030-01-01T10:00:00", List.of(item(999999))))
        .post("/invoices").then().statusCode(422).body("message", notNullValue());
    int id = createInvoice("R-" + unique(), clientId, "2030-01-01T10:00:00", productA);
    json().body(Map.of("client", 999999)).patch("/invoices/" + id).then().statusCode(422);
    json().body(item(999999)).post("/invoices/" + id + "/items").then().statusCode(422);
  }

  @Test
  void sameProductTwiceIsRejectedEverywhere() {
    json().body(body("R-" + unique(), clientId, "2030-01-01T10:00:00", List.of(item(productA), item(productA))))
        .post("/invoices").then().statusCode(400);

    int id = createInvoice("R-" + unique(), clientId, "2030-01-01T10:00:00", productA);

    json().body(item(productA)).post("/invoices/" + id + "/items").then().statusCode(400);
    json().body(Map.of("items", Map.of("create", List.of(item(productA))))).patch("/invoices/" + id)
        .then().statusCode(400);
  }

  @Test
  void addsItemToExistingInvoice() {
    int id = createInvoice("R-" + unique(), clientId, "2030-01-01T10:00:00", productA);

    json().body(item(productB)).post("/invoices/" + id + "/items")
        .then().statusCode(201)
        .header("Location", notNullValue())
        .body("product.id", equalTo(productB));
    given().get("/invoices/" + id + "/items").then().body("", hasSize(2));
  }

  @Test
  void batchCreateUpdateRemoveInOneRequest() {
    int id = createInvoice("R-" + unique(), clientId, "2030-01-01T10:00:00", productA);
    int itemId = given().get("/invoices/" + id + "/items").then().extract().path("[0].id");
    int productC = create("/products", Map.of("name", "rules-c-" + unique()));

    json().body(Map.of("items", Map.of(
        "create", List.of(item(productB)),
        "update", List.of(Map.of("id", itemId, "quantity", 9, "product", productC)))))
        .patch("/invoices/" + id)
        .then().statusCode(200).body("items", hasSize(2));

    json().body(Map.of("items", Map.of("remove", List.of(itemId)))).patch("/invoices/" + id)
        .then().statusCode(200).body("items", hasSize(1));
    json().body(Map.of("items", Map.of("remove", List.of(itemId)))).patch("/invoices/" + id)
        .then().statusCode(404);
  }

  @Test
  void createDtoRejectsInvalidItems() {
    var n = "R-" + unique();
    json().body(body(n, clientId, "2030-01-01T10:00:00",
        List.of(Map.of("quantity", -1, "unitValue", 1, "product", productA))))
        .post("/invoices").then().statusCode(400);
    json().body(body(n, clientId, "2030-01-01T10:00:00", java.util.Arrays.asList((Object) null)))
        .post("/invoices").then().statusCode(400);
    json().body(Map.of("number", "", "dateTime", "2030-01-01T10:00:00", "client", clientId,
        "items", List.of(item(productA)))).post("/invoices").then().statusCode(400);
    json().body(Map.of("number", n, "client", clientId, "items", List.of(item(productA))))
        .post("/invoices").then().statusCode(400);
  }

  @Test
  void itemEndpointsDoNotRequireIdInBody() {
    int id = createInvoice("R-" + unique(), clientId, "2030-01-01T10:00:00", productA);
    int itemId = given().get("/invoices/" + id + "/items").then().extract().path("[0].id");

    json().body(Map.of("quantity", 5, "unitValue", 1, "product", productA))
        .put("/invoices/" + id + "/items/" + itemId).then().statusCode(200).body("quantity", equalTo(5.0f));
    json().body(Map.of("unitValue", 7)).patch("/invoices/" + id + "/items/" + itemId)
        .then().statusCode(200).body("unitValue", equalTo(7.0f)).body("quantity", equalTo(5.0f));
  }

  @Test
  void deletingClientOrProductInUseIsConflict() {
    createInvoice("R-" + unique(), clientId, "2030-01-01T10:00:00", productA);

    given().delete("/clients/" + clientId).then().statusCode(409).body("message", notNullValue());
    given().delete("/products/" + productA).then().statusCode(409).body("message", notNullValue());
    given().get("/clients/" + clientId).then().statusCode(200);
  }

  // --- operadores de filtro (dateTime e clientId) ------------------------------------

  private void seedThreeDays() {
    createInvoice("R-" + unique(), clientId, "2030-03-01T10:00:00", productA);
    createInvoice("R-" + unique(), clientId, "2030-03-02T10:00:00", productA);
    createInvoice("R-" + unique(), clientId, "2030-03-03T10:00:00", productA);
  }

  private int totalForDateTime(String filter) {
    return given().queryParam("clientId", clientId).queryParam("dateTime", filter).get("/invoices")
        .then().statusCode(200).extract().path("total");
  }

  @Test
  void dateTimeOperators() {
    seedThreeDays();
    var d1 = "2030-03-01T10:00:00";
    var d2 = "2030-03-02T10:00:00";
    var d3 = "2030-03-03T10:00:00";

    org.junit.jupiter.api.Assertions.assertAll(
        () -> org.junit.jupiter.api.Assertions.assertEquals(1, totalForDateTime("eq|" + d2), "eq"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(1, totalForDateTime(d2), "sem critério"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(2, totalForDateTime("ne|" + d2), "ne"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(2, totalForDateTime("gt|" + d1), "gt"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(3, totalForDateTime("ge|" + d1), "ge"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(2, totalForDateTime("lt|" + d3), "lt"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(2, totalForDateTime("le|" + d2), "le"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(2, totalForDateTime("between|" + d1 + "," + d2), "between"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(1, totalForDateTime("notBetween|" + d1 + "," + d2),
            "notBetween"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(2, totalForDateTime("in|" + d1 + "," + d3), "in"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(1, totalForDateTime("notIn|" + d1 + "," + d3), "notIn"),
        () -> org.junit.jupiter.api.Assertions.assertEquals(2, totalForDateTime("ge|2030-03-02"), "data sem hora (de novo)"));
  }

  @Test
  void dateTimeWithOffsetIsRejectedBecauseItDependsOnServerTimezone() {
    given().queryParam("dateTime", "ge|2030-03-01T10:00:00Z").get("/invoices").then().statusCode(400);
    given().queryParam("dateTime", "ge|2030-03-01T10:00:00-03:00").get("/invoices").then().statusCode(400);
  }

  @Test
  void clientIdOperatorsAndSort() {
    int first = createInvoice("R-" + unique(), clientId, "2031-01-01T10:00:00", productA);
    int second = createInvoice("R-" + unique(), otherClientId, "2031-01-02T10:00:00", productA);

    given().queryParam("clientId", "in|" + clientId + "," + otherClientId).get("/invoices")
        .then().statusCode(200).body("total", equalTo(2));
    given().queryParam("clientId", "between|" + clientId + "," + otherClientId).get("/invoices")
        .then().body("total", equalTo(2));
    given().queryParam("clientId", "eq|" + clientId).get("/invoices")
        .then().body("total", equalTo(1)).body("result[0].id", equalTo(first));
    given().queryParam("clientId", "ge|" + otherClientId).get("/invoices")
        .then().body("result.id", org.hamcrest.Matchers.hasItem(second));
    given().queryParam("clientId", "in|" + clientId + "," + otherClientId).queryParam("sort", "dateTime|ASC")
        .get("/invoices").then().body("result.id", org.hamcrest.Matchers.contains(first, second));
    given().queryParam("clientId", "in|" + clientId + "," + otherClientId).queryParam("sort", "dateTime|DESC")
        .get("/invoices").then().body("result.id", org.hamcrest.Matchers.contains(second, first));
  }

  @Test
  void invalidFiltersAreBadRequest() {
    given().queryParam("dateTime", "contains|x").get("/invoices").then().statusCode(400);
    given().queryParam("dateTime", "ge|abc").get("/invoices").then().statusCode(400);
    given().queryParam("dateTime", "between|2030-01-01").get("/invoices").then().statusCode(400);
    given().queryParam("dateTime", "foo|2030-01-01").get("/invoices").then().statusCode(400);
    given().queryParam("clientId", "eq|abc").get("/invoices").then().statusCode(400);
    given().queryParam("sort", "foo|ASC").get("/invoices").then().statusCode(400);
    given().queryParam("sort", "dateTime|FOO").get("/invoices").then().statusCode(400);
    given().queryParam("sort", "build|ASC").get("/invoices").then().statusCode(400);
  }

  @Test
  void errorsNeverLeakInternalDetails() {
    var response = given().get("/invoices/999999").then().statusCode(404).extract().asString();
    org.junit.jupiter.api.Assertions.assertFalse(response.contains("Exception"));
    org.junit.jupiter.api.Assertions.assertFalse(response.contains("org.example"));
  }
}
