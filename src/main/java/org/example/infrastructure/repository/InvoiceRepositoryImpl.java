package org.example.infrastructure.repository;

import static org.example.jooq.Tables.INVOICE;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.row;
import static org.jooq.impl.DSL.select;

import java.util.Map;
import java.util.Optional;

import org.example.application.mapper.InvoiceMapper;
import org.example.domain.entity.Client;
import org.example.domain.entity.Invoice;
import org.example.domain.entity.InvoiceItem;
import org.example.domain.entity.Product;
import org.example.domain.repository.InvoiceRepository;
import org.example.domain.useCase.invoice.InvoiceFilter;
import org.example.domain.useCase.invoice.InvoiceSort;
import org.example.infrastructure.entity.InvoiceEntity;
import org.example.util.NumberOperators;
import org.example.util.Page;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Records;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InvoiceRepositoryImpl implements InvoiceRepository {

  @Inject
  private BaseRepository<InvoiceEntity> repository;
  @Inject
  private DSLContext db;
  @Inject
  private InvoiceMapper mapper;

  public Invoice save(Invoice invoice) {
    var entity = mapper.toInvoiceEntity(invoice);

    return mapper.toInvoice(repository.save(entity, true));
  }

  public void remove(Invoice invoice) {
    repository.remove(mapper.toInvoiceEntity(invoice));
  }

  public Optional<Invoice> findById(Integer id) {
    return find(InvoiceFilter.builder()
        .id(NumberOperators.builder()
            .eq(id)
            .build())
        .build());
  }

  public Optional<Invoice> find(InvoiceFilter filter) {
    var client = field(row(
        INVOICE.client().ID,
        INVOICE.client().VERSION,
        INVOICE.client().NAME)
            .mapping(Client::new)
            .as("client"));

    var items = multiset(
        select(
            INVOICE.invoiceItem().ID,
            INVOICE.invoiceItem().VERSION,
            INVOICE.invoiceItem().QUANTITY,
            INVOICE.invoiceItem().UNIT_VALUE,
            row(
                INVOICE.invoiceItem().product().ID,
                INVOICE.invoiceItem().product().VERSION,
                INVOICE.invoiceItem().product().NAME)
                    .mapping(Product::new)
                    .as("product"))
                        .from(INVOICE.invoiceItem())
                        .orderBy(INVOICE.invoiceItem().ID))
                            .as("items")
                            .convertFrom(r -> r.map(Records.mapping(InvoiceItem::new)));

    var where = repository.buildWhere(
        filter,
        getFiltersMap());

    return db.select(
        INVOICE.ID,
        INVOICE.VERSION,
        INVOICE.NUMBER,
        INVOICE.DATE_TIME,
        client,
        items)
        .from(INVOICE)
        .where(where)
        .fetchOptional(r -> new Invoice(
            r.get(INVOICE.ID),
            r.get(INVOICE.VERSION),
            r.get(INVOICE.NUMBER),
            r.get(INVOICE.DATE_TIME),
            r.get(client),
            r.get(items)));
  }

  public Page<Invoice> findAll(int offset, int limit, InvoiceFilter filter, InvoiceSort sort) {
    var where = repository.buildWhere(filter, getFiltersMap());
    var orderBy = repository.buildSort(sort, getSortsMap());

    var client = field(row(
        INVOICE.client().ID,
        INVOICE.client().VERSION,
        INVOICE.client().NAME)
            .mapping(Client::new)
            .as("client"));

    var invoices = db.select(
        INVOICE.ID,
        INVOICE.VERSION,
        INVOICE.NUMBER,
        INVOICE.DATE_TIME,
        client)
        .from(INVOICE)
        .where(where)
        .orderBy(orderBy)
        .offset(offset)
        .limit(limit)
        .fetch(r -> new Invoice(
            r.get(INVOICE.ID),
            r.get(INVOICE.VERSION),
            r.get(INVOICE.NUMBER),
            r.get(INVOICE.DATE_TIME),
            r.get(client)));

    var total = db.selectCount()
        .from(INVOICE)
        .where(where)
        .fetchOneInto(Integer.class);

    return new Page<>(invoices, total);
  }

  private Map<String, Field<?>> getFiltersMap() {
    return Map.of(
        "id", INVOICE.ID,
        "number", INVOICE.NUMBER,
        "dateTime", INVOICE.DATE_TIME,
        "clientId", INVOICE.client().ID);
  }

  private Map<String, Field<?>> getSortsMap() {
    return Map.of(
        "id", INVOICE.ID,
        "number", INVOICE.NUMBER,
        "dateTime", INVOICE.DATE_TIME,
        "clientId", INVOICE.client().ID);
  }
}
