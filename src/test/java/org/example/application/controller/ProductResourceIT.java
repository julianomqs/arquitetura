package org.example.application.controller;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ProductResourceIT extends CatalogResourceIT {

  @Override
  String path() {
    return "/products";
  }
}
