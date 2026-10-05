package org.example.application.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Validation;
import org.example.application.controller.InvoiceController.CreateInvoiceDto;
import org.example.application.controller.InvoiceController.CreateInvoiceItemDto;
import org.example.application.controller.InvoiceController.ModifyPatchInvoiceItemDto;
import org.example.application.controller.InvoiceController.PatchInvoiceDto;
import org.junit.jupiter.api.Test;

class InvoiceValidationTest {
  @Test
  void cascadesValidationIntoInvoiceItems() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var dto = new CreateInvoiceDto("INV-1", LocalDateTime.now(), 1,
          List.of(new CreateInvoiceItemDto(null, null, null)));

      var paths = factory.getValidator().validate(dto).stream()
          .map(violation -> violation.getPropertyPath().toString())
          .toList();

      assertTrue(paths.contains("items[0].quantity"));
      assertTrue(paths.contains("items[0].unitValue"));
      assertTrue(paths.contains("items[0].product"));
    }
  }

  @Test
  void cascadesValidationThroughPatchItemCollections() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var dto = new PatchInvoiceDto(null, null, null,
          new ModifyPatchInvoiceItemDto(List.of(new CreateInvoiceItemDto(null, null, null)), null, null));

      var paths = factory.getValidator().validate(dto).stream()
          .map(violation -> violation.getPropertyPath().toString())
          .toList();

      assertTrue(paths.contains("items.create[0].quantity"));
      assertTrue(paths.contains("items.create[0].unitValue"));
      assertTrue(paths.contains("items.create[0].product"));
    }
  }
}
