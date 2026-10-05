package org.example.application.mapper;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.example.application.controller.InvoiceController.CreateInvoiceDto;
import org.example.application.controller.InvoiceController.CreateInvoiceItemDto;
import org.example.application.controller.InvoiceController.FindAllInvoiceDto;
import org.example.application.controller.InvoiceController.FindOneInvoiceDto;
import org.example.application.controller.InvoiceController.InvoiceItemDto;
import org.example.application.controller.InvoiceController.PatchInvoiceDto;
import org.example.application.controller.InvoiceController.PatchInvoiceItemDto;
import org.example.application.controller.InvoiceController.PatchItemBodyDto;
import org.example.application.controller.InvoiceController.UpdateItemBodyDto;
import org.example.application.controller.InvoiceController.UpdateInvoiceDto;
import org.example.application.controller.InvoiceController.UpdateInvoiceItemDto;
import org.example.domain.entity.Invoice;
import org.example.domain.entity.InvoiceItem;
import org.example.domain.EntityNotFoundException;
import org.example.exceptionmapper.BusinessException;
import org.example.infrastructure.entity.InvoiceEntity;
import org.example.infrastructure.entity.InvoiceItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import jakarta.inject.Inject;

@Mapper(uses = { ClientMapper.class, ProductMapper.class })
public abstract class InvoiceMapper {

  @Inject
  private ClientMapper clientMapper;

  public abstract Invoice toInvoice(CreateInvoiceDto dto);

  public abstract Invoice toInvoice(InvoiceEntity entity);

  public InvoiceEntity toInvoiceEntity(Invoice invoice) {
    InvoiceEntity entity = new InvoiceEntity();

    entity.setId(invoice.getId());
    entity.setVersion(invoice.getVersion());
    entity.setNumber(invoice.getNumber());
    entity.setDateTime(invoice.getDateTime());
    entity.setClient(clientMapper.toClientEntity(invoice.getClient()));

    if (invoice.getItems() != null) {
      entity.setItems(invoice.getItems()
          .stream()
          .map(i -> {
            var item = toInvoiceItemEntity(i);
            item.setInvoice(entity);
            return item;
          })
          .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    return entity;
  }

  public Invoice updateInvoice(UpdateInvoiceDto dto, Invoice invoice) {
    invoice.setNumber(dto.number());
    invoice.setDateTime(dto.dateTime());
    invoice.setClient(clientMapper.fromId(dto.client()));

    if (dto.items() != null) {
      applyItemChanges(invoice, dto.items().update(), UpdateInvoiceItemDto::id, dto.items().remove(),
          dto.items().create(), this::updateInvoiceItem);
    }

    return invoice;
  }


  private <T> void applyItemChanges(Invoice invoice, List<T> updates, Function<T, Integer> idOf,
      Set<Integer> removes, List<CreateInvoiceItemDto> creates, BiConsumer<T, InvoiceItem> updater) {
    if (updates != null) {
      var seenIds = new java.util.HashSet<Integer>();

      for (var item : updates) {
        if (item != null && !seenIds.add(idOf.apply(item))) {
          throw new BusinessException("O item com id " + idOf.apply(item) + " aparece mais de uma vez em update.");
        }
      }

      if (removes != null) {
        for (var id : removes) {
          if (id != null && seenIds.contains(id)) {
            throw new BusinessException("O item com id " + id + " não pode estar em update e remove ao mesmo tempo.");
          }
        }
      }

      for (var item : updates) {
        if (item == null) {
          continue;
        }
        updater.accept(item, findItem(invoice, idOf.apply(item)));
      }
    }

    if (removes != null) {
      for (var id : removes) {
        if (id == null) {
          continue;
        }
        invoice.getItems().remove(findItem(invoice, id));
      }
    }

    if (creates != null) {
      for (var item : creates) {
        if (item != null) {
          invoice.getItems().add(toInvoiceItem(item));
        }
      }
    }
  }

  private InvoiceItem findItem(Invoice invoice, Integer id) {
    return invoice.getItems()
        .stream()
        .filter(i -> i.getId() != null && i.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Item com id " + id + " não encontrado"));
  }

  public abstract Invoice updateInvoice(Invoice invoice, @MappingTarget Invoice invoiceToUpdate);

  public Invoice patchInvoice(PatchInvoiceDto dto, Invoice invoice) {
    if (dto.number() != null) {
      invoice.setNumber(dto.number());
    }

    if (dto.dateTime() != null) {
      invoice.setDateTime(dto.dateTime());
    }

    if (dto.client() != null) {
      invoice.setClient(clientMapper.fromId(dto.client()));
    }

    if (dto.items() != null) {
      applyItemChanges(invoice, dto.items().update(), PatchInvoiceItemDto::id, dto.items().remove(),
          dto.items().create(), this::patchInvoiceItem);
    }

    return invoice;
  }

  public abstract FindOneInvoiceDto toFindOneInvoiceDto(Invoice invoice);

  public abstract FindAllInvoiceDto toFindAllInvoiceDto(Invoice invoice);

  public abstract InvoiceItem toInvoiceItem(CreateInvoiceItemDto dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  public abstract InvoiceItem updateInvoiceItem(UpdateInvoiceItemDto dto, @MappingTarget InvoiceItem item);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  public abstract InvoiceItem updateInvoiceItem(UpdateItemBodyDto dto, @MappingTarget InvoiceItem item);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "quantity", nullValuePropertyMappingStrategy = IGNORE)
  @Mapping(target = "unitValue", nullValuePropertyMappingStrategy = IGNORE)
  @Mapping(target = "product", nullValuePropertyMappingStrategy = IGNORE)
  public abstract InvoiceItem patchInvoiceItem(PatchInvoiceItemDto dto, @MappingTarget InvoiceItem item);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "quantity", nullValuePropertyMappingStrategy = IGNORE)
  @Mapping(target = "unitValue", nullValuePropertyMappingStrategy = IGNORE)
  @Mapping(target = "product", nullValuePropertyMappingStrategy = IGNORE)
  public abstract InvoiceItem patchInvoiceItem(PatchItemBodyDto dto, @MappingTarget InvoiceItem item);

  public abstract InvoiceItemDto toInvoiceItemDto(InvoiceItem item);

  public abstract InvoiceItemEntity toInvoiceItemEntity(InvoiceItem item);
}
