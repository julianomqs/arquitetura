package org.example.domain.useCase.client;

import org.example.util.NumberOperators;
import org.example.util.StringOperators;

import lombok.Builder;

@Builder
public record ClientFilter(NumberOperators id, StringOperators name) {
}
