package org.example.infrastructure.repository;

import static org.example.jooq.Tables.CLIENT;

import java.util.Map;
import java.util.Optional;

import org.example.application.mapper.ClientMapper;
import org.example.domain.entity.Client;
import org.example.domain.repository.ClientRepository;
import org.example.domain.useCase.client.ClientFilter;
import org.example.domain.useCase.client.ClientSort;
import org.example.infrastructure.entity.ClientEntity;
import org.example.util.NumberOperators;
import org.example.util.Page;
import org.jooq.DSLContext;
import org.jooq.Field;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClientRepositoryImpl implements ClientRepository {

  @Inject
  private BaseRepository<ClientEntity> repository;
  @Inject
  private DSLContext db;
  @Inject
  private ClientMapper mapper;

  public Client save(Client client) {
    var entity = mapper.toClientEntity(client);

    return mapper.toClient(repository.save(entity));
  }

  public void remove(Client client) {
    repository.remove(mapper.toClientEntity(client));
  }

  public Optional<Client> findById(Integer id) {
    return find(ClientFilter.builder()
        .id(NumberOperators.builder()
            .eq(id)
            .build())
        .build());
  }

  public Optional<Client> find(ClientFilter filter) {
    var where = repository.buildWhere(filter, getFiltersMap());

    return db.selectFrom(CLIENT)
        .where(where)
        .fetchOptional(r -> new Client(r.get(CLIENT.ID), r.get(CLIENT.VERSION), r.get(CLIENT.NAME)));
  }

  public Page<Client> findAll(int offset, int limit, ClientFilter filter, ClientSort sort) {
    var where = repository.buildWhere(filter, getFiltersMap());
    var orderBy = repository.buildSort(sort, getSortsMap());

    var clients = db.selectFrom(CLIENT)
        .where(where)
        .orderBy(orderBy)
        .offset(offset)
        .limit(limit)
        .fetch(r -> new Client(r.get(CLIENT.ID), r.get(CLIENT.VERSION), r.get(CLIENT.NAME)));

    var total = db.selectCount()
        .from(CLIENT)
        .where(where)
        .fetchOneInto(Integer.class);

    return new Page<>(clients, total);
  }

  private Map<String, Field<?>> getFiltersMap() {
    return Map.of(
        "id", CLIENT.ID,
        "name", CLIENT.NAME);
  }

  private Map<String, Field<?>> getSortsMap() {
    return Map.of(
        "id", CLIENT.ID,
        "name", CLIENT.NAME);
  }
}
