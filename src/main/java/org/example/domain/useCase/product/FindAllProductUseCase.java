package org.example.domain.useCase.product;

import org.example.domain.entity.Product;
import org.example.domain.repository.ProductRepository;
import org.example.util.Page;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FindAllProductUseCase {

  @Inject
  private ProductRepository repository;

  public Page<Product> execute(int offset, int limit, ProductFilter filter, ProductSort sort) {
    return repository.findAll(offset, limit, filter, sort);
  }
}
