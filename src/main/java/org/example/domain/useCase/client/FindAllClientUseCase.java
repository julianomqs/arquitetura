package org.example.domain.useCase.client;

import org.example.domain.entity.Client;
import org.example.domain.repository.ClientRepository;
import org.example.util.Page;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FindAllClientUseCase {

  @Inject
  private ClientRepository repository;

  public Page<Client> execute(int offset, int limit, ClientFilter filter, ClientSort sort) {
    return repository.findAll(offset, limit, filter, sort);
  }
}
