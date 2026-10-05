package org.example.domain.useCase.invoice;

import org.example.domain.EntityNotFoundException;
import org.example.domain.entity.Invoice;
import org.example.domain.repository.InvoiceRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FindByIdInvoiceUseCase {

  @Inject
  private InvoiceRepository repository;

  public Invoice execute(Integer id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Invoice com o código " + id + " não encontrada."));
  }
}
