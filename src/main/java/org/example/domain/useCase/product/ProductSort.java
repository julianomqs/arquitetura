package org.example.domain.useCase.product;

import java.util.List;

import org.example.util.SortOrder;

import lombok.Builder;

@Builder
public record ProductSort(SortOrder id, SortOrder name, List<String> order) {
}
