package org.example.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

  private Integer id;
  private Integer version;
  private String number;
  private LocalDateTime dateTime;
  private Client client;
  private List<InvoiceItem> items = new ArrayList<>();

  public Invoice(Integer id, Integer version, String number, LocalDateTime dateTime, Client client) {
    this.id = id;
    this.version = version;
    this.number = number;
    this.dateTime = dateTime;
    this.client = client;
  }
}
