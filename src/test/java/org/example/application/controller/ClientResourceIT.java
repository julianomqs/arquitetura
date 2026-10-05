package org.example.application.controller;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ClientResourceIT extends CatalogResourceIT {

  @Override
  String path() {
    return "/clients";
  }
}
