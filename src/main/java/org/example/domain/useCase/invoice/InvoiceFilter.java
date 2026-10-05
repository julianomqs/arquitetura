package org.example.domain.useCase.invoice;

import org.example.util.NumberOperators;
import org.example.util.StringOperators;
import org.example.util.TemporalOperators;

import lombok.Builder;

@Builder
public record InvoiceFilter(NumberOperators id, StringOperators number, TemporalOperators dateTime,
    NumberOperators clientId) {
}
