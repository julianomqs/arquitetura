package org.example;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public final class ApiSupport {

  private ApiSupport() {
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
