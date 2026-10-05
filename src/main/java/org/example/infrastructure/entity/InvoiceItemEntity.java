package org.example.infrastructure.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "invoice_item")
@Data
@ToString(exclude = { "invoice", "product" })
@NoArgsConstructor
public class InvoiceItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Version
  private Integer version;
  private BigDecimal quantity;
  @Column(name = "unit_value")
  private BigDecimal unitValue;
  @ManyToOne(fetch = FetchType.LAZY)
  private ProductEntity product;
  @ManyToOne(fetch = FetchType.LAZY)
  private InvoiceEntity invoice;

  /** Itens persistidos são iguais pelo id; itens novos (sem id) só são iguais a si mesmos. */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    return other instanceof InvoiceItemEntity item && id != null && id.equals(item.id);
  }

  @Override
  public int hashCode() {
    return InvoiceItemEntity.class.hashCode();
  }

  public InvoiceItemEntity(Integer id, Integer version, BigDecimal quantity, BigDecimal unitValue,
      ProductEntity product, InvoiceEntity invoice) {
    this.id = id;
    this.version = version;
    this.quantity = quantity;
    this.unitValue = unitValue;
    this.product = product;
    this.invoice = invoice;
  }
}
