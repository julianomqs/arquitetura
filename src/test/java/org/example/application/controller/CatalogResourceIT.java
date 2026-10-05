package org.example.application.controller;

import static io.restassured.RestAssured.given;
import static org.example.ApiSupport.create;
import static org.example.ApiSupport.json;
import static org.example.ApiSupport.unique;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Comportamento comum de /clients e /products, contra o banco real. */
abstract class CatalogResourceIT {

  abstract String path();

  /** Nome com prefixo "zzz" para aparecer primeiro na ordenação DESC, isolado entre testes. */
  private static String name(String token, String suffix) {
    return "zzz-" + token + "-" + suffix;
  }

  @Test
  void createReturns201WithLocationAndBody() {
    var name = name(unique(), "a");

    json().body(Map.of("name", name)).post(path())
        .then().statusCode(201)
        .header("Location", notNullValue())
        .body("id", notNullValue())
        .body("name", equalTo(name));
  }

  @Test
  void createRejectsInvalidBodies() {
    json().body(Map.of("name", "")).post(path()).then().statusCode(400);
    json().body(Map.of("name", "x".repeat(256))).post(path()).then().statusCode(400);
    json().body(Map.of()).post(path()).then().statusCode(400);
    json().body("{not json").post(path()).then().statusCode(400);
  }

  @Test
  void createRejectsDuplicateName() {
    var name = name(unique(), "dup");
    create(path(), Map.of("name", name));

    json().body(Map.of("name", name)).post(path()).then().statusCode(400).body("message", notNullValue());
  }

  @Test
  void findByIdReturnsEntityOrNotFound() {
    var name = name(unique(), "a");
    int id = create(path(), Map.of("name", name));

    given().get(path() + "/" + id).then().statusCode(200).body("name", equalTo(name));
    given().get(path() + "/999999").then().statusCode(404).body("message", notNullValue());
  }

  @Test
  void putReplacesNameAllowsOwnNameAndRejectsOthers() {
    var token = unique();
    int id = create(path(), Map.of("name", name(token, "a")));
    create(path(), Map.of("name", name(token, "b")));

    json().body(Map.of("name", name(token, "c"))).put(path() + "/" + id)
        .then().statusCode(200).body("name", equalTo(name(token, "c")));
    json().body(Map.of("name", name(token, "c"))).put(path() + "/" + id).then().statusCode(200);
    json().body(Map.of("name", name(token, "b"))).put(path() + "/" + id).then().statusCode(400);
    json().body(Map.of("name", "")).put(path() + "/" + id).then().statusCode(400);
    json().body(Map.of("name", "x")).put(path() + "/999999").then().statusCode(404);
  }

  @Test
  void patchChangesNameAndValidates() {
    var token = unique();
    int id = create(path(), Map.of("name", name(token, "a")));

    json().body(Map.of("name", name(token, "p"))).patch(path() + "/" + id)
        .then().statusCode(200).body("name", equalTo(name(token, "p")));
    json().body(Map.of()).patch(path() + "/" + id)
        .then().statusCode(200).body("name", equalTo(name(token, "p")));
    json().body(Map.of("name", "")).patch(path() + "/" + id).then().statusCode(400);
    json().body(Map.of("name", "x")).patch(path() + "/999999").then().statusCode(404);
  }

  @Test
  void deleteRemovesAndSecondDeleteIsNotFound() {
    int id = create(path(), Map.of("name", name(unique(), "a")));

    given().delete(path() + "/" + id).then().statusCode(204);
    given().get(path() + "/" + id).then().statusCode(404);
    given().delete(path() + "/" + id).then().statusCode(404);
  }

