package org.example.application.mapper;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

import org.example.application.controller.ClientController.ClientDto;
import org.example.application.controller.ClientController.CreateClientDto;
import org.example.application.controller.ClientController.PatchClientDto;
import org.example.domain.entity.Client;
import org.example.infrastructure.entity.ClientEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ClientMapper {

  Client toClient(CreateClientDto dto);

  Client toClient(ClientEntity entity);

  ClientDto toDto(Client client);

  Client updateClient(CreateClientDto dto, @MappingTarget Client client);

  Client updateClient(Client client, @MappingTarget Client clientToUpdate);

  @Mapping(target = "name", nullValuePropertyMappingStrategy = IGNORE)
  Client patchClient(PatchClientDto dto, @MappingTarget Client client);

  Client fromId(Integer id);

  ClientEntity toClientEntity(Client client);
}
