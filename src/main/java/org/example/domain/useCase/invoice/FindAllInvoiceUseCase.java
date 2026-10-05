package org.example.domain.useCase.invoice;

import org.example.domain.entity.Invoice;
import org.example.domain.repository.InvoiceRepository;
import org.example.util.Page;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FindAllInvoiceUseCase {

  @Inject
  private InvoiceRepository repository;

  public Page<Invoice> execute(int offset, int limit, InvoiceFilter filter, InvoiceSort sort) {
    return repository.findAll(offset, limit, filter, sort);
  }
}