  @Test
  void listPaginatesAndSorts() {
    var token = unique();
    for (var s : new String[] { "a", "b", "c" }) {
      create(path(), Map.of("name", name(token, s)));
    }
    var filter = "startsWith|" + name(token, "");

    given().queryParam("name", filter).queryParam("sort", "name|ASC").get(path())
        .then().statusCode(200)
        .body("total", equalTo(3))
        .body("result.name", contains(name(token, "a"), name(token, "b"), name(token, "c")));
    given().queryParam("name", filter).queryParam("sort", "name|DESC").get(path())
        .then().body("result.name", contains(name(token, "c"), name(token, "b"), name(token, "a")));
    given().queryParam("name", filter).queryParam("limit", 2).get(path())
        .then().body("result", hasSize(2)).body("total", equalTo(3));
    given().queryParam("name", filter).queryParam("offset", 2).queryParam("limit", 2).get(path())
        .then().body("result", hasSize(1)).body("total", equalTo(3));
  }

  @Test
  void listStringOperators() {
    var token = unique();
    var a = name(token, "a");
    var b = name(token, "b");
    var c = name(token, "c");
    for (var n : new String[] { a, b, c }) {
      create(path(), Map.of("name", n));
    }

    check("eq|" + b, hasSize(1), hasItem(b));
    check(b, hasSize(1), hasItem(b)); // sem critério equivale a eq
    check("startsWith|zzz-" + token, hasSize(3), hasItems(a, b, c));
    check("endsWith|" + token + "-c", hasSize(1), hasItem(c));
    check("contains|" + token, hasSize(3), hasItems(a, b, c));
    check("in|" + a + "," + c, hasSize(2), hasItems(a, c));

    // operadores de exclusão: o resultado é paginado, então só dá para checar o que NÃO aparece
    checkExcludes("ne|" + b, List.of(a, c), List.of(b));
    checkExcludes("notIn|" + a + "," + b, List.of(c), List.of(a, b));
    checkExcludes("notContains|-b", List.of(a, c), List.of(b));
    checkExcludes("notStartsWith|zzz-" + token + "-a", List.of(b, c), List.of(a));
    checkExcludes("notEndsWith|-c", List.of(a, b), List.of(c));
  }

  @Test
  void listRejectsBadInput() {
    given().queryParam("name", "contains|x|y").get(path()).then().statusCode(400);
    given().queryParam("name", "foo|x").get(path()).then().statusCode(400);
    given().queryParam("name", "between|x").get(path()).then().statusCode(400);
    given().queryParam("name", "in|").get(path()).then().statusCode(400);
    given().queryParam("sort", "foo|ASC").get(path()).then().statusCode(400);
    given().queryParam("sort", "name|FOO").get(path()).then().statusCode(400);
    given().queryParam("sort", "name").get(path()).then().statusCode(400);
    given().queryParam("limit", 101).get(path()).then().statusCode(400);
    given().queryParam("limit", -1).get(path()).then().statusCode(400);
    given().queryParam("offset", -1).get(path()).then().statusCode(400);
  }

  private void check(String filter, org.hamcrest.Matcher<?> size, org.hamcrest.Matcher<?> names) {
    var response = given().queryParam("name", filter).queryParam("sort", "name|DESC").queryParam("limit", 100)
        .get(path()).then().statusCode(200);
    response.body("result", castSize(size));
    response.body("result.name", castItems(names));
  }

  private void checkExcludes(String filter, List<String> present, List<String> absent) {
    var response = given().queryParam("name", filter).queryParam("sort", "name|DESC").queryParam("limit", 100)
        .get(path()).then().statusCode(200);
    for (var name : present) {
      response.body("result.name", hasItem(name));
    }
    for (var name : absent) {
      response.body("result.name", not(hasItem(name)));
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> org.hamcrest.Matcher<T> castSize(org.hamcrest.Matcher<?> m) {
    return (org.hamcrest.Matcher<T>) m;
  }

  @SuppressWarnings("unchecked")
  private static <T> org.hamcrest.Matcher<T> castItems(org.hamcrest.Matcher<?> m) {
    return (org.hamcrest.Matcher<T>) m;
  }
}
