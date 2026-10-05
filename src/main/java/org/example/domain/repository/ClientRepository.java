package org.example.domain.repository;

import java.util.Optional;

import org.example.domain.entity.Client;
import org.example.domain.useCase.client.ClientFilter;
import org.example.domain.useCase.client.ClientSort;
import org.example.util.Page;

public interface ClientRepository {

  Client save(Client client);

  void remove(Client client);

  Optional<Client> findById(Integer id);

  Optional<Client> find(ClientFilter filter);

  Page<Client> findAll(int offset, int limit, ClientFilter filter, ClientSort sort);
}
