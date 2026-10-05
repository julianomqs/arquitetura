package org.example.application.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.application.controller.InvoiceController.CreateInvoiceDto;
import org.example.application.controller.InvoiceController.CreateInvoiceItemDto;
import org.example.application.controller.InvoiceController.ModifyInvoiceItemDto;
import org.example.application.controller.InvoiceController.UpdateInvoiceDto;
import org.example.domain.EntityNotFoundException;
import org.example.domain.entity.Invoice;
import org.example.domain.entity.InvoiceItem;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceMapperTest {
  private InvoiceMapper mapper;

  @BeforeEach
  void setUp() throws Exception {
    mapper = Mappers.getMapper(InvoiceMapper.class);
    setField(mapper, "clientMapper", Mappers.getMapper(ClientMapper.class));
    setField(mapper, "productMapper", Mappers.getMapper(ProductMapper.class));
  }

  @Test
  void mappingInvoiceWithMultipleNewItemsKeepsEveryItem() {
    var dto = new CreateInvoiceDto("INV-1", LocalDateTime.parse("2026-01-01T10:00:00"), 1, List.of(
        new CreateInvoiceItemDto(new BigDecimal("1"), new BigDecimal("2.50"), 10),
        new CreateInvoiceItemDto(new BigDecimal("3"), new BigDecimal("4.50"), 20)));

    var invoice = mapper.toInvoice(dto);

    assertEquals(2, invoice.getItems().size());
    assertEquals(Set.of(10, 20), invoice.getItems().stream().map(i -> i.getProduct().getId()).collect(Collectors.toSet()));
  }

  @Test
  void missingItemIdInBatchUpdateBecomesDomainNotFound() {
    var invoice = new Invoice();
    invoice.setItems(List.of(new InvoiceItem(1, 0, BigDecimal.ONE, BigDecimal.ONE, null)));
    var dto = new UpdateInvoiceDto("INV-1", LocalDateTime.parse("2026-01-01T10:00:00"), 1,
        new ModifyInvoiceItemDto(null, List.of(new org.example.application.controller.InvoiceController.UpdateInvoiceItemDto(
            999, BigDecimal.ONE, BigDecimal.ONE, 10)), null));

    var error = assertThrows(EntityNotFoundException.class, () -> mapper.updateInvoice(dto, invoice));
    assertEquals("Item com id 999 não encontrado", error.getMessage());
  }

  @Test
  void batchUpdateWithCreateAndUnknownIdThrowsNotFoundNotNpe() {
    var invoice = new Invoice();
    invoice.setItems(new java.util.ArrayList<>(List.of(new InvoiceItem(1, 0, BigDecimal.ONE, BigDecimal.ONE, null))));
    var dto = new UpdateInvoiceDto("INV-1", LocalDateTime.parse("2026-01-01T10:00:00"), 1,
        new ModifyInvoiceItemDto(
            List.of(new CreateInvoiceItemDto(BigDecimal.ONE, BigDecimal.ONE, 20)),
            List.of(new org.example.application.controller.InvoiceController.UpdateInvoiceItemDto(
                999, BigDecimal.ONE, BigDecimal.ONE, 10)),
            null));

    assertThrows(EntityNotFoundException.class, () -> mapper.updateInvoice(dto, invoice));
  }

  @Test
  void batchRemoveWithCreateAndUnknownIdThrowsNotFound() {
    var invoice = new Invoice();
    invoice.setItems(new java.util.ArrayList<>(List.of(new InvoiceItem(1, 0, BigDecimal.ONE, BigDecimal.ONE, null))));
    var dto = new org.example.application.controller.InvoiceController.PatchInvoiceDto(null, null, null,
        new org.example.application.controller.InvoiceController.ModifyPatchInvoiceItemDto(
            List.of(new CreateInvoiceItemDto(BigDecimal.ONE, BigDecimal.ONE, 20)), null, Set.of(999)));

    assertThrows(EntityNotFoundException.class, () -> mapper.patchInvoice(dto, invoice));
  }

  @Test
  void batchCreateUpdateRemoveTogether() {
    var invoice = new Invoice();
    invoice.setItems(new java.util.ArrayList<>(List.of(
        new InvoiceItem(1, 0, BigDecimal.ONE, BigDecimal.ONE, null),
        new InvoiceItem(2, 0, BigDecimal.ONE, BigDecimal.ONE, null))));
    var dto = new UpdateInvoiceDto("INV-1", LocalDateTime.parse("2026-01-01T10:00:00"), 1,
        new ModifyInvoiceItemDto(
            List.of(new CreateInvoiceItemDto(BigDecimal.ONE, BigDecimal.ONE, 30)),
            List.of(new org.example.application.controller.InvoiceController.UpdateInvoiceItemDto(
                1, new BigDecimal("9"), BigDecimal.ONE, 10)),
            Set.of(2)));

    mapper.updateInvoice(dto, invoice);

    assertEquals(2, invoice.getItems().size());
    assertEquals(new BigDecimal("9"), invoice.getItems().get(0).getQuantity());
    assertEquals(30, invoice.getItems().get(1).getProduct().getId());
  }

  @Test
  void itemBodyNeverChangesItemIdOrVersion() {
    var item = new InvoiceItem(5, 3, BigDecimal.ONE, BigDecimal.ONE, null);

    mapper.updateInvoiceItem(new org.example.application.controller.InvoiceController.UpdateInvoiceItemDto(
        7, new BigDecimal("2"), BigDecimal.TEN, 10), item);

    assertEquals(5, item.getId());
    assertEquals(3, item.getVersion());
    assertEquals(new BigDecimal("2"), item.getQuantity());
  }

  @Test
  void patchItemKeepsFieldsWhenNull() {
    var item = new InvoiceItem(5, 3, BigDecimal.ONE, BigDecimal.TEN, null);

    mapper.patchInvoiceItem(new org.example.application.controller.InvoiceController.PatchItemBodyDto(
        new BigDecimal("4"), null, null), item);

    assertEquals(new BigDecimal("4"), item.getQuantity());
    assertEquals(BigDecimal.TEN, item.getUnitValue());
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    boolean found = false;
    for (var type = target.getClass(); type != null; type = type.getSuperclass()) {
      try {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
        found = true;
      } catch (NoSuchFieldException ignored) {
      }
    }
    if (!found) throw new NoSuchFieldException(name);
  }
}
