package org.example.application.controller;

import java.util.HashMap;

import org.example.application.mapper.ProductMapper;
import org.example.domain.useCase.product.FindAllProductUseCase;
import org.example.domain.useCase.product.FindByIdProductUseCase;
import org.example.domain.useCase.product.ProductFilter;
import org.example.domain.useCase.product.ProductSort;
import org.example.domain.useCase.product.RemoveProductUseCase;
import org.example.domain.useCase.product.SaveProductUseCase;
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

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductController {

  @Inject
  private SaveProductUseCase saveProductUseCase;
  @Inject
  private RemoveProductUseCase removeProductUseCase;
  @Inject
  private FindByIdProductUseCase findByIdProductUseCase;
  @Inject
  private FindAllProductUseCase findAllProductUseCase;
  @Inject
  private ProductMapper mapper;
  @Context
  private UriInfo uriInfo;

  public record CreateProductDto(@NotBlank @Size(max = 255) String name) {
  }

  public record PatchProductDto(@Size(min = 1, max = 255) @Pattern(regexp = ".*\\S.*") String name) {
  }

  public record ProductDto(Integer id, String name) {
  }

  @POST
  public Response create(@NotNull @Valid CreateProductDto dto) {
    var product = mapper.toProduct(dto);
    var savedProduct = saveProductUseCase.execute(product);
    var productDto = mapper.toDto(savedProduct);
    var uri = uriInfo.getAbsolutePathBuilder().path(savedProduct.getId().toString()).build();

    return Response.created(uri).entity(productDto).tag(ETags.of(savedProduct.getVersion())).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@PathParam("id") Integer id, @HeaderParam("If-Match") String ifMatch,
      @NotNull @Valid CreateProductDto dto) {
    var product = findByIdProductUseCase.execute(id);
    ETags.check(ifMatch, product.getVersion());
    var updatedProduct = mapper.updateProduct(dto, product);

    var savedProduct = saveProductUseCase.execute(updatedProduct);
    var productDto = mapper.toDto(savedProduct);

    return Response.ok(productDto).tag(ETags.of(savedProduct.getVersion())).build();
  }

  @PATCH
  @Path("/{id}")
  public Response patch(@PathParam("id") Integer id, @HeaderParam("If-Match") String ifMatch,
      @NotNull @Valid PatchProductDto dto) {
    var product = findByIdProductUseCase.execute(id);
    ETags.check(ifMatch, product.getVersion());
    var updatedProduct = mapper.patchProduct(dto, product);

    var savedProduct = saveProductUseCase.execute(updatedProduct);
    var productDto = mapper.toDto(savedProduct);

    return Response.ok(productDto).tag(ETags.of(savedProduct.getVersion())).build();
  }

  @DELETE
  @Path("/{id}")
  public Response remove(@PathParam("id") Integer id, @HeaderParam("If-Match") String ifMatch) {
    var product = findByIdProductUseCase.execute(id);
    ETags.check(ifMatch, product.getVersion());

    removeProductUseCase.execute(product);

    return Response.noContent().build();
  }

  @GET
  @Path("/{id}")
  public Response findById(@PathParam("id") Integer id) {
    var product = findByIdProductUseCase.execute(id);
    var productDto = mapper.toDto(product);

    return Response.ok(productDto).tag(ETags.of(product.getVersion())).build();
  }

  @GET
  public Response findAll(
      @QueryParam("offset") @DefaultValue("0") @Min(0) int offset,
      @QueryParam("limit") @DefaultValue("20") @Min(0) @Max(100) int limit,
      @QueryParam("name") String name,
      @QueryParam("sort") @DefaultValue("name|ASC") String sort) {
    var filterMap = new HashMap<String, Object>();
    filterMap.put("name", name);

    var productFilter = ResourceUtil.buildFilter(ProductFilter.class, filterMap);

    var productSort = ResourceUtil.buildSort(ProductSort.class, sort);

    var products = findAllProductUseCase.execute(
        offset,
        limit,
        productFilter,
        productSort);

    var page = new Page<ProductDto>(products.result()
        .stream()
        .map(mapper::toDto)
        .toList(),
        products.total());

    return Response.ok(page).build();
  }
}
