package org.example.domain.useCase.product;

import org.example.application.mapper.ProductMapper;
import org.example.domain.entity.Product;
import org.example.domain.repository.ProductRepository;
import org.example.exceptionmapper.BusinessException;
import org.example.util.NumberOperators;
import org.example.util.StringOperators;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SaveProductUseCase {

  @Inject
  private ProductRepository repository;
  @Inject
  private ProductMapper mapper;
  @Inject
  private FindByIdProductUseCase findByIdProductUseCase;

  @Transactional
  public Product execute(Product product) {
    validateExistingName(product);

    if (product.getId() != null) {
      var productDB = findByIdProductUseCase.execute(product.getId());
      product = mapper.updateProduct(product, productDB);
    }

    return repository.save(product);
  }

  private void validateExistingName(Product product) {
    if (product.getName() != null) {
      var filterBuilder = ProductFilter.builder()
          .name(StringOperators.builder()
              .eq(product.getName())
              .build());

      if (product.getId() != null) {
        filterBuilder = filterBuilder.id(NumberOperators.builder()
            .ne(product.getId())
            .build());
      }

      var filter = filterBuilder.build();

      if (repository.find(filter).isPresent()) {
        throw new BusinessException("Já existe um produto com o nome informado.");
      }
    }
  }
}
