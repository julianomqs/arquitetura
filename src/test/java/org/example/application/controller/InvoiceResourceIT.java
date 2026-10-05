package org.example.application.controller;

import static io.restassured.RestAssured.given;
import static org.example.ApiSupport.create;
import static org.example.ApiSupport.json;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class InvoiceResourceIT {

  private static final AtomicInteger SEQ = new AtomicInteger();

  private int clientId;
  private int productA;
  private int productB;
  private int productC;

  @BeforeEach
  void seed() {
    var suffix = String.valueOf(System.nanoTime());
    clientId = create("/clients", Map.of("name", "Cliente " + suffix));
    productA = create("/products", Map.of("name", "A " + suffix));
    productB = create("/products", Map.of("name", "B " + suffix));
    productC = create("/products", Map.of("name", "C " + suffix));
  }

  private static Map<String, Object> item(int product, int quantity) {
    return Map.of("quantity", quantity, "unitValue", 2.5, "product", product);
  }

  private String newNumber() {
    return "INV-" + SEQ.incrementAndGet() + "-" + System.nanoTime();
  }

  private Map<String, Object> invoiceBody(List<Map<String, Object>> items) {
    return Map.of("number", newNumber(), "dateTime", "2026-01-10T10:00:00", "client", clientId, "items", items);
  }

  private int createInvoice(List<Map<String, Object>> items) {
    return create("/invoices", invoiceBody(items));
  }

  @Test
  void createsInvoiceWithTwoItems() {
    json().body(invoiceBody(List.of(item(productA, 1), item(productB, 2))))
        .post("/invoices")
        .then().statusCode(201)
        .header("Location", notNullValue())
        .body("items", hasSize(2));
  }

  @Test
  void rejectsDuplicateNumber() {
    var body = invoiceBody(List.of(item(productA, 1)));
    json().body(body).post("/invoices").then().statusCode(201);
    json().body(body).post("/invoices").then().statusCode(400);
  }

  @Test
  void rejectsEmptyItemsAndMalformedJson() {
    json().body(invoiceBody(List.of())).post("/invoices").then().statusCode(400);
    json().body("{not json").post("/invoices").then().statusCode(400);
  }

  @Test
  void unknownInvoiceAndItemAreNotFound() {
    given().get("/invoices/999999").then().statusCode(404).body("message", notNullValue());
    var id = createInvoice(List.of(item(productA, 1)));
    given().get("/invoices/" + id + "/items/999999").then().statusCode(404);
  }

  @Test
  void batchUpdateWithUnknownIdAndCreateIsNotFound() {
    var id = createInvoice(List.of(item(productA, 1)));
    var body = Map.of("number", newNumber(), "dateTime", "2026-01-10T10:00:00", "client", clientId,
        "items", Map.of(
            "create", List.of(item(productB, 1)),
            "update", List.of(Map.of("id", 999999, "quantity", 1, "unitValue", 1, "product", productA))));
    json().body(body).put("/invoices/" + id).then().statusCode(404);
  }

  @Test
  void itemPutIgnoresBodyIdAndDoesNotTouchOtherInvoice() {
    var first = createInvoice(List.of(item(productA, 1)));
    var second = createInvoice(List.of(item(productB, 7)));
    int firstItem = given().get("/invoices/" + first + "/items").then().extract().path("[0].id");
    int secondItem = given().get("/invoices/" + second + "/items").then().extract().path("[0].id");

    json().body(Map.of("id", secondItem, "quantity", 3, "unitValue", 1, "product", productC))
        .put("/invoices/" + first + "/items/" + firstItem)
        .then().statusCode(200).body("id", equalTo(firstItem));

    given().get("/invoices/" + second + "/items/" + secondItem)
        .then().statusCode(200).body("quantity", equalTo(7.0f));
    given().get("/invoices/" + first + "/items").then().body("", hasSize(1));
  }

  @Test
  void patchInvoiceKeepsOtherFields() {
    var id = createInvoice(List.of(item(productA, 1)));
    var number = "PATCHED-" + System.nanoTime();

    json().body(Map.of("number", number)).patch("/invoices/" + id)
        .then().statusCode(200)
        .body("number", equalTo(number))
        .body("items", hasSize(1));
  }

  @Test
  void deletesItemAndInvoice() {
    var id = createInvoice(List.of(item(productA, 1), item(productB, 1)));
    int itemId = given().get("/invoices/" + id + "/items").then().extract().path("[0].id");

    given().delete("/invoices/" + id + "/items/" + itemId).then().statusCode(204);
    given().get("/invoices/" + id + "/items").then().body("", hasSize(1));
    given().delete("/invoices/" + id).then().statusCode(204);
    given().get("/invoices/" + id).then().statusCode(404);
  }

  @Test
  void listFiltersByDateRangeWithExactResults() {
    var id = create("/invoices", Map.of("number", newNumber(), "dateTime", "2032-05-05T10:00:00",
        "client", clientId, "items", List.of(item(productA, 1))));

    given().queryParam("clientId", clientId)
        .queryParam("dateTime", "between|2032-05-01T00:00:00,2032-05-31T23:59:59")
        .get("/invoices").then().statusCode(200)
        .body("total", equalTo(1)).body("result[0].id", equalTo(id));
    given().queryParam("clientId", clientId)
        .queryParam("dateTime", "notBetween|2032-05-01T00:00:00,2032-05-31T23:59:59")
        .get("/invoices").then().body("total", equalTo(0));
  }
}
