package org.example.domain.useCase.client;

import org.example.domain.EntityNotFoundException;
import org.example.domain.entity.Client;
import org.example.domain.repository.ClientRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FindByIdClientUseCase {

  @Inject
  private ClientRepository repository;

  public Client execute(Integer id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Cliente com id " + id + " não encontrado"));
  }
}
