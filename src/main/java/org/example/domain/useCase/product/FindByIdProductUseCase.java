package org.example.domain.useCase.product;

import org.example.domain.EntityNotFoundException;
import org.example.domain.entity.Product;
import org.example.domain.repository.ProductRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FindByIdProductUseCase {

  @Inject
  private ProductRepository repository;

  public Product execute(Integer id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Produto com id " + id + " não encontrado"));
  }
}
