package org.example.application.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.example.application.controller.ClientController.ClientDto;
import org.example.application.controller.ProductController.ProductDto;
import org.example.application.mapper.InvoiceMapper;
import org.example.constraint.Unique;
import org.example.domain.EntityNotFoundException;
import org.example.domain.useCase.invoice.FindAllInvoiceUseCase;
import org.example.domain.useCase.invoice.FindByIdInvoiceUseCase;
import org.example.domain.useCase.invoice.InvoiceFilter;
import org.example.domain.useCase.invoice.InvoiceSort;
import org.example.domain.useCase.invoice.RemoveInvoiceUseCase;
import org.example.domain.useCase.invoice.SaveInvoiceUseCase;
import org.example.util.ETags;
import org.example.util.Page;
import org.example.util.ResourceUtil;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

@Path("/invoices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InvoiceController {

  @Inject
  private SaveInvoiceUseCase saveInvoiceUseCase;
  @Inject
  private RemoveInvoiceUseCase removeInvoiceUseCase;
  @Inject
  private FindByIdInvoiceUseCase findByIdInvoiceUseCase;
  @Inject
  private FindAllInvoiceUseCase findAllInvoiceUseCase;
  @Inject
  private InvoiceMapper mapper;
  @Context
  private UriInfo uriInfo;

  public record CreateInvoiceDto(
      @NotBlank @Size(max = 255) String number,
      @NotNull LocalDateTime dateTime,
      @NotNull Integer client,
      @NotEmpty @Unique( {
          "product" }) List<@NotNull @Valid CreateInvoiceItemDto> items){
  }

  public record CreateInvoiceItemDto(
      @NotNull @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal quantity,
      @NotNull @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal unitValue,
      @NotNull Integer product) {
  }

  public record UpdateInvoiceDto(
      @NotBlank @Size(max = 255) String number,
      @NotNull LocalDateTime dateTime,
      @NotNull Integer client,
      @Valid ModifyInvoiceItemDto items) {
  }

  public record ModifyInvoiceItemDto(
      @Unique( {
          "product" }) List<@NotNull @Valid CreateInvoiceItemDto> create,
      @Unique({
          "id", "product" }) List<@NotNull @Valid UpdateInvoiceItemDto> update,
      Set<Integer> remove){
  }

  public record UpdateInvoiceItemDto(
      @NotNull Integer id,
      @NotNull @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal quantity,
      @NotNull @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal unitValue,
      @NotNull Integer product) {
  }

  public record UpdateItemBodyDto(
      @NotNull @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal quantity,
      @NotNull @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal unitValue,
      @NotNull Integer product) {
  }

  public record PatchItemBodyDto(
      @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal quantity,
      @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal unitValue,
      Integer product) {
  }

  public record PatchInvoiceDto(
      @Size(min = 1, max = 255) @Pattern(regexp = ".*\\S.*") String number,
      LocalDateTime dateTime,
      Integer client,
      @Valid ModifyPatchInvoiceItemDto items) {
  }

  public record ModifyPatchInvoiceItemDto(
      @Unique( {
          "product" }) List<@NotNull @Valid CreateInvoiceItemDto> create,
      @Unique({
          "id", "product" }) List<@NotNull @Valid PatchInvoiceItemDto> update,
      Set<Integer> remove){
  }

  public record PatchInvoiceItemDto(
      @NotNull Integer id,
      @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal quantity,
      @Min(0) @Digits(integer = 15, fraction = 4) BigDecimal unitValue,
      Integer product) {
  }

  public record FindOneInvoiceDto(Integer id, String number, LocalDateTime dateTime, ClientDto client,
      List<InvoiceItemDto> items) {
  }

  public record FindAllInvoiceDto(Integer id, String number, LocalDateTime dateTime, ClientDto client) {
  }

  public record InvoiceItemDto(Integer id, BigDecimal quantity, BigDecimal unitValue, ProductDto product) {
  }

  @POST
  public Response create(@NotNull @Valid CreateInvoiceDto dto) {
    var invoice = mapper.toInvoice(dto);
    var savedInvoice = findByIdInvoiceUseCase.execute(saveInvoiceUseCase.execute(invoice).getId());
    var invoiceDto = mapper.toFindOneInvoiceDto(savedInvoice);
    var uri = uriInfo.getAbsolutePathBuilder().path(savedInvoice.getId().toString()).build();

    return Response.created(uri).entity(invoiceDto).tag(ETags.of(savedInvoice.getVersion())).build();
  }

  @POST
  @Path("/{id}/items")
  public Response createItem(@PathParam("id") Integer id, @NotNull @Valid CreateInvoiceItemDto dto) {
    var invoice = findByIdInvoiceUseCase.execute(id);

    var invoiceItem = mapper.toInvoiceItem(dto);
    invoice.getItems().add(invoiceItem);

    var savedInvoice = findByIdInvoiceUseCase.execute(saveInvoiceUseCase.execute(invoice).getId());
    var savedInvoiceItem = savedInvoice.getItems()
        .stream()
        .filter(i -> i.getProduct().getId().equals(invoiceItem.getProduct().getId()))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Item recém-criado não encontrado"));

    var invoiceItemDto = mapper.toInvoiceItemDto(savedInvoiceItem);
    var uri = uriInfo.getAbsolutePathBuilder().path(savedInvoiceItem.getId().toString()).build();

    return Response.created(uri).entity(invoiceItemDto).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@PathParam("id") Integer id, @HeaderParam("If-Match") String ifMatch,
      @NotNull @Valid UpdateInvoiceDto dto) {
    var invoice = findByIdInvoiceUseCase.execute(id);
    ETags.check(ifMatch, invoice.getVersion());
    var updatedInvoice = mapper.updateInvoice(dto, invoice);

    var savedInvoice = findByIdInvoiceUseCase.execute(saveInvoiceUseCase.execute(updatedInvoice).getId());
    var invoiceDto = mapper.toFindOneInvoiceDto(savedInvoice);

    return Response.ok(invoiceDto).tag(ETags.of(savedInvoice.getVersion())).build();
  }

  @PUT
  @Path("/{id}/items/{itemId}")
  public Response updateItem(
      @PathParam("id") Integer id,
      @PathParam("itemId") Integer itemId,
      @NotNull @Valid UpdateItemBodyDto dto) {
    var invoice = findByIdInvoiceUseCase.execute(id);
    var invoiceItem = invoice.getItems()
        .stream()
        .filter(i -> i.getId().equals(itemId))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Item com id " + itemId + " não encontrado"));

    mapper.updateInvoiceItem(dto, invoiceItem);

    var savedInvoice = findByIdInvoiceUseCase.execute(saveInvoiceUseCase.execute(invoice).getId());
    var savedInvoiceItem = savedInvoice.getItems()
        .stream()
        .filter(i -> i.getId().equals(invoiceItem.getId()))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Item com id " + itemId + " não encontrado"));

    var invoiceItemDto = mapper.toInvoiceItemDto(savedInvoiceItem);

    return Response.ok(invoiceItemDto).build();
  }

  @PATCH
  @Path("/{id}")
  public Response patch(@PathParam("id") Integer id, @HeaderParam("If-Match") String ifMatch,
      @NotNull @Valid PatchInvoiceDto dto) {
    var invoice = findByIdInvoiceUseCase.execute(id);
    ETags.check(ifMatch, invoice.getVersion());
    var updatedInvoice = mapper.patchInvoice(dto, invoice);

    var savedInvoice = findByIdInvoiceUseCase.execute(saveInvoiceUseCase.execute(updatedInvoice).getId());
    var invoiceDto = mapper.toFindOneInvoiceDto(savedInvoice);

    return Response.ok(invoiceDto).tag(ETags.of(savedInvoice.getVersion())).build();
  }

  @PATCH
  @Path("/{id}/items/{itemId}")
  public Response patchItem(
      @PathParam("id") Integer id,
      @PathParam("itemId") Integer itemId,
      @NotNull @Valid PatchItemBodyDto dto) {
    var invoice = findByIdInvoiceUseCase.execute(id);
    var invoiceItem = invoice.getItems()
        .stream()
        .filter(i -> i.getId().equals(itemId))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Item com id " + itemId + " não encontrado"));

    mapper.patchInvoiceItem(dto, invoiceItem);

    var savedInvoice = findByIdInvoiceUseCase.execute(saveInvoiceUseCase.execute(invoice).getId());
    var savedInvoiceItem = savedInvoice.getItems()
        .stream()
        .filter(i -> i.getId().equals(invoiceItem.getId()))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Item com id " + itemId + " não encontrado"));

    var invoiceItemDto = mapper.toInvoiceItemDto(savedInvoiceItem);

    return Response.ok(invoiceItemDto).build();
  }

  @DELETE
  @Path("/{id}")
  public Response remove(@PathParam("id") Integer id, @HeaderParam("If-Match") String ifMatch) {
    var invoice = findByIdInvoiceUseCase.execute(id);
    ETags.check(ifMatch, invoice.getVersion());

    removeInvoiceUseCase.execute(invoice);

    return Response.noContent().build();
  }

  @DELETE
  @Path("/{id}/items/{itemId}")
  public Response deleteItem(
      @PathParam("id") Integer id,
      @PathParam("itemId") Integer itemId) {
    var invoice = findByIdInvoiceUseCase.execute(id);
    var invoiceItem = invoice.getItems()
        .stream()
        .filter(i -> i.getId().equals(itemId))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Item com id " + itemId + " não encontrado"));

    invoice.getItems().remove(invoiceItem);

    saveInvoiceUseCase.execute(invoice);

    return Response.noContent().build();
  }

  @GET
  @Path("/{id}")
  public Response findById(@PathParam("id") Integer id) {
    var invoice = findByIdInvoiceUseCase.execute(id);
    var invoiceDto = mapper.toFindOneInvoiceDto(invoice);

    return Response.ok(invoiceDto).tag(ETags.of(invoice.getVersion())).build();
  }

  @GET
  @Path("/{id}/items/{itemId}")
  public Response getItem(
      @PathParam("id") Integer id,
      @PathParam("itemId") Integer itemId) {
    var invoice = findByIdInvoiceUseCase.execute(id);
    var invoiceItem = invoice.getItems()
        .stream()
        .filter(i -> i.getId().equals(itemId))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Item com id " + itemId + " não encontrado"));

    var invoiceItemDto = mapper.toInvoiceItemDto(invoiceItem);

    return Response.ok(invoiceItemDto).build();
  }

  @GET
  public Response findAll(
      @QueryParam("offset") @DefaultValue("0") @Min(0) int offset,
      @QueryParam("limit") @DefaultValue("20") @Min(0) @Max(100) int limit,
      @QueryParam("dateTime") String dateTime,
      @QueryParam("clientId") String clientId,
      @QueryParam("sort") @DefaultValue("dateTime|DESC") String sort) {
    var filterMap = new HashMap<String, Object>();
    filterMap.put("dateTime", dateTime);
    filterMap.put("clientId", clientId);

    var invoiceFilter = ResourceUtil.buildFilter(InvoiceFilter.class, filterMap);

    var invoiceSort = ResourceUtil.buildSort(InvoiceSort.class, sort);

    var invoices = findAllInvoiceUseCase.execute(
        offset,
        limit,
        invoiceFilter,
        invoiceSort);

    var page = new Page<FindAllInvoiceDto>(invoices.result()
        .stream()
        .map(mapper::toFindAllInvoiceDto)
        .toList(),
        invoices.total());

    return Response.ok(page).build();
  }

  @GET
  @Path("/{id}/items")
  public Response getItems(@PathParam("id") Integer id) {
    var invoice = findByIdInvoiceUseCase.execute(id);

    var dtos = invoice.getItems()
        .stream()
        .map(mapper::toInvoiceItemDto)
        .toList();

    return Response.ok(dtos).build();
  }
}
