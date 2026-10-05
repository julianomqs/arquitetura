package org.example.domain.useCase.invoice;

import java.util.List;

import org.example.util.SortOrder;

import lombok.Builder;

@Builder
public record InvoiceSort(SortOrder id, SortOrder number, SortOrder dateTime, SortOrder clientId, List<String> order) {
}
