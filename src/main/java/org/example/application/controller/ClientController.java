package org.example.application.controller;

import java.util.HashMap;

import org.example.application.mapper.ClientMapper;
import org.example.domain.useCase.client.ClientFilter;
import org.example.domain.useCase.client.ClientSort;
import org.example.domain.useCase.client.FindAllClientUseCase;
import org.example.domain.useCase.client.FindByIdClientUseCase;
import org.example.domain.useCase.client.RemoveClientUseCase;
import org.example.domain.useCase.client.SaveClientUseCase;
import org.example.util.ETags;
import org.example.util.Page;
import org.example.util.ResourceUtil;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientController {

  @Inject
  private SaveClientUseCase saveClientUseCase;
  @Inject
  private RemoveClientUseCase removeClientUseCase;
  @Inject
  private FindByIdClientUseCase findByIdClientUseCase;
  @Inject
  private FindAllClientUseCase findAllClientUseCase;
  @Inject
  private ClientMapper mapper;
  @Context
  private UriInfo uriInfo;

  public record CreateClientDto(@NotBlank @Size(max = 255) String name) {
  }

  public record PatchClientDto(@Size(min = 1, max = 255) @Pattern(regexp = ".*\\S.*") String name) {
  }

  public record ClientDto(Integer id, String name) {
  }

  @POST
  public Response create(@NotNull @Valid CreateClientDto dto) {
    var client = mapper.toClient(dto);
    var savedClient = saveClientUseCase.execute(client);
    var clientDto = mapper.toDto(savedClient);
    var uri = uriInfo.getAbsolutePathBuilder().path(savedClient.getId().toString()).build();

    return Response.created(uri).entity(clientDto).tag(ETags.of(savedClient.getVersion())).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@PathParam("id") Integer id, @HeaderParam("If-Match") String ifMatch,
      @NotNull @Valid CreateClientDto dto) {
    var client = findByIdClientUseCase.execute(id);
    ETags.check(ifMatch, client.getVersion());
    var updatedClient = mapper.updateClient(dto, client);

    var savedClient = saveClientUseCase.execute(updatedClient);
    var clientDto = mapper.toDto(savedClient);

    return Response.ok(clientDto).tag(ETags.of(savedClient.getVersion())).build();
  }

  @PATCH
  @Path("/{id}")
  public Response patch(@PathParam("id") Integer id, @HeaderParam("If-Match") String ifMatch,
      @NotNull @Valid PatchClientDto dto) {
    var client = findByIdClientUseCase.execute(id);
    ETags.check(ifMatch, client.getVersion());
    var updatedClient = mapper.patchClient(dto, client);

    var savedClient = saveClientUseCase.execute(updatedClient);
    var clientDto = mapper.toDto(savedClient);

    return Response.ok(clientDto).tag(ETags.of(savedClient.getVersion())).build();
  }

  @DELETE
  @Path("/{id}")
  public Response remove(@PathParam("id") Integer id, @HeaderParam("If-Match") String ifMatch) {
    var client = findByIdClientUseCase.execute(id);
    ETags.check(ifMatch, client.getVersion());

    removeClientUseCase.execute(client);

    return Response.noContent().build();
  }

  @GET
  @Path("/{id}")
  public Response findById(@PathParam("id") Integer id) {
    var client = findByIdClientUseCase.execute(id);
    var clientDto = mapper.toDto(client);

    return Response.ok(clientDto).tag(ETags.of(client.getVersion())).build();
  }

  @GET
  public Response findAll(
      @QueryParam("offset") @DefaultValue("0") @Min(0) int offset,
      @QueryParam("limit") @DefaultValue("20") @Min(0) @Max(100) int limit,
      @QueryParam("name") String name,
      @QueryParam("sort") @DefaultValue("name|ASC") String sort) {
    var filterMap = new HashMap<String, Object>();
    filterMap.put("name", name);

    var clientFilter = ResourceUtil.buildFilter(ClientFilter.class, filterMap);

    var clientSort = ResourceUtil.buildSort(ClientSort.class, sort);

    var clients = findAllClientUseCase.execute(
        offset,
        limit,
        clientFilter,
        clientSort);

    var page = new Page<ClientDto>(clients.result()
        .stream()
        .map(mapper::toDto)
        .toList(),
        clients.total());

    return Response.ok(page).build();
  }
}
