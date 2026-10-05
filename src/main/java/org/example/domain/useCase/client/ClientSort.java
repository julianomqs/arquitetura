package org.example.domain.useCase.client;

import java.util.List;

import org.example.util.SortOrder;

import lombok.Builder;

@Builder
public record ClientSort(SortOrder id, SortOrder name, List<String> order) {
}
