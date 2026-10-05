package org.example.domain.useCase.invoice;

import java.util.LinkedHashSet;

import org.example.application.mapper.InvoiceMapper;
import org.example.domain.entity.Invoice;
import org.example.domain.repository.InvoiceRepository;
import org.example.domain.useCase.client.FindByIdClientUseCase;
import org.example.domain.useCase.product.FindByIdProductUseCase;
import org.example.domain.EntityNotFoundException;
import org.example.exceptionmapper.BusinessException;
import org.example.exceptionmapper.InvalidReferenceException;
import org.example.util.NumberOperators;
import org.example.util.StringOperators;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SaveInvoiceUseCase {

  @Inject
  private InvoiceRepository repository;
  @Inject
  private InvoiceMapper mapper;
  @Inject
  private FindByIdInvoiceUseCase findByIdInvoiceUseCase;
  @Inject
  private FindByIdClientUseCase findByIdClientUseCase;
  @Inject
  private FindByIdProductUseCase findByIdProductUseCase;

  @Transactional
  public Invoice execute(Invoice invoice) {
    validateExistingNumber(invoice);
    validateDuplicateProducts(invoice);

    if (invoice.getId() != null) {
      var invoiceDB = findByIdInvoiceUseCase.execute(invoice.getId());
      invoice = mapper.updateInvoice(invoice, invoiceDB);
    }

    var clientId = invoice.getClient().getId();

    try {
      invoice.setClient(findByIdClientUseCase.execute(clientId));
    } catch (EntityNotFoundException ex) {
      throw new InvalidReferenceException("O cliente " + clientId + " informado não existe.");
    }

    invoice.getItems().forEach(item -> {
      var productId = item.getProduct().getId();

      try {
        item.setProduct(findByIdProductUseCase.execute(productId));
      } catch (EntityNotFoundException ex) {
        throw new InvalidReferenceException("O produto " + productId + " informado não existe.");
      }
    });

    return repository.save(invoice);
  }

  private void validateExistingNumber(Invoice invoice) {
    if (invoice.getNumber() != null) {
      var filterBuilder = InvoiceFilter.builder()
          .number(StringOperators.builder()
              .eq(invoice.getNumber())
              .build());

      if (invoice.getId() != null) {
        filterBuilder = filterBuilder.id(NumberOperators.builder()
            .ne(invoice.getId())
            .build());
      }

      var filter = filterBuilder.build();

      if (repository.find(filter).isPresent()) {
        throw new BusinessException("Já existe uma nota fiscal com o número informado.");
      }
    }
  }

  private void validateDuplicateProducts(Invoice invoice) {
    var productIds = new LinkedHashSet<>();

    invoice.getItems().forEach(item -> {
      if (!productIds.add(item.getProduct().getId())) {
        throw new BusinessException("A invoice contém itens com produtos repetidos.");
      }
    });
  }
}
