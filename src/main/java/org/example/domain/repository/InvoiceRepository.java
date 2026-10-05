package org.example.domain.repository;

import java.util.Optional;

import org.example.domain.entity.Invoice;
import org.example.domain.useCase.invoice.InvoiceFilter;
import org.example.domain.useCase.invoice.InvoiceSort;
import org.example.util.Page;

public interface InvoiceRepository {

  public Invoice save(Invoice invoice);

  public void remove(Invoice invoice);

  public Optional<Invoice> findById(Integer id);

  public Optional<Invoice> find(InvoiceFilter filter);

  public Page<Invoice> findAll(int offset, int limit, InvoiceFilter filter, InvoiceSort sort);
}
