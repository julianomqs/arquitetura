package org.example.domain.useCase.client;

import org.example.domain.entity.Client;
import org.example.domain.repository.ClientRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RemoveClientUseCase {

  @Inject
  private ClientRepository repository;

  @Transactional
  public void execute(Client client) {
    repository.remove(client);
  }
}
