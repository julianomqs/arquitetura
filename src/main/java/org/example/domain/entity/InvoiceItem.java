package org.example.domain.entity;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {

  @EqualsAndHashCode.Include
  private Integer id;
  private Integer version;
  private BigDecimal quantity;
  private BigDecimal unitValue;
  private Product product;
}
