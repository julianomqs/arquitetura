package org.example.domain.useCase.product;

import org.example.domain.entity.Product;
import org.example.domain.repository.ProductRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RemoveProductUseCase {

  @Inject
  private ProductRepository repository;

  @Transactional
  public void execute(Product product) {
    repository.remove(product);
  }
}
