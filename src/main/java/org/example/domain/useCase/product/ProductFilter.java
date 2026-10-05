package org.example.domain.useCase.product;

import org.example.util.NumberOperators;
import org.example.util.StringOperators;

import lombok.Builder;

@Builder
public record ProductFilter(NumberOperators id, StringOperators name) {
}
