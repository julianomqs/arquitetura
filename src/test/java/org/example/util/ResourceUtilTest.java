package org.example.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.example.domain.useCase.client.ClientFilter;
import org.example.domain.useCase.client.ClientSort;
import org.example.domain.useCase.invoice.InvoiceFilter;
import org.junit.jupiter.api.Test;

class ResourceUtilTest {
  @Test
  void buildsStringAndNumberFilters() {
    var filter = ResourceUtil.buildFilter(ClientFilter.class, Map.of("name", "contains|ana", "id", "ge|12"));
    assertEquals("ana", filter.name().getContains());
    assertEquals(new BigDecimal("12"), filter.id().getGe());
  }

  @Test
  void buildsSortWithMultipleFields() {
    var sort = ResourceUtil.buildSort(ClientSort.class, "name|DESC,id|ASC");
    assertEquals(SortOrder.DESC, sort.name());
    assertEquals(SortOrder.ASC, sort.id());
  }

  @Test
  void parsesInFiltersUsingTheListElementType() {
    var filter = ResourceUtil.buildFilter(ClientFilter.class,
        Map.of("id", "in|1,2,3", "name", "in|Ana,Bia"));

    assertEquals(java.util.List.of(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3")), filter.id().getIn());
    assertEquals(java.util.List.of("Ana", "Bia"), filter.name().getIn());
  }

  @Test
  void parsesDateTimeFilter() {
    var filter = ResourceUtil.buildFilter(InvoiceFilter.class,
        Map.of("dateTime", "ge|2026-01-02T03:04:05"));

    assertEquals(LocalDateTime.parse("2026-01-02T03:04:05"), filter.dateTime().getGe());
  }

  @Test
  void blankSortMeansNoExplicitSort() {
    assertNull(ResourceUtil.buildSort(ClientSort.class, "  "));
  }

  @Test
  void rejectsUnknownSortFieldAsBadInput() {
    var error = assertThrows(IllegalArgumentException.class,
        () -> ResourceUtil.buildSort(ClientSort.class, "doesNotExist|ASC"));
    assertEquals("Campo de ordenação inválido: doesNotExist", error.getMessage());
  }

  @Test
  void rejectsMalformedSort() {
    assertThrows(IllegalArgumentException.class, () -> ResourceUtil.buildSort(ClientSort.class, "name"));
  }

  @Test
  void buildsBetweenOnNumberAndTemporal() {
    var filter = ResourceUtil.buildFilter(InvoiceFilter.class,
        Map.of("id", "between|1,5", "dateTime", "between|2026-01-01T00:00:00,2026-01-31T23:59:59"));

    assertEquals(new BigDecimal("1"), filter.id().getBetween().getStart());
    assertEquals(LocalDateTime.parse("2026-01-31T23:59:59"), filter.dateTime().getBetween().getEnd());
  }

  @Test
  void betweenRequiresTwoValues() {
    assertThrows(IllegalArgumentException.class,
        () -> ResourceUtil.buildFilter(ClientFilter.class, Map.of("id", "between|1")));
  }

  @Test
  void stringOperatorOnDateFieldIsBadInputNotNpe() {
    var error = assertThrows(IllegalArgumentException.class,
        () -> ResourceUtil.buildFilter(InvoiceFilter.class, Map.of("dateTime", "contains|x")));
    assertEquals("Critério 'contains' não suportado para o campo dateTime", error.getMessage());
  }

  @Test
  void rejectsInvalidCriteriaAndExtraPipes() {
    assertThrows(IllegalArgumentException.class,
        () -> ResourceUtil.buildFilter(ClientFilter.class, Map.of("id", "foo|1")));
    assertThrows(IllegalArgumentException.class,
        () -> ResourceUtil.buildFilter(ClientFilter.class, Map.of("id", "eq|a|b")));
  }

  @Test
  void rejectsEmptyInList() {
    var error = assertThrows(IllegalArgumentException.class,
        () -> ResourceUtil.buildFilter(ClientFilter.class, Map.of("name", "in|")));
    assertEquals("Filtros 'in' e 'notIn' devem conter pelo menos um valor.", error.getMessage());
  }

  @Test
  void invalidSortOrderHasFriendlyMessage() {
    var error = assertThrows(IllegalArgumentException.class,
        () -> ResourceUtil.buildSort(ClientSort.class, "name|FOO"));
    assertEquals("Valor de ordenação inválido: FOO. Use ASC ou DESC.", error.getMessage());
  }

  @Test
  void sortOrderIsCaseInsensitive() {
    assertEquals(SortOrder.ASC, ResourceUtil.buildSort(ClientSort.class, "name|asc").name());
  }

  @Test
  void sortRejectsNonSortMethods() {
    assertThrows(IllegalArgumentException.class, () -> ResourceUtil.buildSort(ClientSort.class, "build|ASC"));
  }

  @Test
  void sortRecordsTheOrderRequestedByTheClient() {
    var sort = ResourceUtil.buildSort(org.example.domain.useCase.invoice.InvoiceSort.class,
        "clientId|ASC,dateTime|DESC,id|ASC");

    assertEquals(java.util.List.of("clientId", "dateTime", "id"), sort.order());
  }
}
