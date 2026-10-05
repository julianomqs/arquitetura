package org.example.domain.useCase.invoice;

import org.example.domain.entity.Invoice;
import org.example.domain.repository.InvoiceRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RemoveInvoiceUseCase {

  @Inject
  private InvoiceRepository repository;

  @Transactional
  public void execute(Invoice invoice) {
    repository.remove(invoice);
  }
}
