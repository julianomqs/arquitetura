package org.example.infrastructure.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "invoice")
@Data
@ToString(exclude = { "items", "client" })
@EqualsAndHashCode(of = { "id" })
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Version
  private Integer version;
  private String number;
  @Column(name = "date_time")
  private LocalDateTime dateTime;
  @ManyToOne(fetch = FetchType.LAZY)
  private ClientEntity client;
  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<InvoiceItemEntity> items = new LinkedHashSet<>();

  public InvoiceEntity(Integer id) {
    this.id = id;
  }

  public InvoiceEntity(Integer id, String number, LocalDateTime dateTime, ClientEntity client) {
    this.id = id;
    this.number = number;
    this.dateTime = dateTime;
    this.client = client;
  }
}
